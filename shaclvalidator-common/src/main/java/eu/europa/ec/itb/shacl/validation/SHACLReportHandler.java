package eu.europa.ec.itb.shacl.validation;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import eu.europa.ec.itb.shacl.util.Utils;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SHACLReportHandler {

    private static final Logger logger = LoggerFactory.getLogger(SHACLReportHandler.class);

	private TAR report;
	private Model shaclReport;
	private boolean reportsOrdered;

    private ObjectFactory objectFactory = new ObjectFactory();

	public SHACLReportHandler(String inputFile, Model shapes, Model shaclReport, boolean reportsOrdered) {
		this.shaclReport = shaclReport;
		this.reportsOrdered = reportsOrdered;
		report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        try {
            report.setDate(Utils.getXMLGregorianCalendarDateTime());
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Exception while creating XMLGregorianCalendar", e);
        }
        this.report.setName("SHACL Validation");
        this.report.setReports(new TestAssertionGroupReportsType());

        AnyContent attachment = new AnyContent();
        attachment.setType("map");
        
        AnyContent inputAttachment = new AnyContent();
        inputAttachment.setName("input");
        inputAttachment.setType("string");
        inputAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        inputAttachment.setValue(inputFile);
        attachment.getItem().add(inputAttachment);
        
        AnyContent shapeAttachment = new AnyContent();
        shapeAttachment.setName("shapes");
        shapeAttachment.setType("string");
        shapeAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        shapeAttachment.setValue(modelToString(shapes));
        attachment.getItem().add(shapeAttachment);

        this.report.setContext(attachment);
	}

	private String getStatementStringSafe(Statement statement) {
        try {
            return statement.getString();
        } catch (Exception e) {
            logger.warn("Error while getting statement string", e);
            return "-";
        }
    }

    private String getStatementResourceStringSafe(Statement statement) {
        try {
            return statement.getResource().toString();
        } catch (Exception e) {
            logger.warn("Error while getting statement resource string", e);
            return "-";
        }
    }

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
                            error.setDescription(getStatementStringSafe(statement));
            			}
            			if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#focusNode")) {
            				focusNode = getStatementResourceStringSafe(statement);
            			}
            			if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#resultPath")) {
            				resultPath = getStatementResourceStringSafe(statement);
            			}
            			if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#sourceShape")) {
            				shape = getStatementResourceStringSafe(statement);
            			}
            			if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#resultSeverity")) {
            				severity = getStatementResourceStringSafe(statement);
            			}
                        if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#value")) {
                            value = getStatementStringSafe(statement);
                        }
            		}
            		error.setLocation("Focus node ["+focusNode+"] - Result path [" + resultPath + "]");
            		error.setTest("Shape ["+shape+"] - Value ["+value+"]");
            		
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

        if (reportsOrdered) {
            Collections.sort(this.report.getReports().getInfoOrWarningOrError(), new ReportItemComparator());
        }
        if(errors > 0) {
            this.report.setResult(TestResultType.FAILURE);
        }else {
            this.report.setResult(TestResultType.SUCCESS);
        }
        
		return this.report;
	}
	
    private String modelToString(Model shaclReport) {
		StringWriter writer = new StringWriter();		
		
		shaclReport.write(writer);
		
		return writer.toString();
    }

    private static class ReportItemComparator implements Comparator<JAXBElement<TestAssertionReportType>> {

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
