package eu.europa.ec.itb.shacl.validation;

import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.BAR;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestResultType;
import com.gitb.tr.ValidationCounters;

import eu.europa.ec.itb.shacl.util.Utils;

public class SHACLReportHandler {
	private TAR report;
	private Model shaclReport;

    private ObjectFactory objectFactory = new ObjectFactory();

	public SHACLReportHandler(String inputFile, String contentSyntax, Model shapes, Model shaclReport) {
		this.shaclReport = shaclReport;
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
            		while(it.hasNext()) {
            			Statement statement = it.next();
            			
            			if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#resultMessage")) {
            				error.setDescription(statement.getString());
            			}
            			if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#focusNode")) {
            				focusNode = statement.getResource().toString();
            			}
            			if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#resultPath")) {
            				resultPath = statement.getResource().toString();
            			}
            			if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#sourceShape")) {
            				error.setTest(statement.getResource().toString());
            			}
            			if(statement.getPredicate().hasURI("http://www.w3.org/ns/shacl#resultSeverity")) {
            				severity = statement.getResource().toString();
            			}
            		}
            		error.setLocation("Focus node [" + resultPath + "] - Result path [" + focusNode + "]");
            		
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
        
        if(errors>0) {
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
}
