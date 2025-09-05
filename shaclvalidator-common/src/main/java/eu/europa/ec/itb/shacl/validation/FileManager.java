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

package eu.europa.ec.itb.shacl.validation;

import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.http.DefaultHttpClient;
import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.loader.FileLoader;
import com.apicatalog.jsonld.loader.HttpLoader;
import com.apicatalog.jsonld.loader.SchemeRouter;
import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.SparqlQueryConfig;
import eu.europa.ec.itb.shacl.ValidationSpecs;
import eu.europa.ec.itb.shacl.util.ShaclValidatorUtils;
import eu.europa.ec.itb.validation.commons.BaseFileManager;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.artifact.RemoteValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.http.auth.AuthEnv;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapStd;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static eu.europa.ec.itb.shacl.util.ShaclValidatorUtils.handleEquivalentContentSyntaxes;
import static eu.europa.ec.itb.shacl.util.ShaclValidatorUtils.isRdfContentSyntax;
import static org.apache.jena.riot.lang.LangJSONLD11.JSONLD_OPTIONS;

/**
 * Manages file-system operations.
 */
@Component
public class FileManager extends BaseFileManager<ApplicationConfig> {

    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    @Autowired
    private ApplicationConfig appConfig;

    private final ConcurrentHashMap<String, Path> shaclModelCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Model> materialisedShaclModelCache = new ConcurrentHashMap<>();
    private final Set<Model> cachedModels = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    private final ReentrantLock cacheLock = new ReentrantLock();

    /**
     * Create a cache key to use for SHACL model file lookup.
     *
     * @param domainConfig The domain configuration.
     * @param validationType The validation type to consider.
     * @param contentSyntax The content systax of the report.
     * @return The key.
     */
    private String toShaclModelCacheKey(DomainConfig domainConfig, String validationType, String contentSyntax) {
        return "%s|%s|%s".formatted(domainConfig.getDomain(), validationType, contentSyntax);
    }

    /**
     * @see BaseFileManager#getFileExtension(String)
     *
     * @param contentType The content type (as a mime type).
     * @return The file extension (without dot) that corresponds to the provided RDF syntax.
     */
    @Override
    public String getFileExtension(String contentType) {
        String extension = null;
        Lang language = RDFLanguages.contentTypeToLang(handleEquivalentContentSyntaxes(contentType));
        if (language != null) {
            extension = language.getFileExtensions().get(0);
        }
        return extension;
    }

    /**
     * @see BaseFileManager#getContentTypeForFile(FileInfo, String)
     *
     * @param file The file of which to retrieve the content type.
     * @param declaredContentType The content type declared in the validator's inputs.
     * @return The content type to consider.
     */
    @Override
    protected String getContentTypeForFile(FileInfo file, String declaredContentType) {
        boolean noContentSyntaxProvided = StringUtils.isBlank(declaredContentType);
        /*
         * Step 1: Use the content type provided as part of the inputs. If none is provided then
         * attempt to determine the content type from the name of the file being processed.
         *
         * Step 2: In case as part of the file loading process we determined a content type,
         * consider this as well. Practically this only occurs when the file is loaded from a remote server
         * in which case the server may have returned a content type as part of the response's headers.
         */
        if (noContentSyntaxProvided) {
            ContentType detectedContentType = RDFLanguages.guessContentType(file.getFile().getName());
            if (detectedContentType != null) {
                declaredContentType = detectedContentType.getContentTypeStr();
            }
            // Only override the content syntax if one has not been explicitly provided as part of the input.
            declaredContentType = ShaclValidatorUtils.contentSyntaxToUse(declaredContentType, file.getType());
        }
        return declaredContentType;
    }

    /**
     * We post-process the downloaded file to make sure that if the configuration specified a content type, this
     * is applied to the file if needed.
     *
     * @param declaredFileInfo The declared file information.
     * @param downloadedFile The downloaded file.
     * @return The file to record.
     */
    @Override
    protected FileInfo postProcessDownloadedRemoteFile(RemoteValidationArtifactInfo declaredFileInfo, FileInfo downloadedFile) {
        if (declaredFileInfo.getType() != null && (downloadedFile.getType() == null || !isRdfContentSyntax(downloadedFile.getType()))) {
            return new FileInfo(downloadedFile.getFile(), declaredFileInfo.getType(), downloadedFile.getSource());
        } else {
            return downloadedFile;
        }
    }

    /**
     * Convert the provided RDF model to a string.
     *
     * @param rdfModel the RDF model.
     * @param outputSyntax The RDF syntax to use for the serialisation.
     * @return The RDF content as a string.
     */
    public String writeRdfModelToString(Model rdfModel, String outputSyntax) {
        StringWriter out = new StringWriter();
        writeRdfModel(out, rdfModel, outputSyntax);
        return out.toString();
    }

    /**
     * Write the provided RDF model to the provided stream.
     *
     * @param outputStream The stream to write to.
     * @param rdfModel The RDF model.
     * @param outputSyntax The RDF syntax to use for the serialisation.
     */
    public void writeRdfModel(OutputStream outputStream, Model rdfModel, String outputSyntax) {
        writeRdfModel(new OutputStreamWriter(outputStream), rdfModel, outputSyntax);
    }

    /**
     * Write the provided SHACL shape model to the output path.
     *
     * @param outputPath The output path to write to.
     * @param rdfModel The model to write.
     * @param validationType The current validation type.
     * @param outputSyntax The output syntax.
     * @param domainConfig The current domain configuration.
     * @throws IOException If an IO error occurs.
     */
    public void writeShaclShapes(Path outputPath, Model rdfModel, String validationType, String outputSyntax, DomainConfig domainConfig) throws IOException {
        if (outputSyntax == null) {
            outputSyntax = domainConfig.getDefaultReportSyntax();
            if (outputSyntax == null) {
                outputSyntax = appConfig.getDefaultReportSyntax();
            }
        }
        String outputSyntaxToUse = outputSyntax;
        if (domainConfig.canCacheShapes(validationType)) {
            // Write the model to a file and cache it.
            Path cachedPath;
            cacheLock.lock();
            try {
                String cacheKey = toShaclModelCacheKey(domainConfig, validationType, outputSyntax);
                cachedPath = shaclModelCache.get(cacheKey);
                if (cachedPath == null || !Files.exists(cachedPath)) {
                    // Initialise in case of first access or in case file was removed.
                    cachedPath = storeShapeGraph(rdfModel, outputSyntaxToUse);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Cached shape model for [{}] at [{}]", cacheKey, cachedPath.toAbsolutePath());
                    }
                    shaclModelCache.put(cacheKey, cachedPath);
                    materialisedShaclModelCache.put(cacheKey, rdfModel);
                    cachedModels.add(rdfModel);
                }
            } finally {
                cacheLock.unlock();
            }
            Files.copy(cachedPath, outputPath);
        } else {
            try (var out = new FileWriter(outputPath.toFile())) {
                writeRdfModel(out, rdfModel, outputSyntaxToUse);
            }
        }
    }

    /**
     * Build the RDF model from the provided stream.
     *
     * @param dataStream The stream to read from.
     * @param rdfLanguage The content type of the stream's data.
     * @return The parsed model.
     */
    public Model readModel(InputStream dataStream, Lang rdfLanguage, Map<String, String> nsPrefixes) {
        var builder = RDFParserBuilder
                .create()
                .lang(rdfLanguage)
                .source(dataStream);
        if (nsPrefixes != null) {
            // Before parsing set the prefixes of the model to avoid mismatches.
            PrefixMap prefixes = new PrefixMapStd();
            prefixes.putAll(nsPrefixes);
            builder = builder.prefixes(prefixes);
        }
        if (Lang.JSONLD11.equals(rdfLanguage) || Lang.JSONLD.equals(rdfLanguage)) {
            var options = new JsonLdOptions();
            var httpLoader = new HttpLoader(DefaultHttpClient.defaultInstance());
            /*
             * Set fallback type for remote contexts to avoid errors for non JSON/JSON-LD Content Types.
             * This allows us to proceed if e.g. the Content Type originally returned is "text/plain".
             */
            httpLoader.setFallbackContentType(MediaType.JSON);
            options.setDocumentLoader(new SchemeRouter()
                    .set("http", httpLoader)
                    .set("https", httpLoader)
                    .set("file", new FileLoader()));
            builder = builder.context(Context.create().set(JSONLD_OPTIONS, options));
        }
        return builder.build().toModel();
    }

    /**
     * Check to see if the provided model is cached.
     *
     * @param modelToCheck The model to check.
     * @return The check result.
     */
    public boolean isCachedModel(Model modelToCheck) {
        return modelToCheck != null && cachedModels.contains(modelToCheck);
    }

    /**
     * Add the provided model to the cache if possible.
     *
     * @param specs The current validation settings.
     * @param shapeModel The model to cache.
     */
    private void cacheShapeModelIfPossible(ValidationSpecs specs, Model shapeModel) {
        if (specs.getDomainConfig().canCacheShapes(specs.getValidationType()) && !specs.isLoadImports()) {
            String cacheKey = toShaclModelCacheKey(specs.getDomainConfig(), specs.getValidationType(), specs.getDomainConfig().getDefaultReportSyntax());
            cacheLock.lock();
            try {
                materialisedShaclModelCache.putIfAbsent(cacheKey, shapeModel);
                cachedModels.add(shapeModel);
            } finally {
                cacheLock.unlock();
            }
        }
    }

    /**
     * Get the shapes model from the cache or from the provided supplier function.
     *
     * @param specs The current validation settings.
     * @param modelSupplierIfNotCached Supplier for the model if it was not found in the cache.
     * @return The model to use.
     */
    public Model getShapeModel(ValidationSpecs specs, Supplier<Model> modelSupplierIfNotCached) {
        Model cachedModel = null;
        String cacheKey = toShaclModelCacheKey(specs.getDomainConfig(), specs.getValidationType(), specs.getDomainConfig().getDefaultReportSyntax());
        if (specs.isLoadImports()) {
            if (specs.isLogProgress() && logger.isDebugEnabled()) {
                logger.debug("Cached shape model for [{}] not loaded as we are loading imports", cacheKey);
            }
        } else {
            cacheLock.lock();
            try {
                Model materialisedModel = materialisedShaclModelCache.get(cacheKey);
                if (materialisedModel == null) {
                    Path modelPath = shaclModelCache.get(cacheKey);
                    if (modelPath != null) {
                        try (InputStream dataStream = Files.newInputStream(modelPath)) {
                            materialisedModel = readModel(dataStream, RDFLanguages.contentTypeToLang(specs.getDomainConfig().getDefaultReportSyntax()), null);
                        } catch (IOException e) {
                            throw new ValidatorException("validator.label.exception.errorReadingShaclFile", e);
                        }
                        materialisedShaclModelCache.put(cacheKey, materialisedModel);
                        cachedModel = materialisedModel;
                    }
                } else {
                    cachedModel = materialisedModel;
                }
            } finally {
                cacheLock.unlock();
            }
            if (specs.isLogProgress() && logger.isDebugEnabled()) {
                if (cachedModel == null) {
                    logger.debug("Cached shape model for [{}] not found", cacheKey);
                } else {
                    logger.debug("Cached shape model for [{}] found", cacheKey);
                }
            }
        }
        cachedModel = Objects.requireNonNullElseGet(cachedModel, () -> {
           Model loadedModel = modelSupplierIfNotCached.get();
           cacheShapeModelIfPossible(specs, loadedModel);
           return loadedModel;
        });
        return cachedModel;
    }

    /**
     * Write the provided RDF model to the SHACL shape cache.
     *
     * @param rdfModel The shape model.
     * @param outputSyntaxToUse The shapes' content type.
     * @return The stored path.
     */
    private Path storeShapeGraph(Model rdfModel, String outputSyntaxToUse) {
        Path shaclModelCacheFolder = getTempFolder().toPath().resolve("shacl-model-cache");
        try {
            Files.createDirectories(shaclModelCacheFolder);
            Path shaclModelFile = shaclModelCacheFolder.resolve(UUID.randomUUID().toString());
            try (var out = new OutputStreamWriter(Files.newOutputStream(shaclModelFile))) {
                writeRdfModel(out, rdfModel, outputSyntaxToUse);
            }
            return shaclModelFile;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to cache aggregated shape model file", e);
        }
    }

    /**
     * Write the provided RDF model to the provided writer.
     *
     * @param outputWriter The writer to write to.
     * @param rdfModel The RDF model.
     * @param outputSyntax The RDF syntax to use for the serialisation.
     */
    public void writeRdfModel(Writer outputWriter, Model rdfModel, String outputSyntax) {
        if (outputSyntax == null) {
            outputSyntax = appConfig.getDefaultReportSyntax();
        }
        Lang lang = RDFLanguages.contentTypeToLang(outputSyntax);
        if (lang == null) {
            // Set to RDF/XML (the global default) for an unrecognised or unspecified syntax.
            lang = Lang.RDFXML;
        }
        var writer = rdfModel.getWriter(lang.getName());
        if (Lang.RDFXML.equals(lang)) {
            // Adapt the writer to handle potentially bad URIs and speed up RDF/XML processing.
            writer.setProperty("allowBadURIs", "true");
            writer.setProperty("relativeURIs", "");
        }
        try (outputWriter) {
            writer.write(rdfModel, outputWriter, null);
            outputWriter.flush();
        } catch (IOException e) {
            logger.error("Error writing RDF model", e);
            throw new IllegalStateException("Error writing RDF model", e);
        }
    }

    /**
     * Remove the external files linked to the validation.
     *
     * @param inputFile The file to remove.
     * @param externalShaclFiles The user-provided SHACL shape files.
     */
    public void removeContentToValidate(File inputFile, List<FileInfo> externalShaclFiles) {
        // Remove content that was validated.
        if (inputFile != null && inputFile.exists() && inputFile.isFile()) {
            FileUtils.deleteQuietly(inputFile);
        }
        // Remove externally provided SHACL files.
        if (externalShaclFiles != null) {
            for (FileInfo externalFile: externalShaclFiles) {
                FileUtils.deleteQuietly(externalFile.getFile());
            }
        }
    }

    /**
     * @return The root documentation folder for the Hydra docs.
     */
    public File getHydraDocsRootFolder() {
        return new File(config.getTmpFolder(), "hydra");
    }

    /**
     * Get the Hydra documentation folder for a given domain.
     *
     * @param domainName The domain.
     * @return The folder.
     */
    public File getHydraDocsFolder(String domainName) {
        return new File(getHydraDocsRootFolder(), domainName);
    }

    /**
     * Get the content to validate from a SPARQL endpoint and persist to a file in the validator's temp folder.
     *
     * @param queryConfig The SPARQL query configuration to use.
     * @param parentFolder The folder within which to store the retrieved RDF content.
     * @param fileName The name to use for the file in which the content is stored.
     * @return The path to the stored file.
     */
    public Path getContentFromSparqlEndpoint(SparqlQueryConfig queryConfig, File parentFolder, String fileName) {
        Model resultModel;
        AuthEnv.get().registerUsernamePassword(URI.create(queryConfig.getEndpoint()), queryConfig.getUsername(), queryConfig.getPassword());
        try (var query = QueryExecutionHTTP.service(queryConfig.getEndpoint())
                .query(getQuery(queryConfig.getQuery()))
                .acceptHeader(queryConfig.getPreferredContentType())
                .build()) {
            resultModel = query.execConstruct();
        } catch (Exception e) {
            throw new ValidatorException("validator.label.exception.sparqlQueryError", e, e.getMessage());
        }
        Path modelPath = null;
        if (resultModel != null) {
            modelPath = createFile(parentFolder, getFileExtension(queryConfig.getPreferredContentType()), fileName);
            try {
                writeRdfModel(new FileWriter(modelPath.toFile()), resultModel, queryConfig.getPreferredContentType());
            } catch (IOException e) {
                throw new ValidatorException("validator.label.exception.sparqlQueryErrorInContentProcessing", e);
            }
        }
        return modelPath;
    }

    /**
     * Get the content to validate from a SPARQL endpoint and persist to a file in the validator's temp folder.
     *
     * @param queryConfig The SPARQL query configuration to use.
     * @param parentFolder The folder within which to store the retrieved RDF content.
     * @return The path to the stored file.
     */
    public Path getContentFromSparqlEndpoint(SparqlQueryConfig queryConfig, File parentFolder) {
        return this.getContentFromSparqlEndpoint(queryConfig, parentFolder, null);
    }

    /**
     * Construct a SPARQL query from the provided string.
     *
     * @param sQuery The query text.
     * @return The query to use.
     * @throws ValidatorException if the qu
     */
    private Query getQuery(String sQuery) {
        try {
            Query query = QueryFactory.create(sQuery);
            if (query.isConstructType()) {
                return query;
            } else {
                logger.error("The input query must be a CONSTRUCT query.");
                throw new ValidatorException("validator.label.exception.sparqlQueryMustBeConstruct");
            }
        } catch(QueryException e) {
            logger.error("Error getting SPARQL Query", e);
            throw new ValidatorException("validator.label.exception.sparqlQueryParsingError", e, e.getMessage());
        }
    }

}
