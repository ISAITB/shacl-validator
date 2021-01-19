package eu.europa.ec.itb.shacl.standalone;


import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.InputHelper;
import eu.europa.ec.itb.shacl.SparqlQueryConfig;
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
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final Logger logger = LoggerFactory.getLogger(ValidationRunner.class);
    private static final Logger loggerFeedback = LoggerFactory.getLogger("FEEDBACK");
    private static final Logger loggerFeedbackFile = LoggerFactory.getLogger("VALIDATION_RESULT");

    private static final String FLAG_NO_REPORTS = "-noreports";
    private static final String FLAG_VALIDATION_TYPE = "-validationType";
    private static final String FLAG_REPORT_SYNTAX = "-reportSyntax";
    private static final String FLAG_CONTENT_TO_VALIDATE = "-contentToValidate";
    private static final String FLAG_EXTERNAL_SHAPES = "-externalShapes";
    private static final String FLAG_LOAD_IMPORTS = "-loadImports";
    private static final String FLAG_CONTENT_QUERY = "-contentQuery";
    private static final String FLAG_CONTENT_QUERY_ENDPOINT = "-contentQueryEndpoint";
    private static final String FLAG_CONTENT_QUERY_USERNAME = "-contentQueryUsername";
    private static final String FLAG_CONTENT_QUERY_PASSWORD = "-contentQueryPassword";

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
        SparqlQueryConfig queryConfig = null;

        String reportSyntax = null;
        String type = null;
        
        if (!requireType) {
            type = domainConfig.getType().get(0);        	
        }
        try {
            if (args.length == 0) {
                printUsage();
            } else {
                try {
                    int i = 0;
                    //Reading the arguments
                    while (i < args.length) {
                        if (FLAG_NO_REPORTS.equalsIgnoreCase(args[i])) {
                            noReports = true;
                        } else if (FLAG_VALIDATION_TYPE.equalsIgnoreCase(args[i])) {
                            if (requireType && args.length > i+1) {
                                type = args[++i];
                            }

                            if (!domainConfig.getType().contains(type)) {
                                throw new IllegalArgumentException("Unknown validation type ["+type+"]");
                            }
                        } else if (FLAG_REPORT_SYNTAX.equalsIgnoreCase(args[i])) {
                            if (args.length > i+1) {
                                reportSyntax = args[++i];
                            }

                            if (!validReportSyntax(reportSyntax)) {
                                throw new IllegalArgumentException("Unknown report syntax ["+reportSyntax+"]");
                            }
                        } else if (FLAG_CONTENT_TO_VALIDATE.equalsIgnoreCase(args[i])) {
                            //File or URI and content lang
                            String contentToValidate;
                            String contentSyntax = null;

                            if (args.length > i+1) {
                                contentToValidate = args[++i];

                                if(args.length > i+1 && !args[i+1].startsWith("-")) {
                                    contentSyntax = args[++i];
                                }
                                File inputFile = getContent(contentToValidate, contentSyntax, parentFolder, "inputFile."+inputs.size());
                                inputs.add(new ValidationInput(inputFile, type, contentToValidate, contentSyntax));
                            }
                        } else if (FLAG_EXTERNAL_SHAPES.equalsIgnoreCase(args[i])) {
                            String file;
                            String contentLang = null;

                            if (args.length > i+1) {
                                file = args[++i];

                                if(args.length > i+1 && !args[i+1].startsWith("-")) {
                                    contentLang = args[++i];
                                }
                                FileInfo fi = getExternalShapes(file, contentLang, parentFolder);
                                externalShapesList.add(fi);
                            }
                        } else if (FLAG_LOAD_IMPORTS.equalsIgnoreCase(args[i])) {
                            if (args.length > i+1) {
                                loadImports = Boolean.valueOf(args[++i]);
                            }
                        } else if(FLAG_CONTENT_QUERY.equalsIgnoreCase(args[i])) {
                            if (requireType && args.length > i+1) {
                                if (queryConfig == null) {
                                    queryConfig = new SparqlQueryConfig();
                                }
                                queryConfig.setQuery(args[++i]);
                            }
                        } else if(FLAG_CONTENT_QUERY_ENDPOINT.equalsIgnoreCase(args[i])) {
                            if (requireType && args.length > i+1) {
                                if (queryConfig == null) {
                                    queryConfig = new SparqlQueryConfig();
                                }
                                queryConfig.setEndpoint(args[++i]);
                            }
                        } else if(FLAG_CONTENT_QUERY_USERNAME.equalsIgnoreCase(args[i])) {
                            if (requireType && args.length > i+1) {
                                if (queryConfig == null) {
                                    queryConfig = new SparqlQueryConfig();
                                }
                                queryConfig.setUsername(args[++i]);
                            }
                        } else if(FLAG_CONTENT_QUERY_PASSWORD.equalsIgnoreCase(args[i])) {
                            if (requireType && args.length > i+1) {
                                if (queryConfig == null) {
                                    queryConfig = new SparqlQueryConfig();
                                }
                                queryConfig.setPassword(args[++i]);
                            }
                        } else {
                            throw new IllegalArgumentException("Unexpected parameter ["+args[i]+"]");
                        }
                        i++;
                    }
                    if (requireType && type == null) {
                        throw new ValidatorException("Unknown validation type. One of [" + String.join("|", domainConfig.getType()) + "] is mandatory.");
                    }
                    boolean hasExternalShapes = domainConfig.getShapeInfo(type).getExternalArtifactSupport() != ExternalArtifactSupport.NONE;
                    if (!hasExternalShapes && externalShapesList.size() > 0) {
                        throw new ValidatorException(String.format("Loading external shape files is not supported for validation type [%s] of domain [%s].", type, domainConfig.getDomainName()));
                    }
                    loadImports = inputHelper.validateLoadInputs(domainConfig, loadImports, type);
                    if (queryConfig != null) {
                        if (!inputs.isEmpty()) {
                            throw new ValidatorException("The content to validate must either be provided via input or SPARQL query but not both.");
                        } else {
                            queryConfig = inputHelper.validateSparqlConfiguration(domainConfig, queryConfig);
                            var inputFile = fileManager.getContentFromSparqlEndpoint(queryConfig, parentFolder, "queryResult").toFile();
                            inputs.add(new ValidationInput(inputFile, type, inputFile.getName(), queryConfig.getPreferredContentType()));
                        }
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

                    // Proceed with validation.
                    StringBuilder summary = new StringBuilder();
                    summary.append("\n");
                    int i=0;
                    for (ValidationInput input: inputs) {
                        loggerFeedback.info("\nValidating ["+input.getFilename()+"]...");

                        File inputFile = input.getInputFile();

                        try {
                            SHACLValidator validator = applicationContext.getBean(SHACLValidator.class, inputFile, type, input.getContentSyntax(), externalShapesList, loadImports, domainConfig);
                            Model report = validator.validateAll();
                            // Output summary results.
                            TAR tarReport = Utils.getTAR(report, domainConfig);
                            FileReport reporter = new FileReport(input.getFilename(), tarReport, requireType, type);
                            summary.append("\n").append(reporter.toString()).append("\n");
                            // Output SHACL validation report (if not skipped).
                            if (!noReports) {
                                Path reportFilePath = getReportFilePath("report."+i, reportSyntax);
                                try (OutputStream fos = Files.newOutputStream(reportFilePath)) {
                                    Lang language = RDFLanguages.contentTypeToLang(reportSyntax);
                                    if (language != null) {
                                        report.write(fos, RDFLanguages.contentTypeToLang(reportSyntax).getName());
                                    } else {
                                        report.write(fos);
                                    }
                                    fos.flush();
                                }
                                summary.append("- Detailed report in: [").append(reportFilePath.toFile().getAbsolutePath()).append("] \n");
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
        } finally {
            FileUtils.deleteQuietly(parentFolder);
        }
    }

    private Path getReportFilePath(String baseName, String reportSyntax) {
        return fileManager.createFile(Paths.get("").toFile(), fileManager.getFileExtension(reportSyntax), baseName);
    }

    /**
     * Print how to call the validation JAR.
     */
    private void printUsage() {
        StringBuilder usageStr = new StringBuilder(String.format("\nExpected usage: java -jar validator.jar %s FILE_1/URI_1 CONTENT_SYNTAX_1 ... [%s FILE_N/URI_N CONTENT_SYNTAX_N] [%s] [%s REPORT_SYNTAX]", FLAG_CONTENT_TO_VALIDATE, FLAG_CONTENT_TO_VALIDATE, FLAG_NO_REPORTS, FLAG_REPORT_SYNTAX));
        StringBuilder detailsStr = new StringBuilder("\n   Where:" +
                "\n      - FILE_X or URI_X is the full file path or URI to the content to validate, optionally followed by CONTENT_SYNTAX_X as the content's mime type."+
                "\n      - REPORT_SYNTAX is the mime type for the validation report(s)."
        );
        if (domainConfig.hasMultipleValidationTypes()) {
            usageStr.append(String.format(" [%s VALIDATION_TYPE]", FLAG_VALIDATION_TYPE));
            detailsStr.append(String.format("\n      - VALIDATION_TYPE is one of [%s].", String.join("|", domainConfig.getType())));
        }
        if (domainConfig.supportsUserProvidedLoadImports()) {
            usageStr.append(String.format(" [%s LOAD_IMPORTS]", FLAG_LOAD_IMPORTS));
            detailsStr.append("\n      - LOAD_IMPORTS is a boolean indicating whether owl:Imports in the input should be loaded (true) or not (false).");
        }
        if (domainConfig.supportsExternalArtifacts()) {
            usageStr.append(String.format(" [%s SHAPE_FILE_1/SHAPE_URI_1 CONTENT_SYNTAX_1] ... [%s SHAPE_FILE_N/SHAPE_URI_N CONTENT_SYNTAX_N]", FLAG_EXTERNAL_SHAPES, FLAG_EXTERNAL_SHAPES));
            detailsStr.append("\n      - SHAPE_FILE_X or SHAPE_URI_X is the full file path or URI to additional shapes to consider, optionally followed by CONTENT_SYNTAX_X as the shapes' mime type.");
        }
        if (domainConfig.isSupportsQueries()) {
            usageStr.append(String.format(" [%s QUERY]", FLAG_CONTENT_QUERY));
            detailsStr.append("\n      - QUERY is a SPARQL query to execute to retrieve the content to validate. This is wrapped with double quotes (\").");
            if (domainConfig.getQueryEndpoint() == null) {
                usageStr.append(String.format(" [%s QUERY_ENDPOINT]", FLAG_CONTENT_QUERY_ENDPOINT));
                detailsStr.append("\n      - QUERY_ENDPOINT is the SPARQL endpoint to execute the query against.");
            }
            if (domainConfig.getQueryUsername() == null) {
                usageStr.append(String.format(" [%s QUERY_USERNAME]", FLAG_CONTENT_QUERY_USERNAME));
                usageStr.append(String.format(" [%s QUERY_PASSWORD]", FLAG_CONTENT_QUERY_PASSWORD));
                detailsStr.append("\n      - QUERY_USERNAME is the username to use for authentication against the SPARQL endpoint.");
                detailsStr.append("\n      - QUERY_PASSWORD is the password to use for authentication against the SPARQL endpoint.");
            }
        }
        String message = usageStr
                .append(detailsStr)
                .append("\n\nThe summary of each validation will be printed and the detailed report produced in the current directory (as \"report.X.SUFFIX\").")
                .toString();
        System.out.println(message);
    }

    /**
     * Validate whether the syntax provided is correct.
     * @return The check result.
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
