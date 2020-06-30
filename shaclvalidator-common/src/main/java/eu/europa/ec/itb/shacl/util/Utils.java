package eu.europa.ec.itb.shacl.util;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.validation.SHACLReportHandler;
import org.apache.jena.rdf.model.Model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils extends eu.europa.ec.itb.validation.commons.Utils {

    public static TAR getTAR(Model report, Path inputFilePath, Model aggregatedShapes, DomainConfig domainConfig) {
    	//SHACL report: from Model to TAR
		try {
			String contentToValidateString = new String(Files.readAllBytes(inputFilePath));
	    	
	    	SHACLReportHandler reportHandler = new SHACLReportHandler(contentToValidateString, aggregatedShapes, report, domainConfig);
	    	
	    	return reportHandler.createReport();
		} catch (IOException e) {
            throw new IllegalStateException("Error during the transformation of the report to TAR");
		}
    }
}
