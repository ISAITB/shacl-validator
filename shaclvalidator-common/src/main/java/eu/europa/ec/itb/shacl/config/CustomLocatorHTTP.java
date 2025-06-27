/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import static org.apache.jena.http.HttpEnv.httpClientBuilder;

/**
 * Replacement of Jena's built-in {@link LocatorHTTP} which resulted in blocked threads when encountering certain types
 * of failures during the loading of remote resources. The problem was due to the reuse of a static HTTP client that if
 * not closed correctly resulted in blocked threads. This replacement implementation replicates the steps taken but
 * ensures that a fresh HTTP client instance is constructed for each request.
 *
 * In addition, this implementation follows deep redirects whereby a URI results in a 301 and then another 30X that leads
 * again to a 301. In the JDK's HTTP client followed redirects stop if another 301 is encountered. The current implementation
 * detects such cases and keeps on following redirects as long as there is no redirect loop.
 */
public class CustomLocatorHTTP extends LocatorHTTP {

    public static final ThreadLocal<LocatorParams> PARAMS = new ThreadLocal<>();
    private static final Logger LOG = LoggerFactory.getLogger(CustomLocatorHTTP.class);

    /**
     * Convert URI string to an HTTP request.
     *
     * @param uri The URI.
     * @param httpVersion The HTTP version to use.
     * @return The request.
     */
    private HttpRequest toRequest(String uri, HttpClient.Version httpVersion) {
        return HttpLib.requestBuilderFor(uri).uri(HttpLib.toRequestURI(uri))
            .GET()
            .version(httpVersion)
            .header(HttpNames.hAccept, WebContent.defaultRDFAcceptHeader)
            .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypedInputStream performOpen(String uri) {
        TypedInputStream result = null;
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            var params = PARAMS.get();
            if (params.urisToSkip().contains(uri)) {
                LOG.debug("Skipped URI {}", uri);
            } else {
                HttpResponse<InputStream> response;
                var attemptedUrls = new HashSet<String>();
                var uriToUse = uri;
                try (HttpClient client = httpClientBuilder()
                        .sslContext(SSLContext.getInstance(SSLContext.getDefault().getProtocol()))
                        .build()) {
                    do {
                        LOG.debug("Sending request to [{}]", uriToUse);
                        attemptedUrls.add(uriToUse);
                        response = client.send(toRequest(uriToUse, params.httpVersion()), HttpResponse.BodyHandlers.ofInputStream());
                        if (response.statusCode() >= 300 && response.statusCode() <= 399) {
                            var nextLocation = response.headers().firstValue("Location");
                            if (nextLocation.isEmpty()) {
                                nextLocation = response.headers().firstValue("location");
                            }
                            if (nextLocation.isPresent()) {
                                uriToUse = nextLocation.get();
                            }
                        }
                    } while (!attemptedUrls.contains(uriToUse));
                    // Ensure we fully consume the response to avoid any blocked threads.
                    var bos = new ByteArrayOutputStream();
                    IOUtils.copy(response.body(), bos);
                    HttpLib.handleHttpStatusCode(response);
                    result = new TypedInputStream(new ByteArrayInputStream(bos.toByteArray()), HttpLib.responseHeader(response, HttpNames.hContentType));
                } catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(String.format("Unexpected error while reading URI [%s]", uri), e);
                } finally {
                    LOG.debug("Received response from [{}]", uri);
                }
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return "CustomLocatorHTTP" ;
    }

    /**
     * Parameters defining how remote resources are to be handled.
     *
     * @param urisToSkip The URIs to skip looking.
     * @param httpVersion The HTTP version to use in the lookups.
     */
    public record LocatorParams(Set<String> urisToSkip, HttpClient.Version httpVersion) {}

}