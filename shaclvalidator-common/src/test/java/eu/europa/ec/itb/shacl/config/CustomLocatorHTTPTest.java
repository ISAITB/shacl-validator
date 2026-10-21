package eu.europa.ec.itb.shacl.config;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.RemoteResourceBudget;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.NormalizedURI;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that credentials configured for shapes are only sent to the origin the shapes were retrieved from, and not to
 * hosts that the shapes import from, and that every redirect followed while loading an import is checked.
 */
class CustomLocatorHTTPTest {

    private HttpServer server;
    private HttpServer otherServer;
    private final List<String> authorizationHeaders = Collections.synchronizedList(new ArrayList<>());
    private final List<String> otherAuthorizationHeaders = Collections.synchronizedList(new ArrayList<>());
    private final List<String> otherApiKeyHeaders = Collections.synchronizedList(new ArrayList<>());
    private final List<String> requestedPaths = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        otherServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/import.ttl", exchange -> {
            authorizationHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            respond(exchange, 200, "<urn:a> <urn:b> <urn:c> .", null);
        });
        server.createContext("/allowed/content.ttl", exchange -> {
            requestedPaths.add(exchange.getRequestURI().getPath());
            respond(exchange, 200, "<urn:a> <urn:b> <urn:c> .", null);
        });
        server.createContext("/allowed/redirect", exchange -> {
            requestedPaths.add(exchange.getRequestURI().getPath());
            respond(exchange, 302, "", "/allowed/content.ttl");
        });
        server.createContext("/allowed/to-blocked", exchange -> {
            requestedPaths.add(exchange.getRequestURI().getPath());
            respond(exchange, 302, "", "/blocked/content.ttl");
        });
        server.createContext("/blocked/content.ttl", exchange -> {
            requestedPaths.add(exchange.getRequestURI().getPath());
            respond(exchange, 200, "<urn:a> <urn:b> <urn:c> .", null);
        });
        server.createContext("/large.ttl", exchange -> {
            requestedPaths.add(exchange.getRequestURI().getPath());
            exchange.getResponseHeaders().add("Content-Type", "text/turtle");
            // No declared length, so the size can only be determined while reading.
            exchange.sendResponseHeaders(200, 0);
            try (var out = exchange.getResponseBody()) {
                for (int i = 0; i < 32; i++) {
                    out.write(new byte[64 * 1024]);
                }
            } catch (IOException e) {
                // The client stopped reading, as expected.
            }
        });
        server.createContext("/to-other-origin", exchange -> {
            authorizationHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            respond(exchange, 302, "", "http://127.0.0.1:" + otherServer.getAddress().getPort() + "/import.ttl");
        });
        otherServer.createContext("/import.ttl", exchange -> {
            otherAuthorizationHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            otherApiKeyHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("X-Api-Key")));
            respond(exchange, 200, "<urn:a> <urn:b> <urn:c> .", null);
        });
        server.start();
        otherServer.start();
    }

    @AfterEach
    void tearDown() {
        LocatorParams.PARAMS.remove();
        server.stop(0);
        otherServer.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, String body, String location) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (location != null) {
            exchange.getResponseHeaders().add("Location", location);
        }
        exchange.getResponseHeaders().add("Content-Type", "text/turtle");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private void open(String url, URI decoratorOrigin, ImportedUriAuthorizer uriAuthorizer) {
        // Credentials can be provided by means of any header.
        Consumer<HttpRequest.Builder> decorator = builder -> builder.header("Authorization", "Bearer secret").header("X-Api-Key", "key");
        LocatorParams.PARAMS.set(new LocatorParams(Set.of(), HttpClient.Version.HTTP_1_1, Map.of(), decorator, decoratorOrigin, uriAuthorizer, null));
        try (var in = new CustomLocatorHTTP().performOpen(url)) {
            in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private ImportedUriAuthorizer authorizerAllowing(String prefix) {
        var config = mock(ApplicationConfig.class);
        when(config.getNormalizedAllowedUriImports()).thenReturn(List.of(NormalizedURI.of(URI.create(prefix))));
        return ImportedUriAuthorizer.forUserContent(config);
    }

    @Test
    void testCredentialsOnlySentToTheOriginOfTheShapes() {
        // Shapes retrieved from the same origin as the import.
        open(url("/import.ttl"), URI.create(url("/shapes.ttl")), null);
        // Shapes not retrieved from a URI (e.g. uploaded).
        open(url("/import.ttl"), null, null);
        // Shapes retrieved from another host.
        open(url("/import.ttl"), URI.create("https://93.184.216.34/shapes.ttl"), null);
        // Shapes retrieved from the same host but another port.
        open(url("/import.ttl"), URI.create("http://127.0.0.1:1/shapes.ttl"), null);
        assertEquals(List.of("Bearer secret", "null", "null", "null"), authorizationHeaders);
    }

    @Test
    void testCredentialsAreNotSentToTheTargetOfARedirectToAnotherOrigin() {
        open(url("/to-other-origin"), URI.create(url("/shapes.ttl")), null);
        // The origin of the shapes got the credentials but the redirect target did not.
        assertEquals(List.of("Bearer secret"), authorizationHeaders);
        assertEquals(List.of("null"), otherAuthorizationHeaders);
        assertEquals(List.of("null"), otherApiKeyHeaders);
    }

    @Test
    void testRedirectWithinAllowedLocationsIsFollowed() {
        open(url("/allowed/redirect"), null, authorizerAllowing(url("/allowed/")));
        assertEquals(List.of("/allowed/redirect", "/allowed/content.ttl"), requestedPaths);
    }

    private static ApplicationConfig.RemoteLimits limits(int maxResourceKb, int maxRequests) {
        var limits = new ApplicationConfig.RemoteLimits();
        limits.setMaxResourceSize(DataSize.ofKilobytes(maxResourceKb));
        limits.setMaxRequests(maxRequests);
        return limits;
    }

    @Test
    void testImportAboveResourceSizeLimitIsRejected() {
        try (var ignored = RemoteResourceBudget.open(limits(512, 100))) {
            var error = assertThrows(ValidatorException.class, () -> open(url("/large.ttl"), null, null));
            assertTrue(error.getMessage().contains("exceeds the maximum allowed size"), error.getMessage());
        }
        // The limit is not applied to lookups outside of a validation.
        open(url("/large.ttl"), null, null);
    }

    @Test
    void testRedirectsCountTowardsTheNumberOfRequestsLimit() {
        var authorizer = authorizerAllowing(url("/allowed/"));
        try (var ignored = RemoteResourceBudget.open(limits(512, 1))) {
            var error = assertThrows(ValidatorException.class, () -> open(url("/allowed/redirect"), null, authorizer));
            assertTrue(error.getMessage().contains("maximum allowed number of remote requests"), error.getMessage());
        }
        assertEquals(List.of("/allowed/redirect"), requestedPaths, "Requests over the limit must not be made");
    }

    @Test
    void testRedirectToLocationThatIsNotAllowedIsNotFollowed() {
        var authorizer = authorizerAllowing(url("/allowed/"));
        assertThrows(ValidatorException.class, () -> open(url("/allowed/to-blocked"), null, authorizer));
        assertEquals(List.of("/allowed/to-blocked"), requestedPaths, "The redirect target must not be requested");
    }

}
