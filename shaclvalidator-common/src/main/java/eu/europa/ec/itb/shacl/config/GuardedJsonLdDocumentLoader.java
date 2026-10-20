/*
 * Copyright (C) 2026 European Union
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

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdErrorCode;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.apicatalog.jsonld.loader.FileLoader;
import com.apicatalog.jsonld.loader.HttpLoader;
import eu.europa.ec.itb.validation.commons.ImportedFileAuthorizer;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Loader of remote JSON-LD documents (e.g. contexts) that only loads resources that are permitted. Only HTTP(S) URIs and
 * files are supported, each subject to authorization before any access is made.
 * <p>
 * The default loader of the JSON-LD processor loads any HTTP(S) or file URI, making it possible for a crafted
 * {@literal @context} to trigger requests to arbitrary internal resources.
 */
public class GuardedJsonLdDocumentLoader implements DocumentLoader {

    private static final Logger LOG = LoggerFactory.getLogger(GuardedJsonLdDocumentLoader.class);

    private final DocumentLoader httpLoader;
    private final DocumentLoader fileLoader;
    private final ImportedFileAuthorizer fileAuthorizer;
    private volatile ValidatorException blocked;

    /**
     * Constructor.
     *
     * @param uriAuthorizer The authorizer for HTTP(S) URIs (if null no restrictions are applied).
     * @param fileAuthorizer The authorizer for files (if null no files can be loaded).
     */
    public GuardedJsonLdDocumentLoader(ImportedUriAuthorizer uriAuthorizer, ImportedFileAuthorizer fileAuthorizer) {
        var loader = new HttpLoader(new AuthorizingJsonLdHttpClient(uriAuthorizer));
        /*
         * Set fallback type for remote contexts to avoid errors for non JSON/JSON-LD Content Types.
         * This allows us to proceed if e.g. the Content Type originally returned is "text/plain".
         */
        loader.fallbackContentType(MediaType.JSON);
        this.httpLoader = loader;
        this.fileLoader = new FileLoader();
        this.fileAuthorizer = fileAuthorizer;
    }

    /**
     * Create the JSON-LD processing options to use for a single parsing operation.
     *
     * @param loader The loader to use.
     * @return The options (never share these between parsing operations as the processor modifies them).
     */
    public static JsonLdOptions newOptions(GuardedJsonLdDocumentLoader loader) {
        var options = new JsonLdOptions();
        options.setDocumentLoader(loader);
        return options;
    }

    /**
     * Create a loader to use for a single parsing operation.
     *
     * @param policy The restrictions to apply to referenced resources.
     * @return The loader.
     */
    public static GuardedJsonLdDocumentLoader forPolicy(ResourceAccessPolicy policy) {
        return new GuardedJsonLdDocumentLoader(policy.uriAuthorizer(), policy.fileAuthorizer());
    }

    /**
     * The JSON-LD parser reports a failure to load a resource as a generic parsing error. If the cause was a resource
     * that is not permitted, this is the original (localised) exception that should be reported instead.
     *
     * @param parsingError The error reported by the parser.
     * @return The exception to throw in place of the parsing error.
     */
    public RuntimeException toReportedError(RuntimeException parsingError) {
        return blocked == null ? parsingError : blocked;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {
        try {
            String scheme = url == null ? null : url.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                return httpLoader.loadDocument(url, options);
            } else if ("file".equalsIgnoreCase(scheme)) {
                if (fileAuthorizer == null) {
                    throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, "Loading of files is not permitted.");
                }
                fileAuthorizer.isUriAllowed(url);
                return fileLoader.loadDocument(url, options);
            } else {
                throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, "URL scheme [" + scheme + "] is not supported.");
            }
        } catch (ValidatorException e) {
            blocked = e;
            throw e;
        } catch (JsonLdError e) {
            // The parser only reports a generic failure so we record the actual reason.
            LOG.warn("Unable to load JSON-LD document [{}]: {}", url, e.getCause() == null ? e.getMessage() : e.getCause().toString());
            throw e;
        }
    }

}
