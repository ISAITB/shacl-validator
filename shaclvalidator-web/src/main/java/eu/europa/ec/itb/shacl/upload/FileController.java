package eu.europa.ec.itb.shacl.upload;

import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.topbraid.jenax.util.JenaUtil;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static eu.europa.ec.itb.shacl.upload.UploadController.*;

/**
 * REST controller used for the manipulation of user inputs and produced reports.
 */
@RestController
public class FileController {

    @Autowired
    private FileManager fileManager = null;
    @Autowired
    private DomainConfigCache domainConfigCache = null;

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
            HttpServletResponse response) {

        DomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);

        File tmpFolder = new File(fileManager.getWebTmpFolder(), id);
        syntax = syntax.replace("_", "/");

        // Determine base file name.
        String baseFileName;
        switch (type) {
            case UploadController.DOWNLOAD_TYPE__CONTENT:
                baseFileName = FILE_NAME__INPUT;
                break;
            case UploadController.DOWNLOAD_TYPE__REPORT:
                baseFileName = FILE_NAME__REPORT;
                break;
            case UploadController.DOWNLOAD_TYPE__SHAPES:
                baseFileName = FILE_NAME__SHAPES;
                break;
            default: throw new IllegalArgumentException("Invalid file type ["+type+"]");
        }
        // Determine extension.
        String extension;
        if (syntax.equals("pdfType")) {
            if (!UploadController.DOWNLOAD_TYPE__REPORT.equals(type)) {
                throw new IllegalArgumentException("A PDF report can only be requested for the validation report");
            }
            extension = "pdf";
        } else {
            extension = fileManager.getFileExtension(syntax);
        }
        File targetFile = new File(tmpFolder, baseFileName+"."+extension);
        if (!targetFile.exists()) {
            // File doesn't exist. Create it based on an existing file.
            File existingFileOfRequestedType = getFileByType(tmpFolder, type);
            Lang lang = RDFLanguages.filenameToLang(existingFileOfRequestedType.getName());
            String existingSyntax = lang.getContentType().getContentType();
            Model fileModel = JenaUtil.createMemoryModel();
            try (FileInputStream in = new FileInputStream(existingFileOfRequestedType); FileWriter out = new FileWriter(targetFile)) {
                fileModel.read(in, null, existingSyntax);
                fileManager.writeRdfModel(out, fileModel, syntax);
            } catch (IOException e) {
                // Delete the target file (if produced) to make sure we don't cache it for future requests.
                FileUtils.deleteQuietly(targetFile);
                throw new NotFoundException();
            } catch (RuntimeException e) {
                // Delete the target file (if produced) to make sure we don't cache it for future requests.
                FileUtils.deleteQuietly(targetFile);
                throw e;
            }
        }
        response.setHeader("Content-Disposition", "attachment; filename=" + type.replace("Type", "") +"."+extension);
        return new FileSystemResource(targetFile);
    }

    /**
     * Delete all data matching a specific ID. This is a POST request as it meant to be sent as
     * beacon communication (which is a POST).
     *
     * @param domain The domain name.
     * @param id The report files' ID.
     */
    @RequestMapping(value = "/{domain}/delete/{id}", method = RequestMethod.POST)
    @ResponseBody
    public void deleteReport(@PathVariable String domain, @PathVariable String id) {
        var domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
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
                case UploadController.DOWNLOAD_TYPE__CONTENT:
                    if (filename.equals(FILE_NAME__INPUT)) {
                        file = fileTmp;
                    }
                    break;
                case UploadController.DOWNLOAD_TYPE__REPORT:
                    if (filename.equals(FILE_NAME__REPORT)) {
                        file = fileTmp;
                    }
                    break;
                case UploadController.DOWNLOAD_TYPE__SHAPES:
                    if (filename.equals(UploadController.FILE_NAME__SHAPES)) {
                        file = fileTmp;
                    }
                    break;
            }
        }
        if (file == null) {
            throw new NotFoundException();
        }
        return file;
    }

}
