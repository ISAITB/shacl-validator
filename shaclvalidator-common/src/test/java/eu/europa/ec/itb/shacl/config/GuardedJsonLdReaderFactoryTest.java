package eu.europa.ec.itb.shacl.config;

import com.sun.net.httpserver.HttpServer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.NormalizedURI;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that JSON-LD parsing that is not explicitly configured (e.g. a plain Model.read call, or the loading of
 * a JSON-LD resource referred to by owl:imports) is also prevented from loading unauthorized remote contexts.
 */
class GuardedJsonLdReaderFactoryTest {

    private static final String CONTEXT = "{\"@context\":{\"name\":\"http://example.org/name\"}}";

    private HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/ctx.jsonld", exchange -> {
            requestCount.incrementAndGet();
            byte[] bytes = CONTEXT.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/ld+json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.getNormalizedAllowedUriImports()).thenReturn(List.of());
        GuardedJsonLdReaderFactory.install(ImportedUriAuthorizer.forUserContent(appConfig));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        LocatorParams.PARAMS.remove();
    }

    private String document() {
        return "{\"@context\":\"http://127.0.0.1:%d/ctx.jsonld\",\"@id\":\"http://example.org/a\",\"name\":\"value\"}".formatted(server.getAddress().getPort());
    }

    private long read(String syntax) {
        var model = ModelFactory.createDefaultModel();
        model.read(new ByteArrayInputStream(document().getBytes(StandardCharsets.UTF_8)), null, syntax);
        return model.size();
    }

    @Test
    void testPlainModelReadDoesNotLoadInternalContext() {
        assertThrows(ValidatorException.class, () -> read("JSON-LD"));
        assertEquals(0, requestCount.get());
    }

    @Test
    void testInstallingTwiceDoesNotWrapTwice() {
        var appConfig = mock(ApplicationConfig.class);
        when(appConfig.getNormalizedAllowedUriImports()).thenReturn(List.of());
        GuardedJsonLdReaderFactory.install(ImportedUriAuthorizer.forUserContent(appConfig));
        assertThrows(ValidatorException.class, () -> read("JSON-LD"));
        assertEquals(0, requestCount.get());
    }

    @Test
    void testRestrictionsOfCurrentImportLoadingAreApplied() {
        var appConfig = mock(ApplicationConfig.class);
        var allowed = "http://127.0.0.1:%d/ctx.jsonld".formatted(server.getAddress().getPort());
        when(appConfig.getNormalizedAllowedUriImports()).thenReturn(List.of(NormalizedURI.of(URI.create(allowed))));
        LocatorParams.PARAMS.set(new LocatorParams(Set.of(), HttpClient.Version.HTTP_1_1, Map.of(), null, ImportedUriAuthorizer.forUserContent(appConfig), null));
        assertEquals(1, read("JSON-LD"));
        assertEquals(1, requestCount.get());
    }

}
