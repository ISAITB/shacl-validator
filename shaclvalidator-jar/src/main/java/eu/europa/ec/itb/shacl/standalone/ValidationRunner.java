package eu.europa.ec.itb.shacl.standalone;


import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.InputHelper;
import eu.europa.ec.itb.shacl.util.Utils;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.FileReport;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.validation.commons.FileInfo;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
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

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    @Autowired
    InputHelper inputHelper;

    @PostConstruct
    public void init() {
        // Determine the domain configuration.
        List<DomainConfig> domainConfigurations = domainConfigCache.getAllDomainConfigurations();
        
        if (domainConfigurations.size() == 1) {
            this.domainConfig = domainConfigurations.get(0);
        } else if (domainConfigurations.size() > 1) {
            StringBuilder message = new StringBuilder();
            message.append("A specific validation domain needs to be selected. Do this by supplying the -Dvalidator.domain argument. Possible values for this are [");
        	for (DomainConfig dc: domainConfigurations) {
        		message.append(dc.getDomainName());
                message.append("|");
        	}
        	message.delete(message.length()-1, message.length()).append("].");
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

    protected void bootstrap(String[] args, File parentFolder) {
        // Process input arguments
        List<ValidationInput> inputs = new ArrayList<>();
        List<FileInfo> externalShapesList = new ArrayList<>();
        boolean noReports = false;        
        boolean requireType = domainConfig.hasMultipleValidationTypes();
        Boolean loadImports = null;


        String reportSyntax = null;
        String inputContentType = null;
        String type = null;
        
        if (!requireType) {
            type = domainConfig.getType().get(0);        	
        }
        try {
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
                        String contentToValidate;
                        String contentSyntax = null;

                        if (args.length > i+1) {
                            contentToValidate = args[++i];

                            if(args.length > i+1 && !args[i+1].startsWith("-")) {
                                contentSyntax = args[++i];
                            }
                            inputContentType = contentSyntax;
                            File inputFile = getContent(contentToValidate, inputContentType, parentFolder, "inputFile."+inputs.size());
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
                            FileInfo fi = getExternalShapes(file, contentLang, parentFolder);
                            externalShapesList.add(fi);
                        }
                    } else if ("-loadImports".equalsIgnoreCase(args[i])) {
                    	if (args.length > i+1) {
                    		loadImports = Boolean.valueOf(args[++i]);
                    	}
                    } else {
                        throw new IllegalArgumentException("Unexpected parameter ["+args[i]+"]");
                    }
                    i++;
                }

                if (requireType && type==null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unknown validation type. One of [");
                    sb.append(String.join("|", domainConfig.getType())).append("] is mandatory.");

                    throw new IllegalArgumentException(sb.toString());
                }
                boolean hasExternalShapes = domainConfig.getShapeInfo(type).getExternalArtifactSupport() != ExternalArtifactSupport.NONE;
                if (!hasExternalShapes && externalShapesList.size() > 0) {
                    throw new ValidatorException(String.format("Loading external shape files is not supported for validation type [%s] of domain [%s].", type, domainConfig.getDomainName()));
                }

                loadImports = inputHelper.validateLoadInputs(domainConfig, loadImports, type);

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

                // Proceed with validation.
                StringBuilder summary = new StringBuilder();
                summary.append("\n");
                int i=0;
                for (ValidationInput input: inputs) {
                    loggerFeedback.info("\nValidating ["+input.getFilename()+"]...");

                    File inputFile = input.getInputFile();

                    try {
                        SHACLValidator validator = applicationContext.getBean(SHACLValidator.class, inputFile, type, inputContentType, externalShapesList, loadImports, domainConfig);
                        Model report = validator.validateAll();

                        String outputData = getShaclReport(report, reportSyntax);
                        File f = new File("");

                        TAR TARreport = Utils.getTAR(report, inputFile.toPath(), validator.getAggregatedShapes(), domainConfig);
                        FileReport reporter = new FileReport(input.getFilename(), TARreport, requireType, type);

                        summary.append("\n").append(reporter.toString()).append("\n");
                        if (!noReports) {
                            File out = fileManager.getFileFromString(f, outputData, reportSyntax, "report."+i);

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
        } finally {
            FileUtils.deleteQuietly(parentFolder);
        }
    }

    /**
     * Print how to call the validation JAR.
     */
    private void printUsage() {
        boolean requireType = domainConfig.hasMultipleValidationTypes();
        StringBuilder msg = new StringBuilder();
        if (requireType) {
            msg.append("\nExpected usage: java -jar validator.jar [-noreports] [-validationType VALIDATION_TYPE] [-reportSyntax REPORT_SYNTAX] -contentToValidate FILE_1/URI_1 CONTENT_SYNTAX_1 ... [-contentToValidate FILE_N/URI_N CONTENT_SYNTAX_N] [-externalShapes SHAPE_FILE_1/SHAPE_URI_1 CONTENT_SYNTAX_1] ... [-externalShapes SHAPE_FILE_N/SHAPE_URI_N CONTENT_SYNTAX_N] [-loadImports LOAD_IMPORTS]");
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
        	msg.append("\nExpected usage: java -jar validator.jar [-noreports] [-reportSyntax REPORT_SYNTAX] -contentToValidate FILE_1/URI_1 CONTENT_SYNTAX_1 ... [-contentToValidate FILE_N/URI_N CONTENT_SYNTAX_N] [-externalShapes SHAPE_FILE_1/SHAPE_URI_1 CONTENT_SYNTAX_1] ... [-externalShapes SHAPE_FILE_N/SHAPE_URI_N CONTENT_SYNTAX_N] [-loadImports LOAD_IMPORTS]\"");
        	msg.append("\n   Where:");
        }
        msg.append("\n      - REPORT_SYNTAX is the mime type for the validation report(s).");
        msg.append("\n      - FILE_X or URI_X is the full file path or URI to the content to validate, optionally followed by CONTENT_SYNTAX_X as the content's mime type.");
        msg.append("\n      - SHAPE_FILE_X or SHAPE_URI_X is the full file path or URI to additional shapes to consider, optionally followed by CONTENT_SYNTAX_X as the shapes' mime type.");
        msg.append("\n      - LOAD_IMPORTS is a boolean indicating whether the owl:Imports should be loaded (true) or not (false).");        
        msg.append("\n\nThe summary of each validation will be printed and the detailed report produced in the current directory (as \"report.X.SUFFIX\").");
        System.out.println(msg.toString());
    }

    /**
     * Get SHACL report as the provided syntax
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
     * @return
     */
    private boolean validReportSyntax(String syntax) {
    	if(!StringUtils.isBlank(syntax)) {
    		Lang lang = RDFLanguages.contentTypeToLang(syntax.toLowerCase());
		
			return lang != null;
    	}else {
    		return false;
    	}
	}
    
    /**
     * Get the content and save it in the folder
     * @return File
     */
    private File getContent(String file, String contentType, File parentFolder, String filename) {
        File inputFile = new File(file);
        
    	try {
        	contentType = getContentType(file, contentType);
        	boolean validSyntax = validReportSyntax(contentType);
			Lang langExtension = RDFLanguages.contentTypeToLang(contentType);
			
            if(!inputFile.exists() || !inputFile.isFile() || !inputFile.canRead()) {           	
            	
            	if(validSyntax && langExtension!=null) {
            	    try {
            	        new URL(file);
                        inputFile = this.fileManager.getFileFromURL(parentFolder, file, langExtension.getFileExtensions().get(0), filename);
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("Unable to load content from ["+file+"]");
                    }
            	}else {
                    throw new IllegalArgumentException("Unknown content syntax ["+contentType+"]");
            	}
            }else {
            	inputFile = this.fileManager.getFileFromInputStream(parentFolder, new FileInputStream(inputFile), contentType, FilenameUtils.removeExtension(inputFile.getName()));
            }
            if (!inputFile.exists() || !inputFile.isFile() || !inputFile.canRead()) {
                throw new IllegalArgumentException("Unable to read file or URL ["+file+"]");
            }
        }catch(IOException e) {
            throw new IllegalArgumentException("Unable to read file or URL ["+file+"]");
        }
    	
    	return inputFile;
    }
    
    private String getContentType(String contentToValidate, String contentType) {
    	if (StringUtils.isBlank(contentType)) {
    		ContentType ct = RDFLanguages.guessContentType(contentToValidate);
    		if(ct!=null) {
    			contentType = ct.getContentType();
    		}
    	}
		return contentType;
	}

	/**
     * Validate the content syntax and save the external shapes
     * @return FileInfo
     */
    private FileInfo getExternalShapes(String file, String contentType, File parentFolder) {
		File f = getContent(file, contentType, parentFolder, UUID.randomUUID().toString()+"."+FilenameUtils.getExtension(file));
		contentType = getContentType(file, contentType);
				
		if(validReportSyntax(contentType)) {
			return new FileInfo(f, contentType);
		}else {
            throw new IllegalArgumentException("The RDF language could not be determined for the provided external shape ["+file+"]");			
		}
    }
}
