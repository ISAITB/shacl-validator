package eu.europa.ec.itb.shacl.config;

import com.sun.net.httpserver.HttpServer;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.RemoteResourceBudget;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.NormalizedURI;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuardedJsonLdDocumentLoaderTest {

    private static final String CONTEXT = "{\"@context\":{\"name\":\"http://example.org/name\"}}";

    private HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();
    private final FileManager fileManager = new FileManager();

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/allowed/ctx.jsonld", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 200, CONTEXT);
        });
        server.createContext("/allowed/redirect", exchange -> {
            requestCount.incrementAndGet();
            exchange.getResponseHeaders().add("Location", "/secret/ctx.jsonld");
            respond(exchange, 302, "");
        });
        server.createContext("/allowed/ctx2.jsonld", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 200, CONTEXT);
        });
        server.createContext("/allowed/ctx3.jsonld", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 200, CONTEXT);
        });
        server.createContext("/allowed/large.jsonld", exchange -> {
            requestCount.incrementAndGet();
            // A (valid) context with many terms.
            var large = new StringBuilder("{\"@context\":{\"name\":\"http://example.org/name\"");
            for (int i = 0; i < 6000; i++) {
                large.append(",\"term").append(i).append("\":\"http://example.org/term").append(i).append('"');
            }
            respond(exchange, 200, large.append("}}").toString());
        });
        server.createContext("/secret/ctx.jsonld", exchange -> {
            requestCount.incrementAndGet();
            respond(exchange, 200, CONTEXT);
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/ld+json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private ResourceAccessPolicy policy(List<String> whitelistedUris) {
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.getNormalizedAllowedUriImports()).thenReturn(whitelistedUris.stream().map(uri -> NormalizedURI.of(URI.create(uri))).toList());
        when(appConfig.getTmpFolder()).thenReturn(tempDir.toString());
        when(appConfig.getResourceRoot()).thenReturn(tempDir.resolve("resources").toString());
        when(appConfig.isRestrictResourcesToDomain()).thenReturn(false);
        var domainConfig = mock(DomainConfig.class);
        return new ResourceAccessPolicy(ImportedUriAuthorizer.forUserContent(appConfig), ImportedFileAuthorizer.from(appConfig, domainConfig));
    }

    private int parse(String contextReference, ResourceAccessPolicy policy) {
        var document = "{\"@context\":\"%s\",\"@id\":\"http://example.org/a\",\"name\":\"value\"}".formatted(contextReference);
        var model = fileManager.readModel(new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8)), Lang.JSONLD11, null, policy);
        return (int) model.size();
    }

    @Test
    void testRemoteContextOnInternalAddressIsBlocked() {
        assertThrows(ValidatorException.class, () -> parse(baseUrl() + "/allowed/ctx.jsonld", policy(List.of())));
        assertEquals(0, requestCount.get(), "No request should have been made to the blocked address");
    }

    @Test
    void testCloudMetadataAddressIsBlocked() {
        assertThrows(ValidatorException.class, () -> parse("http://169.254.169.254/latest/meta-data/", policy(List.of())));
    }

    @Test
    void testWhitelistedRemoteContextIsLoaded() {
        assertEquals(1, parse(baseUrl() + "/allowed/ctx.jsonld", policy(List.of(baseUrl() + "/allowed"))));
        assertEquals(1, requestCount.get());
    }

    @Test
    void testRedirectToNonWhitelistedAddressIsBlocked() {
        assertThrows(ValidatorException.class, () -> parse(baseUrl() + "/allowed/redirect", policy(List.of(baseUrl() + "/allowed"))));
        assertEquals(1, requestCount.get(), "Only the whitelisted request should have been made, not the redirect target");
    }

    @Test
    void testFileOutsideAllowedRootsIsBlocked() throws IOException {
        Path outside = Files.createTempFile("outside-context", ".jsonld");
        try {
            Files.writeString(outside, CONTEXT);
            var allowedRoot = Files.createDirectories(tempDir.resolve("resources"));
            assertNotNull(allowedRoot);
            // The temp dir is an allowed root, so make sure the outside file is really outside of it.
            assertFalse(outside.toAbsolutePath().normalize().startsWith(tempDir.toAbsolutePath().normalize()));
            assertThrows(ValidatorException.class, () -> parse(outside.toUri().toString(), policy(List.of())));
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void testFileWithinAllowedRootsIsLoaded() throws IOException {
        Path context = tempDir.resolve("context.jsonld");
        Files.writeString(context, CONTEXT);
        assertEquals(1, parse(context.toUri().toString(), policy(List.of())));
    }

    @Test
    void testFileIsBlockedIfNoFileAuthorizer() throws IOException {
        Path context = tempDir.resolve("context.jsonld");
        Files.writeString(context, CONTEXT);
        var noFilePolicy = new ResourceAccessPolicy(policy(List.of()).uriAuthorizer(), null);
        assertThrows(Exception.class, () -> parse(context.toUri().toString(), noFilePolicy));
    }

    @Test
    void testUnsupportedSchemesAreRejected() {
        var policy = policy(List.of());
        assertThrows(Exception.class, () -> parse("ftp://example.org/context.jsonld", policy));
        assertThrows(Exception.class, () -> parse("jar:file:///tmp/test.jar!/context.jsonld", policy));
    }

    @Test
    void testInlineContextIsUnaffected() {
        var document = "{\"@context\":{\"name\":\"http://example.org/name\"},\"@id\":\"http://example.org/a\",\"name\":\"value\"}";
        var model = fileManager.readModel(new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8)), Lang.JSONLD11, null, policy(List.of()));
        assertEquals(1, model.size());
    }

    private static ApplicationConfig.RemoteLimits limits(int maxResourceKb, int maxRequests) {
        var limits = new ApplicationConfig.RemoteLimits();
        limits.setMaxResourceSize(org.springframework.util.unit.DataSize.ofKilobytes(maxResourceKb));
        limits.setMaxRequests(maxRequests);
        return limits;
    }

    @Test
    void testContextAboveResourceSizeLimitIsRejected() {
        var policy = policy(List.of(baseUrl() + "/allowed"));
        // Without limits the large context is loaded.
        assertEquals(1, parse(baseUrl() + "/allowed/large.jsonld", policy));
        try (var ignored = RemoteResourceBudget.open(limits(50, 100))) {
            var error = assertThrows(ValidatorException.class, () -> parse(baseUrl() + "/allowed/large.jsonld", policy));
            assertTrue(error.getMessage().contains("exceeds the maximum allowed size"), error.getMessage());
        }
    }

    @Test
    void testContextWithinResourceSizeLimitIsLoaded() {
        try (var ignored = RemoteResourceBudget.open(limits(50, 100))) {
            assertEquals(1, parse(baseUrl() + "/allowed/ctx.jsonld", policy(List.of(baseUrl() + "/allowed"))));
        }
    }

    @Test
    void testNumberOfContextsLoadedIsLimited() {
        var policy = policy(List.of(baseUrl() + "/allowed"));
        var document = "{\"@context\":[\"%1$s/allowed/ctx.jsonld\",\"%1$s/allowed/ctx2.jsonld\",\"%1$s/allowed/ctx3.jsonld\"],\"@id\":\"http://example.org/a\",\"name\":\"value\"}".formatted(baseUrl());
        try (var ignored = RemoteResourceBudget.open(limits(1024, 2))) {
            var error = assertThrows(ValidatorException.class, () -> fileManager.readModel(new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8)), Lang.JSONLD11, null, policy));
            assertTrue(error.getMessage().contains("maximum allowed number of remote requests"), error.getMessage());
        }
        assertEquals(2, requestCount.get(), "Requests over the limit must not be made");
    }
}
