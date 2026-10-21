package eu.europa.ec.itb.shacl.config;

import com.sun.net.httpserver.HttpServer;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceBlockingExecutorTest {

    private HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/sparql", exchange -> {
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        ServiceBlockingExecutor.install();
    }

    @AfterEach
    void tearDown() {
        ServiceBlockingExecutor.unblock();
        server.stop(0);
    }

    private void runServiceQuery() {
        var query = "SELECT * WHERE { SERVICE <http://127.0.0.1:%d/sparql> { ?s ?p ?o } }".formatted(server.getAddress().getPort());
        try (var execution = QueryExecutionFactory.create(query, ModelFactory.createDefaultModel())) {
            execution.execSelect().forEachRemaining(solution -> {});
        }
    }

    private static boolean causedByValidatorException(Throwable t) {
        while (t != null) {
            if (t instanceof ValidatorException) return true;
            t = t.getCause();
        }
        return false;
    }

    @Test
    void testServiceCallIsBlocked() {
        ServiceBlockingExecutor.block();
        var error = assertThrows(RuntimeException.class, this::runServiceQuery);
        assertTrue(causedByValidatorException(error), "Expected the failure to be due to the blocked SERVICE call but was: " + error);
        assertEquals(0, requestCount.get());
    }

    @Test
    void testServiceCallIsNotBlockedWhenNotRequested() {
        // The call fails because of the stub's response, but this must be after it has reached the endpoint.
        assertThrows(RuntimeException.class, this::runServiceQuery);
        assertEquals(1, requestCount.get());
    }

    private void runShaclValidationWithServiceCall() {
        var shapes = ModelFactory.createDefaultModel();
        var shapesTtl = """
                @prefix sh: <http://www.w3.org/ns/shacl#> .
                @prefix ex: <http://example.org/> .
                ex:Shape a sh:NodeShape ;
                  sh:targetNode ex:a ;
                  sh:sparql [
                    a sh:SPARQLConstraint ;
                    sh:message "Test" ;
                    sh:select \"\"\"SELECT $this WHERE { SERVICE <http://127.0.0.1:%d/sparql> { ?s ?p ?o } }\"\"\"
                  ] .
                """.formatted(server.getAddress().getPort());
        shapes.read(new java.io.StringReader(shapesTtl), null, "TTL");
        var data = ModelFactory.createDefaultModel();
        data.createResource("http://example.org/a").addProperty(org.apache.jena.vocabulary.RDF.type, data.createResource("http://example.org/T"));
        org.topbraid.shacl.validation.ValidationUtil.validateModel(data, shapes, false);
    }

    @Test
    void testServiceCallInShaclConstraintIsBlocked() {
        ServiceBlockingExecutor.block();
        try {
            runShaclValidationWithServiceCall();
        } catch (RuntimeException e) {
            // Either the failure is propagated or - if swallowed by the SHACL engine - at least no call must be made.
        }
        assertEquals(0, requestCount.get());
    }

    @Test
    void testServiceCallInShaclConstraintReachesEndpointWhenNotBlocked() {
        try {
            runShaclValidationWithServiceCall();
        } catch (RuntimeException e) {
            // Ignore the failure caused by the stub's response.
        }
        assertEquals(1, requestCount.get(), "Control: without blocking the SHACL engine does make the SERVICE call");
    }

    @Test
    void testInstallingTwiceRegistersOnce() {
        ServiceBlockingExecutor.install();
        ServiceBlockingExecutor.install();
        var registry = org.apache.jena.sparql.service.ServiceExecutorRegistry.get();
        assertEquals(1, registry.getSingleChain().stream().filter(ServiceBlockingExecutor.class::isInstance).count());
    }

}
