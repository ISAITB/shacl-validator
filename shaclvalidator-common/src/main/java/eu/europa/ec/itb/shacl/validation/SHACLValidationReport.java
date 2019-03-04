package eu.europa.ec.itb.shacl.validation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.NamespaceContext;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFReader;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.apache.jena.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.BAR;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;
import com.gitb.tr.TestAssertionGroupReportsType;
import com.gitb.tr.TestAssertionReportType;
import com.gitb.tr.TestResultType;

import eu.europa.ec.itb.shacl.util.Utils;

public class SHACLValidationReport {
    private static Pattern ARRAY_PATTERN = Pattern.compile("\\[\\d+\\]");
    private static Pattern DEFAULTNS_PATTERN = Pattern.compile("\\/[\\w]+:?");
    private static final Logger logger = LoggerFactory.getLogger(SHACLValidationReport.class);
    private Document node;
    private List<ValidationResult> validationResult;
    private NamespaceContext namespaceContext;
    private Boolean hasDefaultNamespace;
    private boolean convertXPathExpressions;
    private boolean includeTest;
    private boolean reportsOrdered;
    private TAR report;
    private ObjectFactory objectFactory = new ObjectFactory();
    
    public SHACLValidationReport(List<ValidationResult> shacl, boolean convertXPathExpressions, boolean includeTest, boolean reportsOrdered) {
        this.validationResult = shacl;
        report = new TAR();
        report.setResult(TestResultType.SUCCESS);
        try {
            report.setDate(Utils.getXMLGregorianCalendarDateTime());
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Exception while creating XMLGregorianCalendar", e);
        }
        this.report.setName("Schematron Validation");
        this.report.setReports(new TestAssertionGroupReportsType());
        AnyContent attachment = new AnyContent();
        attachment.setType("map");
        AnyContent xmlAttachment = new AnyContent();
        xmlAttachment.setName("XML");
        xmlAttachment.setType("object");
        xmlAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        //xmlAttachment.setValue(new String(Utils.serialize(xml)));
        attachment.getItem().add(xmlAttachment);
        AnyContent schemaAttachment = new AnyContent();
        schemaAttachment.setName("SHACL");
        schemaAttachment.setType("schema");
        schemaAttachment.setEmbeddingMethod(ValueEmbeddingEnumeration.STRING);
        //schemaAttachment.setValue(new String(Utils.serialize(sch)));
        attachment.getItem().add(schemaAttachment);
        this.report.setContext(attachment);
        this.convertXPathExpressions = convertXPathExpressions;
        this.includeTest = includeTest;
        this.reportsOrdered = reportsOrdered;
    }
    
	/**
	 * Method filters the given result using sparql and provides byte array stream containing the result of
	 * the sparql query using the provided format. The different formats are created using standard jena
	 * ResultSetFormatter
	 * 
	 * @param result
	 *            Result from shacl validation
	 * @param queryStr
	 *            String containing sparql query
	 * @param format
	 *            String switching output format, currently only provided TXT, CSV,
	 *            XML
	 * @return ByteArrayOutputStream
	 * @throws IOException
	 *             Writing to stream might cause problems
	 */
	private static ByteArrayOutputStream formatOutput(Model result, String queryStr, String format) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		Query query = QueryFactory.create(queryStr);

		QueryExecution qe = QueryExecutionFactory.create(query, result);

		ResultSet queryResult = qe.execSelect();

		queryResult.getResourceModel().setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
		if (format.matches("(CSV)")) {
			ResultSetFormatter.output(outputStream, queryResult, ResultsFormat.FMT_RS_CSV);
		} else if (format.matches("(XML)")) {
			ResultSetFormatter.outputAsXML(outputStream, queryResult);
		} else if (format.matches("(TTL)")) {
			ResultSetFormatter.output(outputStream, queryResult, ResultsFormat.FMT_RDF_TTL);
		} else {
			// output txt
			String resultOutput = ResultSetFormatter.asText(queryResult);
			if (resultOutput != null) {
				outputStream.write(resultOutput.getBytes());
			}
		}
		// close this resource
		qe.close();

		return outputStream;

	}
	
	/**
	 * Method to create a list of validation results from the validationModel
	 * 
	 * @param model
	 *            The ValidationModel which should be converted
	 * @throws IOException
	 *             Writing to stream might cause problems
	 */
	public static List<ValidationResult> formatOutput(Model model) throws IOException {
		// formating result using query to CSV		
		InputStream is = SHACLValidationReport.class.getResourceAsStream("/defaultquery.rq");
		String queryStr = FileUtils.readWholeFileAsUTF8(is);
		ByteArrayOutputStream resultStream = formatOutput(model, queryStr, "CSV");
		String output = resultStream.toString();
		resultStream.close();
		
		// Split the CSV string on newline character, add to ArrayList and remove empty lines
    	List<String> linesList = Arrays.asList(output.split("[\\n\\r]"));
    	ArrayList<String> lines = new ArrayList<String>();
    	lines.addAll(linesList);
    	lines.removeAll(Arrays.asList(""));
    	
    	// Replace empty values with NA. 
    	// If last value is empty, this cannot be changed. It is handled later
    	List<List<String>> items = new ArrayList<>();
    	for (int i = 0; i < lines.size(); i++) {
    		while (lines.get(i).contains(",,")) {
    			lines.set(i, lines.get(i).replaceAll(",,", ",NA,"));
    		}
    	}
    	
    	// Split each line on "," and if the last value was empty, complete with NA
    	List<String> firstLineSplit = new ArrayList<>(Arrays.asList((lines.get(0).split("\\s*,\\s*"))));
    	int size = firstLineSplit.size();
    	for (int i = 0; i < lines.size(); i++) {
    		List<String> splittedLine = new ArrayList<>(Arrays.asList((lines.get(i).split("\\s*,\\s*"))));
    		if (splittedLine.size() != size ) {
    			splittedLine.add("NA");
    		}
    		items.add(splittedLine);
    	}
    	
    	// Load into a List of ValidationResults
    	List<ValidationResult> validationList = new ArrayList<ValidationResult>();
    	//Start at 1 because the first row contains the headers and we do not want to include those.
    	for (int j = 1; j < items.size(); j++) {
    		ValidationResult validationResult = new ValidationResult();
    		validationResult.setFocusNode(items.get(j).get(0));
    		validationResult.setResultMessage(items.get(j).get(1));
    		validationResult.setResultPath(items.get(j).get(2));
    		validationResult.setResultSeverity(items.get(j).get(3));
    		validationResult.setSourceConstraint(items.get(j).get(4));
    		validationResult.setSourceConstraintComponent(items.get(j).get(5));
    		validationResult.setSourceShape(items.get(j).get(6));
    		validationResult.setValue(items.get(j).get(7));
    		
    		validationList.add(validationResult);
    		
    	}
		
		return validationList;
	}
	
	
    public TAR createReport() {
        if (validationResult != null) {
            report.setResult(TestResultType.SUCCESS);
            if (validationResult.size() > 0) {
                BAR error = new BAR();
                error.setDescription(validationResult.get(0).getResultMessage());
                error.setLocation(validationResult.get(0).getResultPath());
                report.setResult(TestResultType.FAILURE);
                report.getReports().getInfoOrWarningOrError().add(objectFactory.createTestAssertionGroupReportsTypeError(error));
            }
        } else {
            report.setResult(TestResultType.FAILURE);
            BAR error = new BAR();
            error.setDescription("An error occurred when generating SHACL Validation Report due to a problem in given content.");
            error.setLocation(ValidationConstants.INPUT_XML+":1:0");
            JAXBElement<TestAssertionReportType> element = objectFactory.createTestAssertionGroupReportsTypeError(error);
            report.getReports().getInfoOrWarningOrError().add(element);
        }
        
        return report;
    }
}
