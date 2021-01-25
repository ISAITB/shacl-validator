package eu.europa.ec.itb.shacl.util;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.validation.SHACLReportHandler;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils extends eu.europa.ec.itb.validation.commons.Utils {

	public static TAR getTAR(Model report, DomainConfig domainConfig) {
		SHACLReportHandler reportHandler = new SHACLReportHandler(report, domainConfig);
		return reportHandler.createReport();
	}

    public static TAR getTAR(Model report, String reportContentToInclude, Path inputFilePath, Model aggregatedShapes, DomainConfig domainConfig) {
    	//SHACL report: from Model to TAR
		try {
			String contentToValidateString = null;
			if (inputFilePath != null) {
				contentToValidateString = new String(Files.readAllBytes(inputFilePath));
			}
	    	SHACLReportHandler reportHandler = new SHACLReportHandler(contentToValidateString, aggregatedShapes, report, reportContentToInclude, domainConfig);
	    	return reportHandler.createReport();
		} catch (IOException e) {
            throw new IllegalStateException("Error during the transformation of the report to TAR");
		}
    }

	/**
	 * Get RDF model in the provided content type.
	 * @param rdfModel The model.
	 * @param reportSyntax The mime type.
	 * @return String The result.
	 */
	public static String serializeRdfModel(Model rdfModel, String reportSyntax) {
		StringWriter writer = new StringWriter();
		Lang lang = RDFLanguages.contentTypeToLang(reportSyntax);
		if (lang == null) {
			rdfModel.write(writer, null);
		} else {
			try {
				rdfModel.write(writer, lang.getName());
			}catch(Exception e) {
				rdfModel.write(writer, null);
			}
		}
		return writer.toString();
	}

}
