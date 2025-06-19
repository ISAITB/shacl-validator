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

package eu.europa.ec.itb.shacl.util;

import com.apicatalog.jsonld.StringUtils;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.ReportSpecs;
import eu.europa.ec.itb.shacl.validation.SHACLReportHandler;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ReportPair;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.Objects;

/**
 * Class with utility methods.
 */
public class ShaclValidatorUtils {

    public static final MimeType TYPE_TEXT_XML = new MimeType("text", "xml");
    public static final MimeType TYPE_TEXT_TURTLE = new MimeType("text", "turtle");
    public static final MimeType TYPE_APPLICATION_XML = new MimeType("application", "xml");
    public static final MimeType TYPE_APPLICATION_RDF_XML = new MimeType("application", "rdf+xml");
    public static final MimeType TYPE_APPLICATION_JSON = new MimeType("application", "json");
    public static final MimeType TYPE_APPLICATION_JSON_LD = new MimeType("application", "ld+json");
    public static final MimeType TYPE_APPLICATION_N_TRIPLES = new MimeType("application", "n-triples");

    private static final Logger logger = LoggerFactory.getLogger(ShaclValidatorUtils.class);

    /**
     * Constructor to prevent instantiation.
     */
    private ShaclValidatorUtils() { throw new IllegalStateException("Utility class"); }

    /**
     * Create a TAR validation report using the provided specifications.
     *
     * @param reportSpecs The report generation specifications.
     * @return The TAR validation report pair (detailed and aggregate).
     */
    public static ReportPair getTAR(ReportSpecs reportSpecs) {
        SHACLReportHandler reportHandler = new SHACLReportHandler(reportSpecs);
        return reportHandler.createReport();
    }

    /**
     * Get the labels to use for the SHACL validation report's fixed texts.
     *
     * @param localisationHelper The localisation helper.
     * @return The labels.
     */
    public static SHACLReportHandler.ReportLabels getReportLabels(LocalisationHelper localisationHelper) {
        var labels = new SHACLReportHandler.ReportLabels();
        labels.setFocusNode(localisationHelper.localise("validator.label.reportItemFocusNode"));
        labels.setResultPath(localisationHelper.localise("validator.label.reportItemResultPath"));
        labels.setShape(localisationHelper.localise("validator.label.reportItemShape"));
        labels.setValue(localisationHelper.localise("validator.label.reportItemValue"));
        return labels;
    }

    /**
     * Get the RDF validation report content to include in the resulting TAR report's context.
     *
     * @param reportModel The model of the RDF validation report.
     * @param rdfReportMimeType The mime type for the RDF report.
     * @param reportProcessingQuery The SPARQL query to use for post-processing of the SHACL validation report.
     * @param fileManager The file manager instance to use.
     * @return The content to inject in the TAR report.
     */
    public static String getRdfReportToIncludeInTAR(Model reportModel, String rdfReportMimeType, String reportProcessingQuery, FileManager fileManager) {
        Model reportToInclude = reportModel;
        if (reportProcessingQuery != null && !reportProcessingQuery.isBlank()) {
            Query query = QueryFactory.create(reportProcessingQuery);
            try (QueryExecution queryExecution = QueryExecutionFactory.create(query, reportToInclude)) {
                reportToInclude = queryExecution.execConstruct();
            }
        }
        return fileManager.writeRdfModelToString(reportToInclude, rdfReportMimeType);
    }

    /**
     * Make sure badly reported content types are correctly handled.
     *
     * @param contentSyntax The input content syntax.
     * @return The RDF content syntax to consider.
     */
    public static String handleEquivalentContentSyntaxes(String contentSyntax) {
        if (StringUtils.isNotBlank(contentSyntax)) {
            var resolvedType = handleEquivalentContentSyntaxes(MimeTypeUtils.parseMimeType(contentSyntax));
            return "%s/%s".formatted(resolvedType.getType(), resolvedType.getSubtype());
        }
        return contentSyntax;
    }

    /**
     * Make sure badly reported content types are correctly handled.
     *
     * @param contentSyntax The input content syntax.
     * @return The RDF content syntax to consider.
     */
    public static MimeType handleEquivalentContentSyntaxes(MimeType contentSyntax) {
        if (contentSyntax != null) {
            if (TYPE_TEXT_XML.equalsTypeAndSubtype(contentSyntax) || TYPE_APPLICATION_XML.equalsTypeAndSubtype(contentSyntax)) {
                contentSyntax = TYPE_APPLICATION_RDF_XML;
            } else if (TYPE_APPLICATION_JSON.equalsTypeAndSubtype(contentSyntax)) {
                contentSyntax = TYPE_APPLICATION_JSON_LD;
            }
        }
        return contentSyntax;
    }

    /**
     * Check to see if the provided content syntax is valid for RDF content.
     *
     * @param contentSyntax The syntax to check.
     * @return The check result.
     */
    public static boolean isRdfContentSyntax(String contentSyntax) {
        if (StringUtils.isNotBlank(contentSyntax)) {
            var providedType = handleEquivalentContentSyntaxes(MimeTypeUtils.parseMimeType(contentSyntax));
            return isRdfContentSyntax(providedType);
        }
        return false;
    }

    /**
     * Check to see if the provided content syntax is valid for RDF content (this assumes equivalent syntaxes have already been applied).
     *
     * @param contentSyntax The syntax to check.
     * @return The check result.
     */
    private static boolean isRdfContentSyntax(MimeType contentSyntax) {
        if (contentSyntax != null) {
            return TYPE_APPLICATION_RDF_XML.equalsTypeAndSubtype(contentSyntax) ||
                    TYPE_APPLICATION_JSON_LD.equalsTypeAndSubtype(contentSyntax) ||
                    TYPE_APPLICATION_N_TRIPLES.equalsTypeAndSubtype(contentSyntax) ||
                    TYPE_TEXT_TURTLE.equalsTypeAndSubtype(contentSyntax);
        }
        return false;
    }

    /**
     * Determine the content type to use for given RDF content.
     *
     * @param contentSyntaxFromInput The content type determined from inputs (direct input or file name deduction).
     * @param candidateContentSyntax The content type to consider as a candidate to replace the inputted one.
     * @return The content type string to use.
     */
    public static String contentSyntaxToUse(String contentSyntaxFromInput, String candidateContentSyntax) {
        if (StringUtils.isNotBlank(candidateContentSyntax)) {
            MimeType candidateType = handleEquivalentContentSyntaxes(MimeTypeUtils.parseMimeType(candidateContentSyntax));
            if (isRdfContentSyntax(candidateType)) {
                return "%s/%s".formatted(candidateType.getType(), candidateType.getSubtype());
            }
        }
        return handleEquivalentContentSyntaxes(contentSyntaxFromInput);
    }

    /**
     * Check whether the provided severity level indicates an information message.
     *
     * @param severity The severity level
     * @return The check result.
     */
    public static boolean isInfoSeverity(String severity) {
        return Objects.equals(severity, "http://www.w3.org/ns/shacl#Info");
    }

    /**
     * Check whether the provided severity level indicates a warning message.
     *
     * @param severity The severity level
     * @return The check result.
     */
    public static boolean isWarningSeverity(String severity) {
        return Objects.equals(severity, "http://www.w3.org/ns/shacl#Warning");
    }

    /**
     * Check whether the provided severity level indicates an error message.
     *
     * @param severity The severity level
     * @return The check result.
     */
    public static boolean isErrorSeverity(String severity) {
        return !isInfoSeverity(severity) && !isWarningSeverity(severity);
    }

    /**
     * Convert the provided RDF statement to a string.
     *
     * @param statement The statement.
     * @return The resulting string.
     */
    public static String getStatementSafe(Statement statement) {
        String result = null;
        if (statement != null) {
            try {
                RDFNode node = statement.getObject();
                if (node.isAnon()) {
                    result = "";
                } else if (node.isLiteral()) {
                    result = node.asLiteral().getLexicalForm();
                } else {
                    result = node.toString();
                }
            } catch (Exception e) {
                logger.warn("Error while getting statement string", e);
                result = "";
            }
        }
        return result;
    }

}
