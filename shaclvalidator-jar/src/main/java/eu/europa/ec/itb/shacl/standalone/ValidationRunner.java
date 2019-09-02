package eu.europa.ec.itb.shacl.standalone;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.atlas.web.ContentType;
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
import com.google.common.base.Strings;

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
            StringBuilder message = new StringBuilder();
            message.append("A specific validation domain needs to be selected. Do this by supplying the -Dvalidator.domain argument. Possible values for this are [");
        	
        	for(int i=0; i<domainConfigurations.length; i++) {
        		DomainConfig dc = domainConfigurations[i];
        		message.append(dc.getDomainName());
        		
        		if(i<domainConfigurations.length-1) {
        			message.append("|");
        		}
        	}
        	message.append("].");
        	
            loggerFeedback.info(message.toString());
            logger.error(message.toString());
            
            throw new IllegalArgumentException();
        } else {
        	String message = "No validation domains could be found.";
        	
        	loggerFeedback.info(message);
            logger.error(message);
            throw new IllegalStateException(message);
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
                    
            		if (args.length > i+1) {
            			contentToValidate = args[++i];
            			
            			if(args.length > i+1 && !args[i+1].startsWith("-")) {
            				contentSyntax = args[++i];
            			}
                		File inputFile = getContent(contentToValidate, contentSyntax, tmpFolder, "inputFile."+inputs.size());
                        inputs.add(new ValidationInput(inputFile, type, contentToValidate));
            		}
                } else if ("-externalShapes".equalsIgnoreCase(args[i])) {
                	String file = null;
                	String contentLang = null;

            		if (args.length > i+1) {
            			file = args[++i];
            			
            			if(args.length > i+1 && !args[i+1].startsWith("-")) {
            				contentLang = args[++i];
            			}
            			FileInfo fi = getExternalShapes(file, contentLang, tmpFolder);	            			
            			externalShapesList.add(fi);
            		}
                } else {
                    throw new IllegalArgumentException("Unexpected parameter ["+args[i]+"]");
                }
                i++;
            }
            
        	if (requireType && type==null) {
        		StringBuilder sb = new StringBuilder();
        		sb.append("Unknown validation type. One of [");            		
        		sb.append(domainConfig.getType().stream().collect(Collectors.joining("|"))).append("] is mandatory.");

        		throw new IllegalArgumentException(sb.toString());		
        	}
        	Boolean hasExternalShapes = domainConfig.getExternalShapes().get(type);
        	if ((hasExternalShapes == null || !hasExternalShapes) && externalShapesList.size()>0) {
    			throw new ValidatorException(String.format("Loading external shape files is not supported for validation type [%s] of domain [%s].", type, domainConfig.getDomainName()));
    		}
        	
        } catch (Exception e) {
            loggerFeedback.info("\nInvalid arguments provided: "+e.getMessage()+"\n");
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
            int i=0;
            for (ValidationInput input: inputs) {
                loggerFeedback.info("\nValidating ["+input.getFilename()+"]...");
                
                File inputFile = input.getInputFile();
                
                try {
					SHACLValidator validator = applicationContext.getBean(SHACLValidator.class, inputFile, type, FilenameUtils.getExtension(inputFile.getAbsolutePath()), externalShapesList, domainConfig);
                    Model report = validator.validateAll();
                    
                    String outputData = getShaclReport(report, reportSyntax);
                    File f = new File("");

					TAR TARreport = Utils.getTAR(report, inputFile.toPath(), validator.getAggregatedShapes(), domainConfig.isReportsOrdered());
                    FileReport reporter = new FileReport(input.getFilename(), TARreport, requireType, type);

                    summary.append("\n").append(reporter.toString()).append("\n");
                    if (!noReports) {
                    	File out = fileManager.getStringFile(f.getAbsolutePath(), outputData, reportSyntax, "report."+i);
                    	
                        summary.append("- Detailed report in: [").append(out.getAbsolutePath()).append("] \n");
                    }
                } catch (ValidatorException e) {
                    loggerFeedback.info("\nAn error occurred while executing the validation: "+e.getMessage());
                    logger.error("An error occurred while executing the validation: "+e.getMessage(), e);
                    break;
                    
                } catch (Exception e) {
                    loggerFeedback.info("\nAn error occurred while executing the validation.");
                    logger.error("An error occurred while executing the validation: "+e.getMessage(), e);
                    break;
                    
                }
                i++;
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
            msg.append("\nExpected usage: java -jar validator.jar [-noreports] [-validationType VALIDATION_TYPE] [-reportSyntax REPORT_SYNTAX] -contentToValidate FILE_1/URI_1 CONTENT_SYNTAX_1 ... [-contentToValidate FILE_N/URI_N CONTENT_SYNTAX_N] [-externalShapes SHAPE_FILE_1/SHAPE_URI_1 CONTENT_SYNTAX_1] ... [-externalShapes SHAPE_FILE_N/SHAPE_URI_N CONTENT_SYNTAX_N]");
            msg.append("\n   Where:");
            msg.append("\n      - VALIDATION_TYPE is the type of validation to perform, one of [");
            for (int i=0; i < domainConfig.getType().size(); i++) {
                String type = domainConfig.getType().get(i);
                msg.append(type);
                if (i+1 < domainConfig.getType().size()) {
                    msg.append("|");
                }
            }            
            msg.append("].");
        } else {
        	msg.append("\nExpected usage: java -jar validator.jar [-noreports] [-reportSyntax REPORT_SYNTAX] -contentToValidate FILE_1/URI_1 CONTENT_SYNTAX_1 ... [-contentToValidate FILE_N/URI_N CONTENT_SYNTAX_N] [-externalShapes SHAPE_FILE_1/SHAPE_URI_1 CONTENT_SYNTAX_1] ... [-externalShapes SHAPE_FILE_N/SHAPE_URI_N CONTENT_SYNTAX_N]");
        	msg.append("\n   Where:");
        }
        msg.append("\n      - REPORT_SYNTAX is the mime type for the validation report(s).");
        msg.append("\n      - FILE_X or URI_X is the full file path or URI to the content to validate, optionally followed by CONTENT_SYNTAX_X as the content's mime type.");
        msg.append("\n      - SHAPE_FILE_X or SHAPE_URI_X is the full file path or URI to additional shapes to consider, optionally followed by CONTENT_SYNTAX_X as the shapes' mime type.");
        
        msg.append("\n\nThe summary of each validation will be printed and the detailed report produced in the current directory (as \"report.X.SUFFIX\").");
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
    	if(!Strings.isNullOrEmpty(syntax)) {
    		Lang lang = RDFLanguages.contentTypeToLang(syntax.toLowerCase());
		
			return lang != null;
    	}else {
    		return false;
    	}
	}
    
    /**
     * Get the content and save it in the folder
     * @param String Path of the file or URL
     * @param String Syntax of the content
     * @param String Temporary folder
     * @return File
     */
    private File getContent(String contentToValidate, String contentSyntax, String tmpFolder, String filename) {
        File inputFile = new File(contentToValidate);
        
    	try {
        	contentSyntax = getSyntax(contentToValidate, contentSyntax);
        	boolean validSyntax = validReportSyntax(contentSyntax);
			Lang langExtension = RDFLanguages.contentTypeToLang(contentSyntax); 
			
            if(!inputFile.exists() || !inputFile.isFile() || !inputFile.canRead()) {           	
            	
            	if(validSyntax && langExtension!=null) {        			
            		inputFile = this.fileManager.getURLFile(tmpFolder, contentToValidate, langExtension.getFileExtensions().get(0), filename);
            	}else {
                    throw new IllegalArgumentException("Unknown content syntax ["+contentSyntax+"]");		
            	}
            }else {
            	inputFile = this.fileManager.getInputStreamFile(tmpFolder, new FileInputStream(inputFile), contentSyntax, FilenameUtils.removeExtension(inputFile.getName()));           	
            }
            if (!inputFile.exists() || !inputFile.isFile() || !inputFile.canRead()) {
                throw new IllegalArgumentException("Unable to read file or URL ["+contentToValidate+"]");
            }
        }catch(IOException e) {
            throw new IllegalArgumentException("Unable to read file or URL ["+contentToValidate+"]");
        }
    	
    	return inputFile;
    }
    
    private String getSyntax(String contentToValidate, String contentSyntax) {
    	if(Strings.isNullOrEmpty(contentSyntax)) {
    		ContentType ct = RDFLanguages.guessContentType(contentToValidate);
    		
    		if(ct!=null) {
    			contentSyntax = ct.getContentType();
    		}
    	}
    	
		return contentSyntax;
	}

	/**
     * Validate the content syntax and save the external shapes
     * @param String Path of the file or URL
     * @param String Syntax of the external shapes
     * @param Sring Path of the temporary folder
     * @return FileInfo
     */
    private FileInfo getExternalShapes(String file, String contentLang, String tmpFolder) {
		File f = getContent(file, contentLang, Paths.get(tmpFolder, "externalShapes").toFile().getAbsolutePath(), FilenameUtils.getName(file));
		contentLang = getSyntax(file, contentLang);
				
		if(validReportSyntax(contentLang)) {
			Lang langExtension = RDFLanguages.contentTypeToLang(contentLang); 

			return new FileInfo(f, langExtension.getFileExtensions().get(0));
		}else {
            throw new IllegalArgumentException("The RDF language could not be determined for the provided external shape ["+file+"]");			
		}
    }
}
