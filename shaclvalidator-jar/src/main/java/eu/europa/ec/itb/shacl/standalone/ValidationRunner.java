package eu.europa.ec.itb.shacl.standalone;


import eu.europa.ec.itb.shacl.*;
import eu.europa.ec.itb.shacl.util.ShaclValidatorUtils;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.ReportSpecs;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.validation.commons.*;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.jar.BaseValidationRunner;
import eu.europa.ec.itb.validation.commons.jar.FileReport;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Component that handles the actual triggering of validation and resulting reporting.
 */
@Component
@Scope("prototype")
public class ValidationRunner extends BaseValidationRunner<DomainConfig> {

    private static final String FLAG_NO_REPORTS = "-noreports";
    private static final String FLAG_CLI_REPORTS = "-clireports";
    private static final String FLAG_VALIDATION_TYPE = "-validationType";
    private static final String FLAG_REPORT_SYNTAX = "-reportSyntax";
    private static final String FLAG_REPORT_QUERY = "-reportQuery";
    private static final String FLAG_CONTENT_TO_VALIDATE = "-contentToValidate";
    private static final String FLAG_EXTERNAL_SHAPES = "-externalShapes";
    private static final String FLAG_LOAD_IMPORTS = "-loadImports";
    private static final String FLAG_CONTENT_QUERY = "-contentQuery";
    private static final String FLAG_CONTENT_QUERY_ENDPOINT = "-contentQueryEndpoint";
    private static final String FLAG_CONTENT_QUERY_USERNAME = "-contentQueryUsername";
    private static final String FLAG_CONTENT_QUERY_PASSWORD = "-contentQueryPassword";
    private static final String FLAG_LOCALE = "-locale";

    @Autowired
    FileManager fileManager;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    InputHelper inputHelper;
    @Autowired
    ApplicationConfig appConfig;

    /**
     * Run the validation.
     *
     * @param args The command-line arguments.
     * @param parentFolder The temporary folder to use for this validator's run.
     */
    @Override
    protected void bootstrapInternal(String[] args, File parentFolder) {
        // Process input arguments
        List<ShaclValidationInput> inputs = new ArrayList<>();
        List<FileInfo> externalShapesList = new ArrayList<>();
        boolean noReports = false;
        boolean cliReports = false;
        boolean requireType = domainConfig.hasMultipleValidationTypes() && domainConfig.getDefaultType() == null;
        Boolean loadImports = null;
        SparqlQueryConfig queryConfig = null;

        String reportSyntax = null;
        String reportQuery = null;
        String type = null;
        String locale = null;
        
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
                        } else if (FLAG_REPORT_SYNTAX.equalsIgnoreCase(args[i])) {
                            if (args.length > i+1) {
                                reportSyntax = args[++i];
                            }
                            if (!validRDFSyntax(reportSyntax)) {
                                throw new IllegalArgumentException("Unknown report syntax ["+reportSyntax+"]");
                            }
                        } else if(FLAG_REPORT_QUERY.equalsIgnoreCase(args[i])) {
                            if (args.length > i+1) {
                                reportQuery = args[++i];
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
                                inputs.add(new ShaclValidationInput(inputFile, contentToValidate, contentSyntax));
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
                            if (args.length > i+1) {
                                if (queryConfig == null) {
                                    queryConfig = new SparqlQueryConfig();
                                }
                                queryConfig.setQuery(args[++i]);
                            }
                        } else if(FLAG_CONTENT_QUERY_ENDPOINT.equalsIgnoreCase(args[i])) {
                            if (args.length > i+1) {
                                if (queryConfig == null) {
                                    queryConfig = new SparqlQueryConfig();
                                }
                                queryConfig.setEndpoint(args[++i]);
                            }
                        } else if(FLAG_CONTENT_QUERY_USERNAME.equalsIgnoreCase(args[i])) {
                            if (args.length > i+1) {
                                if (queryConfig == null) {
                                    queryConfig = new SparqlQueryConfig();
                                }
                                queryConfig.setUsername(args[++i]);
                            }
                        } else if(FLAG_CONTENT_QUERY_PASSWORD.equalsIgnoreCase(args[i])) {
                            if (args.length > i + 1) {
                                if (queryConfig == null) {
                                    queryConfig = new SparqlQueryConfig();
                                }
                                queryConfig.setPassword(args[++i]);
                            }
                        } else if (FLAG_CLI_REPORTS.equalsIgnoreCase(args[i])) {
                            cliReports = true;
                        } else if (FLAG_LOCALE.equalsIgnoreCase(args[i])) {
                            if (args.length > i+1) {
                                locale = args[++i];
                            }
                        } else if (!FLAG_NO_OUTPUT.equalsIgnoreCase(args[i]) && !FLAG_NO_LOG.equalsIgnoreCase(args[i])) {
                            throw new ValidatorException("validator.label.exception.unexpectedParameter", args[i]);
                        }
                        i++;
                    }
                    type = inputHelper.validateValidationType(domainConfig, type);
                    boolean hasExternalShapes = domainConfig.getShapeInfo(type).getExternalArtifactSupport() != ExternalArtifactSupport.NONE;
                    if (!hasExternalShapes && !externalShapesList.isEmpty()) {
                        throw new ValidatorException("validator.label.exception.externalShapeLoadingNotSupported", type, domainConfig.getDomainName());
                    }
                    loadImports = inputHelper.validateLoadInputs(domainConfig, loadImports, type);
                    if (queryConfig != null) {
                        if (!inputs.isEmpty()) {
                            throw new ValidatorException("validator.label.exception.contentExpectedAsInpurOrQuery");
                        } else {
                            queryConfig = inputHelper.validateSparqlConfiguration(domainConfig, queryConfig);
                            var inputFile = fileManager.getContentFromSparqlEndpoint(queryConfig, parentFolder, "queryResult").toFile();
                            inputs.add(new ShaclValidationInput(inputFile, inputFile.getName(), queryConfig.getPreferredContentType()));
                        }
                    }
                } catch (ValidatorException e) {
                    LOGGER_FEEDBACK.info("\nInvalid arguments provided: {}\n", e.getMessageForDisplay(new LocalisationHelper(Locale.ENGLISH)));
                    inputs.clear();
                } catch (Exception e) {
                    LOGGER_FEEDBACK.info("\nInvalid arguments provided: {}\n", e.getMessage());
                    inputs.clear();
                }
                if (inputs.isEmpty()) {
                    printUsage();
                } else {
                    if (reportSyntax == null) {
                        reportSyntax = domainConfig.getDefaultReportSyntax();
                    }
                    // Proceed with validation.
                    StringBuilder summary = new StringBuilder();
                    summary.append("\n");
                    int i=0;
                    var localiser = new LocalisationHelper(domainConfig, Utils.getSupportedLocale(LocaleUtils.toLocale(locale), domainConfig));
                    for (ShaclValidationInput input: inputs) {
                        LOGGER_FEEDBACK.info("\nValidating [{}]...", input.getFileName());
                        File inputFile = input.getInputFile();
                        try {
                            SHACLValidator validator = applicationContext.getBean(SHACLValidator.class, inputFile, type, input.getContentSyntax(), externalShapesList, loadImports, domainConfig, localiser);
                            ModelPair models = validator.validateAll();
                            // Output summary results.
                            ReportPair tarReport = ShaclValidatorUtils.getTAR(ReportSpecs.builder(models.getInputModel(), models.getReportModel(), localiser, domainConfig, validator.getValidationType()).build());
                            FileReport reporter = new FileReport(input.getFileName(), tarReport.getDetailedReport(), requireType, type);
                            summary.append("\n").append(reporter).append("\n");
                            Model report = models.getReportModel();
                            if (!noReports || cliReports) {
                                // Run report post-processing query (if provided).
                                if (reportQuery != null) {
                                    Query query = QueryFactory.create(reportQuery);
                                    try (QueryExecution queryExecution = QueryExecutionFactory.create(query, report)) {
                                        report = queryExecution.execConstruct();
                                    }
                                }
                                if (!noReports) {
                                    // Output SHACL validation report as a file.
                                    Path reportFilePath = getReportFilePath("report."+i, reportSyntax);
                                    try (OutputStream fos = Files.newOutputStream(reportFilePath)) {
                                        fileManager.writeRdfModel(fos, report, reportSyntax);
                                    }
                                    summary.append("- Detailed report in: [").append(reportFilePath.toFile().getAbsolutePath()).append("] \n");
                                }
                                if (cliReports) {
                                    // Output report also/instead to CLI.
                                    try (PrintWriter pw = new PrintWriter(System.out)) {
                                        fileManager.writeRdfModel(pw, report, reportSyntax);
                                    }
                                }
                            }
                        } catch (ValidatorException e) {
                            LOGGER_FEEDBACK.info("\nAn error occurred while executing the validation: {}", e.getMessageForDisplay(localiser));
                            LOGGER.error(String.format("An error occurred while executing the validation: %s", e.getMessageForLog()), e);
                            break;

                        } catch (Exception e) {
                            LOGGER_FEEDBACK.info("\nAn error occurred while executing the validation.");
                            LOGGER.error(String.format("An error occurred while executing the validation: %s", e.getMessage()), e);
                            break;

                        }
                        i++;
                        LOGGER_FEEDBACK.info(" Done.\n");
                    }
                    var summaryString = summary.toString();
                    LOGGER_FEEDBACK.info(summaryString);
                    LOGGER_FEEDBACK_FILE.info(summaryString);
                }
            }
        } finally {
            FileUtils.deleteQuietly(parentFolder);
        }
    }

    /**
     * Get the path for the produced validation report.
     *
     * @param baseName The base name of the report path (without the syntax extension).
     * @param reportSyntax The RDF syntax of the validation report.
     * @return The report path.
     */
    private Path getReportFilePath(String baseName, String reportSyntax) {
        return fileManager.createFile(Paths.get("").toFile(), fileManager.getFileExtension(reportSyntax), baseName);
    }

    /**
     * Print how to call the validation JAR.
     */
    private void printUsage() {
        StringBuilder usageStr = new StringBuilder(String.format("%nExpected usage: java -jar validator.jar %s FILE_1/URI_1 CONTENT_SYNTAX_1 ... [%s FILE_N/URI_N CONTENT_SYNTAX_N] [%s] [%s] [%s] [%s REPORT_SYNTAX] [%s REPORT_QUERY] [%s LOCALE]", FLAG_CONTENT_TO_VALIDATE, FLAG_CONTENT_TO_VALIDATE, FLAG_NO_OUTPUT, FLAG_NO_LOG, FLAG_NO_REPORTS, FLAG_REPORT_SYNTAX, FLAG_REPORT_QUERY, FLAG_LOCALE));
        StringBuilder detailsStr = new StringBuilder("\n").append(PAD).append("Where:");
        detailsStr.append("\n").append(PAD).append(PAD).append(String.format("- FILE_X or URI_X is the full file path or URI to the content to validate, optionally followed by CONTENT_SYNTAX_X as the content's mime type (one of %s).", appConfig.getContentSyntax()));
        detailsStr.append("\n").append(PAD).append(PAD).append(String.format("- REPORT_SYNTAX is the mime type for the validation report(s) (one of %s).", appConfig.getContentSyntax()));
        detailsStr.append("\n").append(PAD).append(PAD).append("- REPORT_QUERY is an optional SPARQL CONSTRUCT query that will be used to post-process the SHACL validation report, replacing it as the output. This is wrapped with double quotes (\").");
        if (domainConfig.hasMultipleValidationTypes()) {
            usageStr.append(String.format(" [%s VALIDATION_TYPE]", FLAG_VALIDATION_TYPE));
            detailsStr.append("\n").append(PAD).append(PAD).append(String.format("- VALIDATION_TYPE is one of [%s].", String.join("|", domainConfig.getType())));
            String defaultType = domainConfig.getDefaultType();
            if (defaultType != null) {
                detailsStr.append("\n").append(PAD).append(PAD).append(String.format("- VALIDATION_TYPE has default value of [%s].", defaultType));
            }
        }
        if (domainConfig.supportsUserProvidedLoadImports()) {
            usageStr.append(String.format(" [%s LOAD_IMPORTS]", FLAG_LOAD_IMPORTS));
            detailsStr.append("\n").append(PAD).append(PAD).append("- LOAD_IMPORTS is a boolean indicating whether owl:Imports in the input should be loaded (true) or not (false).");
        }
        if (domainConfig.supportsExternalArtifacts()) {
            usageStr.append(String.format(" [%s SHAPE_FILE_1/SHAPE_URI_1 CONTENT_SYNTAX_1] ... [%s SHAPE_FILE_N/SHAPE_URI_N CONTENT_SYNTAX_N]", FLAG_EXTERNAL_SHAPES, FLAG_EXTERNAL_SHAPES));
            detailsStr.append("\n").append(PAD).append(PAD).append(String.format("- SHAPE_FILE_X or SHAPE_URI_X is the full file path or URI to additional shapes to consider, optionally followed by CONTENT_SYNTAX_X as the shapes' mime type (one of %s).", appConfig.getContentSyntax()));
        }
        if (domainConfig.isSupportsQueries()) {
            usageStr.append(String.format(" [%s QUERY]", FLAG_CONTENT_QUERY));
            detailsStr.append("\n").append(PAD).append(PAD).append("- QUERY is a SPARQL CONSTRUCT query to execute to retrieve the content to validate. This is wrapped with double quotes (\").");
            if (domainConfig.getQueryEndpoint() == null) {
                usageStr.append(String.format(" [%s QUERY_ENDPOINT]", FLAG_CONTENT_QUERY_ENDPOINT));
                detailsStr.append("\n").append(PAD).append(PAD).append("- QUERY_ENDPOINT is the SPARQL endpoint to execute the query against.");
            }
            if (domainConfig.getQueryUsername() == null) {
                usageStr.append(String.format(" [%s QUERY_USERNAME]", FLAG_CONTENT_QUERY_USERNAME));
                usageStr.append(String.format(" [%s QUERY_PASSWORD]", FLAG_CONTENT_QUERY_PASSWORD));
                detailsStr.append("\n").append(PAD).append(PAD).append("- QUERY_USERNAME is the username to use for authentication against the SPARQL endpoint.");
                detailsStr.append("\n").append(PAD).append(PAD).append("- QUERY_PASSWORD is the password to use for authentication against the SPARQL endpoint.");
            }
        }
        detailsStr.append("\n").append(PAD).append(PAD).append(String.format("- %s disables all command-line output (except validation reports, if %s is set).", FLAG_NO_OUTPUT, FLAG_CLI_REPORTS));
        detailsStr.append("\n").append(PAD).append(PAD).append(String.format("- %s disables all log file output.", FLAG_NO_LOG));
        detailsStr.append("\n").append(PAD).append(PAD).append(String.format("- %s disables validation report generation as files.", FLAG_NO_REPORTS));
        detailsStr.append("\n").append(PAD).append(PAD).append(String.format("- %s enables output of validation reports to the command-line.", FLAG_CLI_REPORTS));
        detailsStr.append("\n").append(PAD).append(PAD).append("- LOCALE is the language code to consider for reporting of results. If the provided locale is not supported by the validator the default locale will be used instead (e.g. 'fr', 'fr_FR').");
        String message = usageStr
                .append(detailsStr)
                .append("\n\nThe summary of each validation will be printed and the detailed report produced in the current directory (as \"report.X.SUFFIX\").")
                .toString();
        System.out.println(message);
    }

    /**
     * Validate whether the syntax provided is correct.
     *
     * @return The check result.
     */
    private boolean validRDFSyntax(String syntax) {
    	if(!StringUtils.isBlank(syntax)) {
    		Lang lang = RDFLanguages.contentTypeToLang(syntax.toLowerCase());
		
			return lang != null;
    	}else {
    		return false;
    	}
	}
    
    /**
     * Get the content and save it in the temp folder assigned for this validation run.
     *
     * @param file The argument corresponding to the input content's path (may be a file path or URL).
     * @param contentType The RDF syntax of the content (as a mime type).
     * @param parentFolder The temp folder to use for this validator's run.
     * @param filename The file name to use to store the input content if this is provided as a URL.
     * @return The file with the content to validate.
     * @throws IllegalArgumentException In case there is a problem with the provided input arguments.
     */
    private File getContent(String file, String contentType, File parentFolder, String filename) {
        File inputFile = new File(file);
    	try {
            contentType = getContentType(file, contentType);
            boolean validSyntax = validRDFSyntax(contentType);
            Lang langExtension = RDFLanguages.contentTypeToLang(contentType);
            if (!inputFile.exists() || !inputFile.isFile() || !inputFile.canRead()) {
                if (validSyntax && langExtension != null) {
                    new URL(file);
                    inputFile = this.fileManager.getFileFromURL(parentFolder, file, langExtension.getFileExtensions().get(0), filename, null, null, null, List.of(contentType), domainConfig.getHttpVersion()).getFile();
                } else {
                    throw new IllegalArgumentException("Unknown content syntax [" + contentType + "]");
                }
            } else {
                inputFile = this.fileManager.getFileFromInputStream(parentFolder, new FileInputStream(inputFile), contentType, FilenameUtils.removeExtension(inputFile.getName()));
            }
            if (!inputFile.exists() || !inputFile.isFile() || !inputFile.canRead()) {
                throw new IllegalArgumentException("Unable to read file or URL [" + file + "]");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read file or URL ["+file+"]");
        }
    	return inputFile;
    }

    /**
     * Get the content type (mime type) for the content to validate.
     *
     * @param contentToValidate The path for the content to validate.
     * @param contentType The provided content type. If not provided the content type is guessed from the provided
     *                    content.
     * @return The content type to consider.
     */
    private String getContentType(String contentToValidate, String contentType) {
    	if (StringUtils.isBlank(contentType)) {
    		ContentType ct = RDFLanguages.guessContentType(contentToValidate);
    		if(ct!=null) {
    			contentType = ct.getContentTypeStr();
    		}
    	}
		return contentType;
	}

	/**
     * Validate a user-provided shape file/resource and store it for use in the validation.
     *
     * @param file The path to the shapes.
     * @param contentType The content type (mime type) of the shape file/resource.
     * @param parentFolder The temp folder to use for this validator run.
     * @return The information on the shape file as stored for the validation.
     */
    private FileInfo getExternalShapes(String file, String contentType, File parentFolder) {
		File f = getContent(file, contentType, parentFolder, UUID.randomUUID() +"."+FilenameUtils.getExtension(file));
		contentType = getContentType(file, contentType);
				
		if(validRDFSyntax(contentType)) {
			return new FileInfo(f, contentType);
		}else {
            throw new IllegalArgumentException("The RDF language could not be determined for the provided external shape ["+file+"]");			
		}
    }
}
