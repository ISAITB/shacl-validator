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

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.SparqlQueryConfig;
import eu.europa.ec.itb.shacl.util.ShaclValidatorUtils;
import eu.europa.ec.itb.validation.commons.BaseFileManager;
import eu.europa.ec.itb.validation.commons.FileInfo;
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
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static eu.europa.ec.itb.shacl.util.ShaclValidatorUtils.handleEquivalentContentSyntaxes;

/**
 * Manages file-system operations.
 */
@Component
public class FileManager extends BaseFileManager<ApplicationConfig> {

    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

    @Autowired
    private ApplicationConfig appConfig;

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
