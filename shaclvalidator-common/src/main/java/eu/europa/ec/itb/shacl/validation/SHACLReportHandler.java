package eu.europa.ec.itb.shacl.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import eu.europa.ec.itb.validation.commons.AggregateReportItems;
import eu.europa.ec.itb.validation.commons.ReportItemComparator;
import eu.europa.ec.itb.validation.commons.ReportPair;
import eu.europa.ec.itb.validation.commons.Utils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

/**
 * Class to handle a SHACL validation report and produce a TAR report.
 */
public class SHACLReportHandler {

    private static final Logger logger = LoggerFactory.getLogger(SHACLReportHandler.class);

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
     * Get the language code for the provided statement.
     *
     * @param statement The statement.
     * @return The language code (empty string if none was defined).
     */
    private String getLanguageCode(Statement statement) {
        RDFNode node = statement.getObject();
        if (node.isLiteral()) {
            if (node.asLiteral().getLanguage() == null) {
                return "";
            } else {
                return StringUtils.replaceChars(node.asLiteral().getLanguage(), '-', '_');
            }
        }
        return "";
    }

    /**
     * Convert the provided RDF statement to a string.
     *
     * @param statement The statement.
     * @return The resulting string.
     */
	private String getStatementSafe(Statement statement) {
	    String result;
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
        return result;
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
        report.setName("SHACL Validation");
        report.setReports(new TestAssertionGroupReportsType());
        report.setContext(reportContext);
        int infos = 0;
        int warnings = 0;
        int errors = 0;
        var messageMap = new LinkedHashMap<String, String>();
        var detectedInvalidLanguageCodes = new HashSet<String>();
        var additionalInfoTemplate = new AdditionalInfoTemplate(reportSpecs.getLocalisationHelper(), reportSpecs.getInputModel());
        AggregateReportItems aggregateReportItems = null;
        if (reportSpecs.isProduceAggregateReport()) {
            aggregateReportItems = new AggregateReportItems(objectFactory, reportSpecs.getLocalisationHelper());
        }
		if (reportSpecs.getReportModel() != null) {
            NodeIterator niResult = reportSpecs.getReportModel().listObjectsOfProperty(reportSpecs.getReportModel().getProperty("http://www.w3.org/ns/shacl#conforms"));
            NodeIterator niValidationResult = reportSpecs.getReportModel().listObjectsOfProperty(reportSpecs.getReportModel().getProperty("http://www.w3.org/ns/shacl#result"));
            var reports = new ArrayList<JAXBElement<TestAssertionReportType>>();

            if (niResult.hasNext() && !niResult.next().asLiteral().getBoolean()) {
            	while(niValidationResult.hasNext()) {
            		RDFNode node = niValidationResult.next();
            		StmtIterator it = reportSpecs.getReportModel().listStatements(node.asResource(), null, (RDFNode)null);

        			BAR error = new BAR();
        			String focusNode = "";
        			String resultPath = "";
        			String severity = "";
                    String value = "";
                    String shape = "";
            		while(it.hasNext()) {
            			Statement statement = it.next();
            			
            			if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#resultMessage")) {
                            var message = getStatementSafe(statement);
                            var languageCode = getLanguageCode(statement);
                            messageMap.put(languageCode, message);
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
                    error.setDescription(getErrorDescription(messageMap, detectedInvalidLanguageCodes));
            		error.setLocation(createStringMessageFromParts(new String[] {reportSpecs.getReportLabels().getFocusNode(), reportSpecs.getReportLabels().getResultPath()}, new String[] {focusNode, resultPath}));
                    error.setTest(createStringMessageFromParts(new String[] {reportSpecs.getReportLabels().getShape(), reportSpecs.getReportLabels().getValue()}, new String[] {shape, value}));
                    JAXBElement<TestAssertionReportType> element;
                    String shapeFinal = shape;
                    Function<JAXBElement<TestAssertionReportType>, String> classifierFn = e -> String.format("%s|%s|%s", shapeFinal, e.getName().getLocalPart(), ((BAR)e.getValue()).getDescription());
                    if (severity.equals("http://www.w3.org/ns/shacl#Info")) {
                        element = this.objectFactory.createTestAssertionGroupReportsTypeInfo(error);
                        infos += 1;
                        if (aggregateReportItems != null) aggregateReportItems.updateForReportItem(element, classifierFn);
                    } else if (severity.equals("http://www.w3.org/ns/shacl#Warning")) {
                        element = this.objectFactory.createTestAssertionGroupReportsTypeWarning(error);
                        warnings += 1;
                        if (aggregateReportItems != null) aggregateReportItems.updateForReportItem(element, classifierFn);
                    } else { // ERROR, FATAL_ERROR
                        element = this.objectFactory.createTestAssertionGroupReportsTypeError(error);
                        errors += 1;
                        if (aggregateReportItems != null) aggregateReportItems.updateForReportItem(element, classifierFn);
                    }
                    reports.add(element);
                    messageMap.clear();
            	}
                if (!detectedInvalidLanguageCodes.isEmpty()) {
                    logger.warn("Detected invalid languages codes for shape messages: {}", detectedInvalidLanguageCodes);
                }
                report.getReports().getInfoOrWarningOrError().addAll(reports);
            }
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
        // Create the aggregate report if needed.
        TAR aggregateReport = null;
        if (aggregateReportItems != null) {
            aggregateReport = new TAR();
            aggregateReport.setContext(new AnyContent());
            aggregateReport.setResult(report.getResult());
            aggregateReport.setCounters(report.getCounters());
            aggregateReport.setDate(report.getDate());
            aggregateReport.setName(report.getName());
            aggregateReport.setReports(new TestAssertionGroupReportsType());
            aggregateReport.getReports().getInfoOrWarningOrError().addAll(aggregateReportItems.getReportItems());
            if (reportSpecs.isReportItemsOrdered()) {
                aggregateReport.getReports().getInfoOrWarningOrError().sort(new ReportItemComparator());
            }
        }
		return new ReportPair(report, aggregateReport);
	}

    /**
     * Get the message to use from the loaded messages considering the current locale.
     *
     * @param messageMap The loaded messages.
     * @param detectedInvalidLanguageCodes The set to which to add any detected invalid language codes.
     * @return The message to use.
     */
    private String getErrorDescription(LinkedHashMap<String, String> messageMap, Set<String> detectedInvalidLanguageCodes) {
        if (messageMap.isEmpty()) {
            return "";
        } else if (messageMap.size() == 1) {
            return messageMap.values().iterator().next();
        } else {
            var localeMap = new HashMap<Locale, String>();
            String defaultMessage = null;
            for (var messageEntry: messageMap.entrySet()) {
                if (messageEntry.getKey().isEmpty()) {
                    defaultMessage = messageEntry.getValue();
                } else {
                    try {
                        var messageLocale = LocaleUtils.toLocale(messageEntry.getKey());
                        localeMap.put(messageLocale, messageEntry.getValue());
                    } catch (IllegalArgumentException e) {
                        detectedInvalidLanguageCodes.add(messageEntry.getKey());
                    }
                }
            }
            String messageToReturn = null;
            if (localeMap.containsKey(reportSpecs.getLocalisationHelper().getLocale())) {
                // Exact match.
                messageToReturn = localeMap.get(reportSpecs.getLocalisationHelper().getLocale());
            } else {
                var matchedLanguage = localeMap.entrySet().stream().filter(entry -> entry.getKey().getLanguage().equals(reportSpecs.getLocalisationHelper().getLocale().getLanguage())).findFirst();
                if (matchedLanguage.isPresent()) {
                    // Message for same language.
                    messageToReturn = matchedLanguage.get().getValue();
                } else if (defaultMessage != null) {
                    // Message defined without a language code.
                    messageToReturn = defaultMessage;
                } else if (!localeMap.isEmpty()) {
                    // The first defined message with a valid language code.
                    messageToReturn = localeMap.values().iterator().next();
                } else if (!messageMap.isEmpty()) {
                    // The first defined message even with an invalid language code.
                    messageToReturn = messageMap.values().iterator().next();
                }
            }
            return StringUtils.defaultString(messageToReturn);
        }
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
	            if (str.length() > 0) {
	                str.append(" - ");
                }
	            str.append(String.format("[%s] - [%s]", labels[i], values[i]));
            }
        }
        if (str.length() > 0) {
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
