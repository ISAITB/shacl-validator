package eu.europa.ec.itb.shacl.standalone;


import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gitb.tr.TAR;

import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.errors.ValidatorException;
import eu.europa.ec.itb.shacl.util.Utils;
import eu.europa.ec.itb.shacl.validation.FileInfo;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.FileReport;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;

/**
 * Created by simatosc on 12/08/2016.
 * Updated by mfontsan on 29/07/2019.
 */
@Component
@Scope("prototype")
public class ValidationRunner {

    private static Logger logger = LoggerFactory.getLogger(ValidationRunner.class);
    private static Logger loggerFeedback = LoggerFactory.getLogger("FEEDBACK");
    private static Logger loggerFeedbackFile = LoggerFactory.getLogger("VALIDATION_RESULT");

    private DomainConfig domainConfig;

    @Autowired
    FileManager fileManager;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    DomainConfigCache domainConfigCache;

    @PostConstruct
    public void init() {
        // Determine the domain configuration.
        DomainConfig[] domainConfigurations = domainConfigCache.getAllDomainConfigurations();
        
        if (domainConfigurations.length == 1) {
            this.domainConfig = domainConfigurations[0];
        } else if (domainConfigurations.length > 1) {
            throw new IllegalArgumentException("A specific validation domain needs to be specified. Do this by supplying property [validator.domain].");
        } else {
            throw new IllegalStateException("No validation domains could be found.");
        }
    }

    protected void bootstrap(String[] args, String tmpFolder) {
        // Process input arguments
        List<ValidationInput> inputs = new ArrayList<>();
        List<FileInfo> externalShapesList = new ArrayList<>();
        boolean noReports = false;        
        boolean requireType = domainConfig.hasMultipleValidationTypes();

        String reportSyntax = null;
        String type = null;
        
        if (!requireType) {
            type = domainConfig.getType().get(0);        	
        }
        
        try {
            int i = 0;
            //Reading the arguments
            while (i < args.length) {
            	if ("-noreports".equalsIgnoreCase(args[i])) {
                    noReports = true;
                } else if ("-validationType".equalsIgnoreCase(args[i])) {
                	if (requireType && args.length > i+1) {
                        type = args[++i];
                	}
                	
                    if (!domainConfig.getType().contains(type)) {
                        throw new IllegalArgumentException("Unknown validation type ["+type+"]");
                    }
                } else if ("-reportSyntax".equalsIgnoreCase(args[i])) {
            		if (args.length > i+1) {
                        reportSyntax = args[++i];
            		}
            		
            		if (!validReportSyntax(reportSyntax)) {
                        throw new IllegalArgumentException("Unknown report syntax ["+reportSyntax+"]");
            		}
                } else if ("-contentToValidate".equalsIgnoreCase(args[i])) {
                	//File or URI and content lang
                    String contentToValidate = null;
                    String contentSyntax = null;
                    
            		if (args.length > i+2) {
            			contentToValidate = args[++i];
            			contentSyntax = args[++i];
                		
                		File inputFile = getContent(contentToValidate, contentSyntax, tmpFolder);
                        inputs.add(new ValidationInput(inputFile, type));
            		}
                } else if ("-externalShapes".equalsIgnoreCase(args[i])) {
                	// File or URI and content lang
                	Boolean hasExternalShapes = domainConfig.getExternalShapes().get(type);
                	
                	if (hasExternalShapes == null || !hasExternalShapes) {
            			throw new ValidatorException(String.format("Loading external shape files is not supported for validation type [%s] of domain [%s].", type, domainConfig.getDomainName()));
            		}else {
	            		if (args.length > i+2) {
	            			String file = args[++i];
	            			String contentLang = args[++i];
	            			
	            			FileInfo fi = getExternalShapes(file, contentLang, tmpFolder);	            			
	            			externalShapesList.add(fi);
	            		}
            		}
                } else {
                    throw new IllegalArgumentException("Unexpected parameter ["+args[i]+"]");
                }
                i++;
            }
        } catch (IllegalArgumentException e) {
            loggerFeedback.info("\nInvalid arguments provided: "+e.getMessage());
            inputs.clear();
        }
        if (inputs.isEmpty()) {
            printUsage();
        } else {
        	if(reportSyntax==null) {
        		reportSyntax = domainConfig.getDefaultReportSyntax();
        	}
            
            // Proceed with invoice.
            StringBuilder summary = new StringBuilder();
            summary.append("\n");
            for (ValidationInput input: inputs) {
                loggerFeedback.info("\nValidating ["+input.getInputFile().getAbsolutePath()+"]...");
                
                File inputFile = input.getInputFile();
                
                try {
					SHACLValidator validator = applicationContext.getBean(SHACLValidator.class, inputFile, type, FilenameUtils.getExtension(inputFile.getAbsolutePath()), externalShapesList, domainConfig);
                    Model report = validator.validateAll();
                    
                    String outputData = getShaclReport(report, reportSyntax);
                    File f = new File("");

					TAR TARreport = Utils.getTAR(report, inputFile.toPath(), validator.getAggregatedShapes(), domainConfig.isReportsOrdered());
                    FileReport reporter = new FileReport(Paths.get(f.getAbsolutePath(), "report").toString(), TARreport, false);

                    summary.append("\n").append(reporter.toString()).append("\n");
                    if (!noReports) {
                    	File out = fileManager.getStringFile(f.getAbsolutePath(), outputData, reportSyntax, "report");
                    	
                        summary.append("- Detailed report in: [").append(out.getAbsolutePath()).append("] \n");
                    }
                    
                } catch (Exception e) {
                    loggerFeedback.info("\nAn error occurred while executing the validation.");
                    logger.error("An error occurred while executing the validation: "+e.getMessage(), e);
                    break;
                    
                }
                loggerFeedback.info(" Done.\n");
            }
            loggerFeedback.info(summary.toString());
            loggerFeedbackFile.info(summary.toString());
        }
        
    }

    /**
     * Print how to call the validation JAR.
     */
    private void printUsage() {
        boolean requireType = domainConfig.hasMultipleValidationTypes();
        StringBuilder msg = new StringBuilder();
        if (requireType) {
        	// java -jar validator.jar -contentToValidate c:/myFile application/rdf+xml
            msg.append("\nExpected usage: java -jar validator.jar [-noreports] [-validationType VALIDATION_TYPE] [-reportSyntax REPORT_SYNTAX] -contentToValidate FILE_1/URI_1 CONTENT_SYNTAX_1 ... [-contentToValidate FILE_N/URI_N CONTENT_SYNTAX_N] [-externalShapes SHAPE_FILE_1/SHAPE_URI_1 CONTENT_SYNTAX_1] ... [-externalShapes SHAPE_FILE_N/SHAPE_URI_N CONTENT_SYNTAX_M]");
            msg.append("\n\tWhere TYPE_X is the type of validation to perform for the file (accepted values: ");
            for (int i=0; i < domainConfig.getType().size(); i++) {
                String type = domainConfig.getType().get(i);
                msg.append(type);
                if (i+1 < domainConfig.getType().size()) {
                    msg.append(", ");
                }
            }
            msg.append(")");
        } else {
            msg.append("\\nExpected usage: java -jar validator.jar [-noreports] [-validationType VALIDATION_TYPE] [-reportSyntax REPORT_SYNTAX] -contentToValidate FILE_1/URI_1 CONTENT_SYNTAX_1 ... [-contentToValidate FILE_N/URI_N CONTENT_SYNTAX_N] [-externalShapes SHAPE_FILE_1/SHAPE_URI_1 CONTENT_SYNTAX_1] ... [-externalShapes SHAPE_FILE_N/SHAPE_URI_N CONTENT_SYNTAX_M]");
        }
        msg.append("\n\tWhere FILE_X is the full path to a file to validate");
        msg.append("\n\nThe  summary of each file will be printed and the detailed report will produced at the location of the input file (with a \"report.ttl\" postfix).");
        System.out.println(msg.toString());
    }

    /**
     * Get SHACL report as the provided syntax
     * @param Model SHACL report as Model
     * @param String Syntax of the report
     * @return String
     */
    private String getShaclReport(Model shaclReport, String reportSyntax) {
		StringWriter writer = new StringWriter();		
		Lang lang = RDFLanguages.contentTypeToLang(reportSyntax);
		
		if(lang == null) {
			shaclReport.write(writer, null);
		}else {
			try {
				shaclReport.write(writer, lang.getName());
			}catch(Exception e) {
				shaclReport.write(writer, null);
			}
		}
		
		return writer.toString();
    }
    
    /**
     * Validate whether the syntax provided is correct.
     * @param String syntax
     * @return
     */
    private boolean validReportSyntax(String syntax) {
		Lang lang = RDFLanguages.contentTypeToLang(syntax.toLowerCase());
		return lang != null;
	}
    
    /**
     * Get the content and save it in the folder
     * @param String Path of the file or URL
     * @param String Syntax of the content
     * @param String Temporary folder
     * @return File
     */
    private File getContent(String contentToValidate, String contentSyntax, String tmpFolder) {
        File inputFile = new File(contentToValidate);
        
    	try {
            if(!inputFile.exists() || !inputFile.isFile() || !inputFile.canRead()) {
            	boolean validSyntax = validReportSyntax(contentSyntax);
    			Lang langExtension = RDFLanguages.contentTypeToLang(contentSyntax);            	
            	
            	if(validSyntax && langExtension!=null) {        			
            		inputFile = this.fileManager.getURLFile(tmpFolder, contentToValidate, langExtension.getFileExtensions().get(0), "inputFile");
            	}else {
                    throw new IllegalArgumentException("Unknown content syntax ["+contentSyntax+"]");		
            	}
            }
            if (!inputFile.exists() || !inputFile.isFile() || !inputFile.canRead()) {
                throw new IllegalArgumentException("Unable to read file or URL ["+contentToValidate+"]");
            }
        }catch(IOException e) {
            throw new IllegalArgumentException("Unable to read file or URL ["+contentToValidate+"]");
        }
    	
    	return inputFile;
    }
    
    /**
     * Validate the content syntax and save the external shapes
     * @param String Path of the file or URL
     * @param String Syntax of the external shapes
     * @param Sring Path of the temporary folder
     * @return FileInfo
     */
    private FileInfo getExternalShapes(String file, String contentLang, String tmpFolder) {
		File f = getContent(file, contentLang, Paths.get(tmpFolder, "externalShapes").toFile().getAbsolutePath());
				
		if(validReportSyntax(contentLang)) {
			return new FileInfo(f, contentLang);
		}else {
            throw new IllegalArgumentException("Unknown external shapes - content lang ["+contentLang+"]");			
		}
    }
}
