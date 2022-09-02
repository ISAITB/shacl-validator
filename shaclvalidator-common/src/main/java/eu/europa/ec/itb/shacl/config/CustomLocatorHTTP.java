package eu.europa.ec.itb.shacl.config;

import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.web.TypedInputStream;
import org.apache.jena.http.HttpLib;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.system.stream.LocatorHTTP;
import org.apache.jena.riot.web.HttpNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;

import static org.apache.jena.http.HttpEnv.httpClientBuilder;

/**
 * Replacement of Jena's built-in {@link LocatorHTTP} which resulted in blocked threads when encountering certain types
 * of failures during the loading of remote resources. The problem was due to the reuse of a static HTTP client that if
 * not closed correctly resulted in blocked threads. This replacement implementation replicates the steps taken but
 * ensures that a fresh HTTP client instance is constructed for each request.
 */
public class CustomLocatorHTTP extends LocatorHTTP {

    private static final Logger LOG = LoggerFactory.getLogger(CustomLocatorHTTP.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public TypedInputStream performOpen(String uri) {
        TypedInputStream result = null;
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            HttpRequest.Builder builder = HttpLib.requestBuilderFor(uri).uri(HttpLib.toRequestURI(uri)).GET();
            builder.header(HttpNames.hAccept, WebContent.defaultRDFAcceptHeader);
            var request = builder.build();
            HttpResponse<InputStream> response;
            try {
                LOG.info("Sending request to [{}]", uri);
                response = httpClientBuilder()
                        .sslContext(SSLContext.getInstance(SSLContext.getDefault().getProtocol()))
                        .build().send(request, HttpResponse.BodyHandlers.ofInputStream());
                // Ensure we fully consume the response to avoid any blocked threads.
                var bos = new ByteArrayOutputStream();
                IOUtils.copy(response.body(), bos);
                HttpLib.handleHttpStatusCode(response);
                result = new TypedInputStream(new ByteArrayInputStream(bos.toByteArray()), HttpLib.responseHeader(response, HttpNames.hContentType));
            } catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
                throw new IllegalStateException(String.format("Unexpected error while reading URI [%s]", uri), e);
            } finally {
                LOG.info("Received response from [{}]", uri);
            }
        }
        return result;
    }

}