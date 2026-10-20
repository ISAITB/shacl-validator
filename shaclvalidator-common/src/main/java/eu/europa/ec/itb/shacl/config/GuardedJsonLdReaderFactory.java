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

import com.apicatalog.jsonld.JsonLdOptions;
import eu.europa.ec.itb.validation.commons.ImportedUriAuthorizer;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParserRegistry;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.ReaderRIOTFactory;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.lang.LangJSONLD11;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.sparql.util.Context;

import java.io.InputStream;
import java.io.Reader;

/**
 * Factory for JSON-LD readers that makes sure that no remote resources are loaded without authorization.
 * <p>
 * Parsers we construct ourselves are explicitly configured with a guarded document loader. This factory is a safety net
 * for all other parsing that goes through Jena's parser registry (e.g. {@code Model.read} calls, or the loading of
 * JSON-LD documents referred to by {@code owl:imports}), for which Jena would otherwise use a default document loader
 * that loads any HTTP(S) or file URI.
 * <p>
 * If parsing is made within the context of the loading of imports (see {@link LocatorParams#PARAMS}) the restrictions
 * recorded there apply, otherwise the restrictions for user-provided content apply and no files may be loaded.
 */
public class GuardedJsonLdReaderFactory implements ReaderRIOTFactory {

    private final ReaderRIOTFactory delegate;
    private final ImportedUriAuthorizer defaultUriAuthorizer;

    /**
     * Constructor.
     *
     * @param delegate The factory to delegate the creation of readers to.
     * @param defaultUriAuthorizer The URI authorizer to apply if no restrictions are recorded for the current thread.
     */
    GuardedJsonLdReaderFactory(ReaderRIOTFactory delegate, ImportedUriAuthorizer defaultUriAuthorizer) {
        this.delegate = delegate;
        this.defaultUriAuthorizer = defaultUriAuthorizer;
    }

    /**
     * Replace the registered JSON-LD parsers with ones that guard the loading of remote resources. Calling this
     * multiple times is safe.
     *
     * @param defaultUriAuthorizer The URI authorizer to apply if no restrictions are recorded for the current thread.
     */
    public static void install(ImportedUriAuthorizer defaultUriAuthorizer) {
        JenaSystem.init();
        for (Lang lang : new Lang[] { Lang.JSONLD, Lang.JSONLD11 }) {
            boolean triples = RDFParserRegistry.isTriples(lang);
            boolean quads = RDFParserRegistry.isQuads(lang);
            ReaderRIOTFactory current = RDFParserRegistry.getFactory(lang);
            if (current instanceof GuardedJsonLdReaderFactory guarded) {
                current = guarded.delegate;
            }
            var factory = new GuardedJsonLdReaderFactory(current, defaultUriAuthorizer);
            if (triples) {
                RDFParserRegistry.registerLangTriples(lang, factory);
            }
            if (quads) {
                RDFParserRegistry.registerLangQuads(lang, factory);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReaderRIOT create(Lang language, ParserProfile profile) {
        return new GuardedReader(delegate.create(language, profile));
    }

    /**
     * Reader that ensures guarded options are present before delegating.
     */
    private class GuardedReader implements ReaderRIOT {

        private final ReaderRIOT reader;

        private GuardedReader(ReaderRIOT reader) {
            this.reader = reader;
        }

        @Override
        public void read(InputStream in, String baseURI, ContentType ct, StreamRDF output, Context context) {
            var loader = new GuardedJsonLdDocumentLoader[1];
            var guardedContext = guard(context, loader);
            try {
                reader.read(in, baseURI, ct, output, guardedContext);
            } catch (RiotException e) {
                throw loader[0] == null ? e : loader[0].toReportedError(e);
            }
        }

        @Override
        public void read(Reader in, String baseURI, ContentType ct, StreamRDF output, Context context) {
            var loader = new GuardedJsonLdDocumentLoader[1];
            var guardedContext = guard(context, loader);
            try {
                reader.read(in, baseURI, ct, output, guardedContext);
            } catch (RiotException e) {
                throw loader[0] == null ? e : loader[0].toReportedError(e);
            }
        }

        private Context guard(Context context, GuardedJsonLdDocumentLoader[] loaderHolder) {
            if (context != null) {
                Object options = context.get(LangJSONLD11.JSONLD_OPTIONS);
                if (options instanceof JsonLdOptions jsonLdOptions && jsonLdOptions.getDocumentLoader() instanceof GuardedJsonLdDocumentLoader) {
                    // Already configured explicitly.
                    return context;
                }
            }
            var params = LocatorParams.PARAMS.get();
            ResourceAccessPolicy policy;
            if (params == null) {
                policy = new ResourceAccessPolicy(defaultUriAuthorizer, null);
            } else {
                policy = new ResourceAccessPolicy(params.uriAuthorizer(), params.fileAuthorizer());
            }
            Context guarded = context == null ? Context.create() : context.copy();
            loaderHolder[0] = GuardedJsonLdDocumentLoader.forPolicy(policy);
            guarded.set(LangJSONLD11.JSONLD_OPTIONS, GuardedJsonLdDocumentLoader.newOptions(loaderHolder[0]));
            return guarded;
        }
    }

}
