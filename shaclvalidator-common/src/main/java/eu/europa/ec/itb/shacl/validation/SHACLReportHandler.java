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

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import eu.europa.ec.itb.shacl.util.ShaclValidatorUtils;
import eu.europa.ec.itb.shacl.util.StatementTranslator;
import eu.europa.ec.itb.validation.commons.AggregateReportItems;
import eu.europa.ec.itb.validation.commons.ReportItemComparator;
import eu.europa.ec.itb.validation.commons.ReportPair;
import eu.europa.ec.itb.validation.commons.Utils;
import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.*;

import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.function.Function;

import static eu.europa.ec.itb.shacl.util.ShaclValidatorUtils.getStatementSafe;
import static eu.europa.ec.itb.shacl.validation.SHACLValidator.RESULT_MESSAGE_URI;
import static eu.europa.ec.itb.shacl.validation.SHACLValidator.RESULT_URI;

/**
 * Class to handle a SHACL validation report and produce a TAR report.
 */
public class SHACLReportHandler {

    private final ObjectFactory objectFactory = new ObjectFactory();
    private final AnyContent reportContext;
    private final ReportSpecs reportSpecs;

    /**
     * Constructor.
     *
     * @param reportSpecs The specification of how to generate the report.
     */
	public SHACLReportHandler(ReportSpecs reportSpecs) {
		this.reportSpecs = reportSpecs;
        reportContext = new AnyContent();
        if (reportSpecs.getInputContentToInclude() != null || reportSpecs.getShapesModel() != null || reportSpecs.getReportContentToInclude() != null) {
            reportContext.setType("map");
            if (reportSpecs.getInputContentToInclude() != null) {
                AnyContent inputAttachment = new AnyContent();
                inputAttachment.setName("input");
                inputAttachment.setType("string");
                inputAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
                inputAttachment.setValue(reportSpecs.getInputContentToInclude());
                reportContext.getItem().add(inputAttachment);
            }
            if (reportSpecs.getShapesModel() != null) {
                AnyContent shapeAttachment = new AnyContent();
                shapeAttachment.setName("shapes");
                shapeAttachment.setType("string");
                shapeAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
                shapeAttachment.setValue(modelToString(reportSpecs.getShapesModel()));
                reportContext.getItem().add(shapeAttachment);
            }
            if (reportSpecs.getReportContentToInclude() != null) {
                AnyContent reportAttachment = new AnyContent();
                reportAttachment.setName("report");
                reportAttachment.setType("string");
                reportAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
                reportAttachment.setValue(reportSpecs.getReportContentToInclude());
                reportContext.getItem().add(reportAttachment);
            }
        }
	}

    /**
     * Create the TAR report.
     *
     * @return The TAR report.
     */
	public ReportPair createReport() {
        var report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        report.setDate(Utils.getXMLGregorianCalendarDateTime());
        report.setReports(new TestAssertionGroupReportsType());
        report.setContext(reportContext);
        int infos = 0;
        int warnings = 0;
        int errors = 0;
        var additionalInfoTemplate = new AdditionalInfoTemplate(reportSpecs.getLocalisationHelper(), reportSpecs.getInputModel());
        AggregateReportItems aggregateReportItems = null;
        if (reportSpecs.isProduceAggregateReport()) {
            aggregateReportItems = new AggregateReportItems(objectFactory, reportSpecs.getLocalisationHelper());
        }
		if (reportSpecs.getReportModel() != null) {
            NodeIterator niValidationResult = reportSpecs.getReportModel().listObjectsOfProperty(reportSpecs.getReportModel().getProperty(RESULT_URI));
            var reports = new ArrayList<JAXBElement<TestAssertionReportType>>();

            while (niValidationResult.hasNext()) {
                RDFNode node = niValidationResult.next();
                StmtIterator it = reportSpecs.getReportModel().listStatements(node.asResource(), null, (RDFNode)null);

                BAR error = new BAR();
                String focusNode = "";
                String resultPath = "";
                String severity = "";
                String value = "";
                String shape = "";

                var statementTranslator = new StatementTranslator();
                while(it.hasNext()) {
                    Statement statement = it.next();
                    if(statement.getPredicate().hasURI(RESULT_MESSAGE_URI)) {
                        statementTranslator.processStatement(statement);
                    }
                    if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#focusNode")) {
                        focusNode = getStatementSafe(statement);
                    }
                    if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#resultPath")) {
                        resultPath = getStatementSafe(statement);
                    }
                    if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#sourceShape")) {
                        shape = getStatementSafe(statement);
                    }
                    if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#resultSeverity")) {
                        severity = getStatementSafe(statement);
                    }
                    if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#value")) {
                        value = getStatementSafe(statement);
                    }
                }
                if (focusNode != null && additionalInfoTemplate.isEnabled()) {
                    error.setAssertionID(additionalInfoTemplate.apply(focusNode));
                }
                error.setDescription(getStatementSafe(statementTranslator.getTranslation(reportSpecs.getLocalisationHelper().getLocale()).getMatchedStatement()));
                error.setLocation(createStringMessageFromParts(new String[] {reportSpecs.getReportLabels().getFocusNode(), reportSpecs.getReportLabels().getResultPath()}, new String[] {focusNode, resultPath}));
                error.setTest(createStringMessageFromParts(new String[] {reportSpecs.getReportLabels().getShape(), reportSpecs.getReportLabels().getValue()}, new String[] {shape, value}));
                JAXBElement<TestAssertionReportType> element;
                String shapeFinal = shape;
                Function<JAXBElement<TestAssertionReportType>, String> classifierFn = e -> String.format("%s|%s|%s", shapeFinal, e.getName().getLocalPart(), ((BAR)e.getValue()).getDescription());
                if (ShaclValidatorUtils.isInfoSeverity(severity)) {
                    element = this.objectFactory.createTestAssertionGroupReportsTypeInfo(error);
                    infos += 1;
                    if (aggregateReportItems != null) aggregateReportItems.updateForReportItem(element, classifierFn);
                } else if (ShaclValidatorUtils.isWarningSeverity(severity)) {
                    element = this.objectFactory.createTestAssertionGroupReportsTypeWarning(error);
                    warnings += 1;
                    if (aggregateReportItems != null) aggregateReportItems.updateForReportItem(element, classifierFn);
                } else { // ERROR, FATAL_ERROR
                    element = this.objectFactory.createTestAssertionGroupReportsTypeError(error);
                    errors += 1;
                    if (aggregateReportItems != null) aggregateReportItems.updateForReportItem(element, classifierFn);
                }
                reports.add(element);
            }
            report.getReports().getInfoOrWarningOrError().addAll(reports);
		} else {
            BAR error1 = new BAR();
            error1.setDescription(reportSpecs.getLocalisationHelper().localise("validator.label.exception.unableToGenerateReportDueToContentProblem"));
            var element1 = this.objectFactory.createTestAssertionGroupReportsTypeError(error1);
            report.getReports().getInfoOrWarningOrError().add(element1);
            errors += 1;
        }
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfErrors(BigInteger.valueOf(errors));
        report.getCounters().setNrOfAssertions(BigInteger.valueOf(infos));
        report.getCounters().setNrOfWarnings(BigInteger.valueOf(warnings));
        if (errors > 0) {
            report.setResult(TestResultType.FAILURE);
        } else {
            report.setResult(TestResultType.SUCCESS);
        }
        if (reportSpecs.isReportItemsOrdered()) {
            report.getReports().getInfoOrWarningOrError().sort(new ReportItemComparator());
        }
        reportSpecs.getDomainConfig().applyMetadata(report, reportSpecs.getValidationType());
        if (report.getName() == null) {
            report.setName("SHACL Validation");
        }
        // Create the aggregate report if needed.
        TAR aggregateReport = null;
        if (aggregateReportItems != null) {
            aggregateReport = new TAR();
            aggregateReport.setContext(new AnyContent());
            aggregateReport.setResult(report.getResult());
            aggregateReport.setCounters(report.getCounters());
            aggregateReport.setOverview(report.getOverview());
            aggregateReport.setDate(report.getDate());
            aggregateReport.setName(report.getName());
            aggregateReport.setReports(new TestAssertionGroupReportsType());
            aggregateReport.getReports().getInfoOrWarningOrError().addAll(aggregateReportItems.getReportItems());
            if (reportSpecs.isReportItemsOrdered()) {
                aggregateReport.getReports().getInfoOrWarningOrError().sort(new ReportItemComparator());
            }
        }
        Utils.sanitizeIfNeeded(report, reportSpecs.getDomainConfig());
        Utils.sanitizeIfNeeded(aggregateReport, reportSpecs.getDomainConfig());
		return new ReportPair(report, aggregateReport);
	}

    /**
     * Create a report item message from the provided parts.
     *
     * @param labels The labels to use.
     * @param values The values to use.
     * @return The string.
     */
    private String createStringMessageFromParts(String[] labels, String[] values) {
	    if (labels.length != values.length) {
            throw new IllegalArgumentException("Wrong number of arguments supplied ["+labels.length+"]["+values.length+"]");
        }
        StringBuilder str = new StringBuilder();
        for (int i=0; i < labels.length; i++) {
	        if (StringUtils.isNotBlank(values[i])) {
	            if (!str.isEmpty()) {
	                str.append(" - ");
                }
	            str.append(String.format("[%s] - [%s]", labels[i], values[i]));
            }
        }
        if (!str.isEmpty()) {
            return str.toString();
        } else {
            return null;
        }
    }

    /**
     * Convert the provided SHACL report model to a string.
     *
     * @param shaclReport The report.
     * @return The string.
     */
    private String modelToString(Model shaclReport) {
		StringWriter writer = new StringWriter();		
		shaclReport.write(writer);
		return writer.toString();
    }

    /**
     * Class used to define fixed texts used within the report.
     */
    public static class ReportLabels {

        private String focusNode;
        private String resultPath;
        private String shape;
        private String value;

        /**
         * @return The focus node label.
         */
        public String getFocusNode() {
            return focusNode;
        }

        /**
         * @param focusNode The focus node label.
         */
        public void setFocusNode(String focusNode) {
            this.focusNode = focusNode;
        }

        /**
         * @return The result path label.
         */
        public String getResultPath() {
            return resultPath;
        }

        /**
         * @param resultPath The result path label.
         */
        public void setResultPath(String resultPath) {
            this.resultPath = resultPath;
        }

        /**
         * @return The shape label.
         */
        public String getShape() {
            return shape;
        }

        /**
         * @param shape The shape label.
         */
        public void setShape(String shape) {
            this.shape = shape;
        }

        /**
         * @return The value label.
         */
        public String getValue() {
            return value;
        }

        /**
         * @param value The value label.
         */
        public void setValue(String value) {
            this.value = value;
        }
    }

}
