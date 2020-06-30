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

@RestController
public class FileController {

    @Autowired
    private FileManager fileManager = null;
    @Autowired
    private DomainConfigCache domainConfigCache = null;

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
			case UploadController.downloadType_content:
				baseFileName = fileName_input;
				break;
			case UploadController.downloadType_report:
				baseFileName = fileName_report;
				break;
			case UploadController.downloadType_shapes:
				baseFileName = fileName_shapes;
				break;
			default: throw new IllegalArgumentException("Invalid file type ["+type+"]");
		}
		// Determine extension.
		String extension;
		if (syntax.equals("pdfType")) {
			if (!UploadController.downloadType_report.equals(type)) {
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
				throw new NotFoundException();
			}
		}
		response.setHeader("Content-Disposition", "attachment; filename=" + type.replace("Type", "") +"."+extension);
        return new FileSystemResource(targetFile);
    }

    private File getFileByType(File folder, String type) {
    	File file = null;
		List<File> files = (List<File>) FileUtils.listFiles(folder, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		for(File fileTmp : files) {
			String filename = FilenameUtils.removeExtension(fileTmp.getName());
	    	switch(type) {
	    		case UploadController.downloadType_content:
	    			if (filename.equals(fileName_input)) {
	    				file = fileTmp;
	    			}
	    		break;
	    		case UploadController.downloadType_report:
	    			if (filename.equals(fileName_report)) {
	    				file = fileTmp;
	    			}
	    		break;
	    		case UploadController.downloadType_shapes:
	    			if (filename.equals(UploadController.fileName_shapes)) {
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
