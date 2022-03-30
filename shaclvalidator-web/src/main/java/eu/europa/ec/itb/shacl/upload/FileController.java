package eu.europa.ec.itb.shacl.upload;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.validation.commons.CsvReportGenerator;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.report.ReportGeneratorBean;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import eu.europa.ec.itb.validation.commons.web.locale.CustomLocaleResolver;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    @ResponseBody
    public FileSystemResource getReport(
            @PathVariable String domain,
            @RequestParam String id,
            @RequestParam String type,
            @RequestParam String syntax,
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
        if (syntax.equals("pdfType") || syntax.equals("csvType")) {
            if (!UploadController.DOWNLOAD_TYPE_REPORT.equals(type)) {
                throw new IllegalArgumentException("This report type can only be requested for the validation report");
            }
            reportFileInfo = produceReport(syntax, tmpFolder, new LocalisationHelper(domainConfig, localeResolver.resolveLocale(request, response, domainConfig, appConfig)), domainConfig);
        } else {
            String extension = fileManager.getFileExtension(syntax);
            reportFileInfo = new ReportFileInfo(extension, new File(tmpFolder, getBaseReportNameForRdfReport(type)+"."+extension));
        }
        if (!reportFileInfo.getFile().toPath().normalize().startsWith(tmpFolder.toPath())) {
            throw new IllegalStateException("Requested invalid file path");
        }
        if (!reportFileInfo.getFile().exists()) {
            // File doesn't exist. Create it based on an existing file.
            File existingFileOfRequestedType = getFileByType(tmpFolder, type);
            Lang lang = RDFLanguages.filenameToLang(existingFileOfRequestedType.getName());
            if (lang == null || lang.getContentType() == null) {
                LOG.error("Unable to determine RDF language from existing file [{}]", existingFileOfRequestedType.getName());
                throw new NotFoundException();
            }
            String existingSyntax = lang.getContentType().getContentTypeStr();
            Model fileModel = JenaUtil.createMemoryModel();
            try (FileInputStream in = new FileInputStream(existingFileOfRequestedType); FileWriter out = new FileWriter(reportFileInfo.getFile())) {
                fileModel.read(in, null, existingSyntax);
                fileManager.writeRdfModel(out, fileModel, syntax);
            } catch (IOException e) {
                // Delete the target file (if produced) to make sure we don't cache it for future requests.
                FileUtils.deleteQuietly(reportFileInfo.getFile());
                throw new NotFoundException();
            } catch (RuntimeException e) {
                // Delete the target file (if produced) to make sure we don't cache it for future requests.
                FileUtils.deleteQuietly(reportFileInfo.getFile());
                throw e;
            }
        }
        response.setHeader("Content-Disposition", "attachment; filename=" + type.replace("Type", "") +"."+reportFileInfo.getExtension());
        return new FileSystemResource(reportFileInfo.getFile());
    }

    /**
     * Get the base name for the given report type.
     *
     * @param type The report type.
     * @return The base name.
     */
    private String getBaseReportNameForRdfReport(String type) {
        String baseFileName;
        switch (type) {
            case UploadController.DOWNLOAD_TYPE_CONTENT:
                baseFileName = FILE_NAME_INPUT;
                break;
            case UploadController.DOWNLOAD_TYPE_REPORT:
                baseFileName = FILE_NAME_REPORT;
                break;
            case UploadController.DOWNLOAD_TYPE_SHAPES:
                baseFileName = FILE_NAME_SHAPES;
                break;
            default: throw new IllegalArgumentException("Invalid file type ["+type+"]");
        }
        return baseFileName;
    }

    /**
     * Construct the report (PDF or CSV).
     *
     * @param reportSyntax The report syntax.
     * @param tmpFolder The temporary folder to use for generated files.
     * @param localiser The localisation helper.
     * @param domainConfig The domain configuration.
     * @return The information reltated to the generated report.
     */
    private ReportFileInfo produceReport(String reportSyntax, File tmpFolder, LocalisationHelper localiser, DomainConfig domainConfig) {
        String extension;
        String baseFileName;
        if (reportSyntax.equals("pdfType")) {
            extension = "pdf";
            baseFileName = FILE_NAME_PDF_REPORT;
        } else if (reportSyntax.equals("csvType")) {
            extension = "csv";
            baseFileName = FILE_NAME_CSV_REPORT;
        } else {
            throw new IllegalArgumentException("Unknown report syntax ["+reportSyntax+"]");
        }
        File targetFile = new File(tmpFolder, baseFileName + "." + extension);
        if (!targetFile.exists()) {
            // Generate the requested PDF report from the TAR XML report.
            File xmlReport = new File(tmpFolder, FILE_NAME_TAR + ".xml");
            if (xmlReport.exists()) {
                if (reportSyntax.equals("pdfType")) {
                    pdfReportGenerator.writeReport(
                            xmlReport,
                            targetFile,
                            (tar) -> pdfReportGenerator.getReportLabels(localiser, tar.getResult())
                    );
                } else {
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
     * Delete all data matching a specific ID. This is a POST request as it meant to be sent as
     * beacon communication (which is a POST).
     *
     * @param domain The domain name.
     * @param id The report files' ID.
     */
    @PostMapping(value = "/{domain}/delete/{id}")
    @ResponseBody
    public void deleteReport(@PathVariable String domain, @PathVariable String id) {
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
     */
    private static class ReportFileInfo {

        private final String extension;
        private final File file;

        /**
         * Constructor.
         *
         * @param extension The report's file extension.
         * @param file The report.
         */
        private ReportFileInfo(String extension, File file) {
            this.extension = extension;
            this.file = file;
        }

        /**
         * @return The file extension.
         */
        public String getExtension() {
            return extension;
        }

        /**
         * @return The file.
         */
        public File getFile() {
            return file;
        }
    }
}
