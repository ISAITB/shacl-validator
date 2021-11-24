package eu.europa.ec.itb.shacl.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Class to handle a SHACL validation report and produce a TAR report.
 */
public class SHACLReportHandler {

    private static final Logger logger = LoggerFactory.getLogger(SHACLReportHandler.class);

	private final TAR report;
	private final Model shaclReport;
	private final DomainConfig domainConfig;
    private final ObjectFactory objectFactory = new ObjectFactory();
    private final ReportLabels labels;

    /**
     * Constructor.
     *
     * @param shaclReport The RDF report.
     * @param domainConfig The domain configuration.
     * @param labels The labels to use for fixed texts.
     */
    public SHACLReportHandler(Model shaclReport, DomainConfig domainConfig, ReportLabels labels) {
        this(null, null, shaclReport, null, domainConfig, labels);
    }

    /**
     * Constructor.
     *
     * @param inputFile The validated RDF content as a string.
     * @param shapes The shapes used for the validation.
     * @param shaclReport The RDF report.
     * @param reportContentToInclude The content for the validation report to add as context to the TAR report.
     * @param domainConfig The domain configuration.
     * @param labels The labels to use for fixed texts.
     */
	public SHACLReportHandler(String inputFile, Model shapes, Model shaclReport, String reportContentToInclude, DomainConfig domainConfig, ReportLabels labels) {
		this.shaclReport = shaclReport;
		this.domainConfig = domainConfig;
        this.labels = labels;
		report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        report.setDate(Utils.getXMLGregorianCalendarDateTime());
        this.report.setName("SHACL Validation");
        this.report.setReports(new TestAssertionGroupReportsType());

        AnyContent attachment = new AnyContent();
        attachment.setType("map");

        if (inputFile != null) {
            AnyContent inputAttachment = new AnyContent();
            inputAttachment.setName("input");
            inputAttachment.setType("string");
            inputAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            inputAttachment.setValue(inputFile);
            attachment.getItem().add(inputAttachment);
        }

        if (shapes != null) {
            AnyContent shapeAttachment = new AnyContent();
            shapeAttachment.setName("shapes");
            shapeAttachment.setType("string");
            shapeAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            shapeAttachment.setValue(modelToString(shapes));
            attachment.getItem().add(shapeAttachment);
        }

        if (reportContentToInclude != null) {
            AnyContent reportAttachment = new AnyContent();
            reportAttachment.setName("report");
            reportAttachment.setType("string");
            reportAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
            reportAttachment.setValue(reportContentToInclude);
            attachment.getItem().add(reportAttachment);
        }

        this.report.setContext(attachment);
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
	public TAR createReport() {
        int infos = 0;
        int warnings = 0;
        int errors = 0;
        
		if(this.shaclReport != null) {
            NodeIterator niResult = this.shaclReport.listObjectsOfProperty(this.shaclReport.getProperty("http://www.w3.org/ns/shacl#conforms"));
            NodeIterator niValidationResult = this.shaclReport.listObjectsOfProperty(this.shaclReport.getProperty("http://www.w3.org/ns/shacl#result"));
            ArrayList reports = new ArrayList();

            if(niResult.hasNext() && !niResult.next().asLiteral().getBoolean()) {
            	while(niValidationResult.hasNext()) {
            		RDFNode node = niValidationResult.next();
            		StmtIterator it = this.shaclReport.listStatements(node.asResource(), null, (RDFNode)null);

        			BAR error = new BAR();
        			String focusNode = "";
        			String resultPath = "";
        			String severity = "";
                    String value = "";
                    String shape = "";
            		while(it.hasNext()) {
            			Statement statement = it.next();
            			
            			if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#resultMessage")) {
                            error.setDescription(getStatementSafe(statement));
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
            		error.setLocation(createStringMessageFromParts(new String[] {labels.getFocusNode(), labels.getResultPath()}, new String[] {focusNode, resultPath}));
                    error.setTest(createStringMessageFromParts(new String[] {labels.getShape(), labels.getValue()}, new String[] {shape, value}));
                    JAXBElement element;
                    if (severity.equals("http://www.w3.org/ns/shacl#Info")) {
                        element = this.objectFactory.createTestAssertionGroupReportsTypeInfo(error);
                        infos += 1;
                    } else if (severity.equals("http://www.w3.org/ns/shacl#Warning")) {
                        element = this.objectFactory.createTestAssertionGroupReportsTypeWarning(error);
                        warnings += 1;
                    } else { // ERROR, FATAL_ERROR
                        element = this.objectFactory.createTestAssertionGroupReportsTypeError(error);
                        errors += 1;
                    }   
                    reports.add(element);
            	}
                this.report.getReports().getInfoOrWarningOrError().addAll(reports);
            }
		} else {
            BAR error1 = new BAR();
            error1.setDescription("An error occurred when generating SHACL Validation Report due to a problem in given content.");
            JAXBElement element1 = this.objectFactory.createTestAssertionGroupReportsTypeError(error1);
            this.report.getReports().getInfoOrWarningOrError().add(element1);
            
            errors += 1;
        }
		
        report.setCounters(new ValidationCounters());
        report.getCounters().setNrOfErrors(BigInteger.valueOf(errors));
        report.getCounters().setNrOfAssertions(BigInteger.valueOf(infos));
        report.getCounters().setNrOfWarnings(BigInteger.valueOf(warnings));

        if (domainConfig.isReportsOrdered()) {
            this.report.getReports().getInfoOrWarningOrError().sort(new ReportItemComparator());
        }
        if(errors > 0) {
            this.report.setResult(TestResultType.FAILURE);
        }else {
            this.report.setResult(TestResultType.SUCCESS);
        }
        
		return this.report;
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

    /**
     * Comparator to allow sorting of report items.
     */
    private static class ReportItemComparator implements Comparator<JAXBElement<TestAssertionReportType>> {

        /**
         * @see Comparator#compare(Object, Object)
         *
         * @param o1 First item.
         * @param o2 Second item.
         * @return Comparison check.
         */
        @Override
        public int compare(JAXBElement<TestAssertionReportType> o1, JAXBElement<TestAssertionReportType> o2) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            } else {
                String name1 = o1.getName().getLocalPart();
                String name2 = o2.getName().getLocalPart();
                if (name1.equals(name2)) {
                    return 0;
                } else if ("error".equals(name1)) {
                    return -1;
                } else if ("error".equals(name2)) {
                    return 1;
                } else if ("warning".equals(name1)) {
                    return -1;
                } else if ("warning".equals(name2)) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

}
