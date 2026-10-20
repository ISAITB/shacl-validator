package eu.europa.ec.itb.shacl.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that credentials configured for shapes are only sent to the host the shapes were retrieved from, and not to
 * hosts that the shapes import from.
 */
class CustomLocatorHTTPTest {

    private HttpServer server;
    private final List<String> authorizationHeaders = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/import.ttl", exchange -> {
            authorizationHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            byte[] bytes = "<urn:a> <urn:b> <urn:c> .".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/turtle");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        LocatorParams.PARAMS.remove();
        server.stop(0);
    }

    private void open(URI decoratorOrigin) {
        Consumer<HttpRequest.Builder> decorator = builder -> builder.header("Authorization", "Bearer secret");
        LocatorParams.PARAMS.set(new LocatorParams(Set.of(), HttpClient.Version.HTTP_1_1, Map.of(), decorator, decoratorOrigin, null, null));
        var url = "http://127.0.0.1:" + server.getAddress().getPort() + "/import.ttl";
        try (var in = new CustomLocatorHTTP().performOpen(url)) {
            in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void testCredentialsOnlySentToTheHostOfTheShapes() {
        // Shapes retrieved from the same host as the import.
        open(URI.create("http://127.0.0.1:1/shapes.ttl"));
        // Shapes not retrieved from a URI (e.g. uploaded).
        open(null);
        // Shapes retrieved from another host.
        open(URI.create("https://93.184.216.34/shapes.ttl"));
        assertEquals(List.of("Bearer secret", "null", "null"), authorizationHeaders);
    }

}
