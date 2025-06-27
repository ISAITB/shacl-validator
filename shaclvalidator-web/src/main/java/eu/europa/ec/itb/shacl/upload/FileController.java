/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.shacl.upload;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.validation.commons.CsvReportGenerator;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.Utils;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.report.ReportGeneratorBean;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import eu.europa.ec.itb.validation.commons.web.locale.CustomLocaleResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.topbraid.jenax.util.JenaUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static eu.europa.ec.itb.shacl.upload.UploadController.*;
import static eu.europa.ec.itb.validation.commons.web.Constants.MDC_DOMAIN;

/**
 * REST controller used for the manipulation of user inputs and produced reports.
 */
@RestController
public class FileController {

    private static final Logger LOG = LoggerFactory.getLogger(FileController.class);
    /** Constant for the detailed PDF report syntax. */
    public static final String SYNTAX_TYPE_PDF_DETAILED = "pdfTypeDetailed";
    /** Constant for the aggregated PDF report syntax. */
    public static final String SYNTAX_TYPE_PDF_AGGREGATED = "pdfTypeAggregated";
    /** Constant for the detailed CSV report syntax. */
    public static final String SYNTAX_TYPE_CSV_DETAILED = "csvTypeDetailed";
    /** Constant for the aggregated PDF report syntax. */
    public static final String SYNTAX_TYPE_CSV_AGGREGATED = "csvTypeAggregated";

    @Autowired
    private FileManager fileManager;
    @Autowired
    private DomainConfigCache domainConfigCache;
    @Autowired
    private ReportGeneratorBean pdfReportGenerator;
    @Autowired
    private CsvReportGenerator csvReportGenerator;
    @Autowired
    private CustomLocaleResolver localeResolver;
    @Autowired
    protected ApplicationConfig appConfig;

    /**
     * Return a resource (input, shapes or report) linked to a given validation run.
     *
     * @param domain The domain in question.
     * @param id The identifier to retrieve the report.
     * @param type The type of download (input, shapes or report).
     * @param syntax The RDF syntax (mime type) for the returned file.
     * @param response The HTTP response.
     * @return The response.
     */
    @GetMapping(value = "/{domain}/report", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public FileSystemResource getReport(
            @PathVariable("domain") String domain,
            @RequestParam("id") String id,
            @RequestParam("type") String type,
            @RequestParam("syntax") String syntax,
            HttpServletRequest request,
            HttpServletResponse response) {

        DomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put(MDC_DOMAIN, domain);
        File tmpFolder = new File(fileManager.getWebTmpFolder(), id);
        if (!tmpFolder.toPath().normalize().startsWith(fileManager.getWebTmpFolder().toPath())) {
            throw new IllegalStateException("Invalid value provided for parameter [id]");
        }
        syntax = syntax.replace("_", "/");
        ReportFileInfo reportFileInfo;
        if (syntax.equals(SYNTAX_TYPE_PDF_DETAILED) || syntax.equals(SYNTAX_TYPE_CSV_DETAILED) || syntax.equals(SYNTAX_TYPE_PDF_AGGREGATED) || syntax.equals(SYNTAX_TYPE_CSV_AGGREGATED)) {
            if (!UploadController.DOWNLOAD_TYPE_REPORT.equals(type)) {
                throw new IllegalArgumentException("This report type can only be requested for the validation report");
            }
            reportFileInfo = produceReport(syntax, tmpFolder, new LocalisationHelper(domainConfig, localeResolver.resolveLocale(request, response, domainConfig, appConfig)), domainConfig);
        } else {
            String extension = fileManager.getFileExtension(syntax);
            reportFileInfo = new ReportFileInfo(extension, new File(tmpFolder, getBaseReportNameForRdfReport(type)+"."+extension));
        }
        if (!reportFileInfo.file().toPath().normalize().startsWith(tmpFolder.toPath())) {
            throw new IllegalStateException("Requested invalid file path");
        }
        if (!reportFileInfo.file().exists()) {
            // File doesn't exist. Create it based on an existing file.
            File existingFileOfRequestedType = getFileByType(tmpFolder, type);
            Lang lang = RDFLanguages.filenameToLang(existingFileOfRequestedType.getName());
            if (lang == null || lang.getContentType() == null) {
                LOG.error("Unable to determine RDF language from existing file [{}]", existingFileOfRequestedType.getName());
                throw new NotFoundException();
            }
            String existingSyntax = lang.getContentType().getContentTypeStr();
            Model fileModel = JenaUtil.createMemoryModel();
            try (FileInputStream in = new FileInputStream(existingFileOfRequestedType); FileWriter out = new FileWriter(reportFileInfo.file())) {
                fileModel.read(in, null, existingSyntax);
                fileManager.writeRdfModel(out, fileModel, syntax);
            } catch (IOException e) {
                // Delete the target file (if produced) to make sure we don't cache it for future requests.
                FileUtils.deleteQuietly(reportFileInfo.file());
                throw new NotFoundException();
            } catch (RuntimeException e) {
                // Delete the target file (if produced) to make sure we don't cache it for future requests.
                FileUtils.deleteQuietly(reportFileInfo.file());
                throw e;
            }
        }
        response.setHeader("Content-Disposition", "attachment; filename=" + type.replace("Type", "") +"."+reportFileInfo.extension());
        return new FileSystemResource(reportFileInfo.file());
    }

    /**
     * Get the base name for the given report type.
     *
     * @param type The report type.
     * @return The base name.
     */
    private String getBaseReportNameForRdfReport(String type) {
        return switch (type) {
            case UploadController.DOWNLOAD_TYPE_CONTENT -> FILE_NAME_INPUT;
            case UploadController.DOWNLOAD_TYPE_REPORT -> FILE_NAME_REPORT;
            case UploadController.DOWNLOAD_TYPE_SHAPES -> FILE_NAME_SHAPES;
            default -> throw new IllegalArgumentException("Invalid file type [" + type + "]");
        };
    }

    /**
     * Construct the report (PDF or CSV).
     *
     * @param reportSyntax The report syntax.
     * @param tmpFolder The temporary folder to use for generated files.
     * @param localiser The localisation helper.
     * @param domainConfig The domain configuration.
     * @return The information related to the generated report.
     */
    private ReportFileInfo produceReport(String reportSyntax, File tmpFolder, LocalisationHelper localiser, DomainConfig domainConfig) {
        String extension;
        String baseFileName = switch (reportSyntax) {
            case SYNTAX_TYPE_PDF_DETAILED -> {
                extension = "pdf";
                yield FILE_NAME_PDF_REPORT_DETAILED;
            }
            case SYNTAX_TYPE_PDF_AGGREGATED -> {
                extension = "pdf";
                yield FILE_NAME_PDF_REPORT_AGGREGATED;
            }
            case SYNTAX_TYPE_CSV_DETAILED -> {
                extension = "csv";
                yield FILE_NAME_CSV_REPORT_DETAILED;
            }
            case SYNTAX_TYPE_CSV_AGGREGATED -> {
                extension = "csv";
                yield FILE_NAME_CSV_REPORT_AGGREGATED;
            }
            default -> throw new IllegalArgumentException("Unknown report syntax [" + reportSyntax + "]");
        };
        File targetFile = new File(tmpFolder, baseFileName + "." + extension);
        if (!targetFile.exists()) {
            // Generate the requested report from the TAR XML report.
            File xmlReport = new File(tmpFolder, ((reportSyntax.equals(SYNTAX_TYPE_PDF_DETAILED) || reportSyntax.equals(SYNTAX_TYPE_CSV_DETAILED))?FILE_NAME_TAR:FILE_NAME_TAR_AGGREGATE) + ".xml");
            if (xmlReport.exists()) {
                if (reportSyntax.equals(SYNTAX_TYPE_PDF_DETAILED) || reportSyntax.equals(SYNTAX_TYPE_PDF_AGGREGATED)) {
                    // PDF generation
                    var tar = Utils.toTAR(xmlReport);
                    if (checkOkToProducePDF(tar, domainConfig)) {
                        pdfReportGenerator.writeReport(tar,targetFile, t -> pdfReportGenerator.getReportLabels(localiser, t), domainConfig.isRichTextReports());
                    } else {
                        LOG.error("Unable to produce PDF report because of too many report items");
                        throw new NotFoundException();
                    }
                } else {
                    // CSV generation
                    csvReportGenerator.writeReport(xmlReport, targetFile, localiser, domainConfig);
                }
            } else {
                LOG.error("Unable to produce report because of missing XML report");
                throw new NotFoundException();
            }
        }
        return new ReportFileInfo(extension, targetFile);
    }

    /**
     * Check to see if we can produce a PDF report.
     *
     * @param tar The TAR report.
     * @param domainConfig The domain configuration.
     * @return The check result.
     */
    private boolean checkOkToProducePDF(TAR tar, DomainConfig domainConfig) {
        return tar.getReports() == null || tar.getReports().getInfoOrWarningOrError().size() <= domainConfig.getMaximumReportsForDetailedOutput();
    }

    /**
     * Delete all data matching a specific ID. This is a POST request as it meant to be sent as
     * beacon communication (which is a POST).
     *
     * @param domain The domain name.
     * @param id The report files' ID.
     */
    @PostMapping(value = "/{domain}/delete/{id}")
    public void deleteReport(@PathVariable("domain") String domain, @PathVariable("id") String id) {
        var domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put(MDC_DOMAIN, domain);
        File tmpFolder = new File(fileManager.getWebTmpFolder(), id);
        if (tmpFolder.exists() && tmpFolder.isDirectory()) {
            FileUtils.deleteQuietly(tmpFolder);
        }
    }

    /**
     * Retrieve the file from the provided folder that corresponds to the requested type.
     *
     * @param folder The folder to look within.
     * @param type The type of file to look for (input, shapes or report).
     * @return The file.
     */
    private File getFileByType(File folder, String type) {
        File file = null;
        List<File> files = (List<File>) FileUtils.listFiles(folder, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for(File fileTmp : files) {
            String filename = FilenameUtils.removeExtension(fileTmp.getName());
            switch(type) {
                case UploadController.DOWNLOAD_TYPE_CONTENT:
                    if (filename.equals(FILE_NAME_INPUT)) {
                        file = fileTmp;
                    }
                    break;
                case UploadController.DOWNLOAD_TYPE_REPORT:
                    if (filename.equals(FILE_NAME_REPORT)) {
                        file = fileTmp;
                    }
                    break;
                case UploadController.DOWNLOAD_TYPE_SHAPES:
                    if (filename.equals(UploadController.FILE_NAME_SHAPES)) {
                        file = fileTmp;
                    }
                    break;
                default: throw new IllegalArgumentException("Unknown download type ["+type+"]");
            }
            if (file != null) {
                break;
            }
        }
        if (file == null) {
            throw new NotFoundException();
        }
        return file;
    }

    /**
     * Wrapper class to communicate information on a generated report.

     * @param extension The report's file extension.
     * @param file      The report.
     */
    private record ReportFileInfo(String extension, File file) {}
}
