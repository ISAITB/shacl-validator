package eu.europa.ec.itb.shacl.upload;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.topbraid.jenax.util.JenaUtil;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.ValidatorChannel;
import eu.europa.ec.itb.shacl.upload.errors.NotFoundException;
import eu.europa.ec.itb.shacl.validation.FileManager;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@RestController
public class FileController {
	private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    ApplicationConfig config;
    @Autowired
    FileManager fileManager;
    @Autowired
    DomainConfigCache domainConfigCache;

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

        String tmpFolder = config.getTmpFolder() + "/web/" + id;
        syntax = syntax.replace("_", "/");

        File fileByType = getFileByType(tmpFolder, type);
        File fileOutput = null;

        try {
        	if(fileByType.exists() && fileByType.isFile()) {
        		if(syntax.equals("pdfType")) {
        			if(type.equals(UploadController.downloadType_report)) {
        				fileOutput = new File(tmpFolder, UploadController.fileName_report + "PDF.pdf");
        			}else {
        	            throw new NotFoundException();
        			}
        		}else {
        			if(type.equals(UploadController.downloadType_content)) {
        				Lang lang = RDFLanguages.filenameToLang(fileByType.getName());

        				fileOutput = getReportAsSyntax(fileByType, syntax, lang.getContentType().getContentType(), tmpFolder);
        			}else {
        				fileOutput = getReportAsSyntax(fileByType, syntax, domainConfig.getDefaultReportSyntax(), tmpFolder);
        			}
        		}

        	}
        }catch(Exception e) {
            throw new NotFoundException();
 	   	}

        if(syntax.equals("pdfType")) {
        	response.setHeader("Content-Disposition", "attachment; filename=" + type.replace("Type", "") +".pdf");
        }else {
        	response.setHeader("Content-Disposition", "attachment; filename=" + type.replace("Type", "") +"." + fileManager.getLanguage(syntax));
        }

        return new FileSystemResource(fileOutput);
    }

    private File getFileByType(String folderName, String type) {
    	File folder = new File(folderName);
    	File file = null;

		List<File> files = (List<File>) FileUtils.listFiles(folder, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);

		for(File fileTmp : files) {
			String filename = FilenameUtils.removeExtension(fileTmp.getName());

	    	switch(type) {
	    		case UploadController.downloadType_content:
	    			if(filename.equals(UploadController.fileName_input)) {
	    				file = fileTmp;
	    			}
	    		break;
	    		case UploadController.downloadType_report:
	    			if(filename.equals(UploadController.fileName_report)) {
	    				file = fileTmp;
	    			}
	    		break;
	    		case UploadController.downloadType_shapes:
	    			if(filename.equals(UploadController.fileName_shapes)) {
	    				file = fileTmp;
	    			}
	    		break;
	    	}

		}

		if(file==null) {
			throw new NotFoundException();
		}

		return file;

    }

    private File getReportAsSyntax(File reportFile, String syntax, String defaultSyntax, String tmpFolder) throws IOException {
    	Model fileModel = JenaUtil.createMemoryModel();
        fileModel.read(new FileInputStream(reportFile), null, defaultSyntax);
        String stringReport = fileManager.getShaclReport(fileModel, syntax);

        return fileManager.getStringFile(tmpFolder, stringReport, syntax, null);
    }


	@Scheduled(fixedDelayString = "${validator.cleanupWebRate}")
	public void removeWebFiles() {
		logger.debug("Remove SHACL file cache");
        long currentMillis = System.currentTimeMillis();

		File reportFolder = new File(config.getTmpFolder()+"/web");

		if (reportFolder.exists()) {
            File[] files = reportFolder.listFiles();
            if (files != null) {
                for (File file: files) {
                    if (!handleReportFile(file, currentMillis)) {
                        handleReportFile(file, currentMillis);
                    }
                }
            }
        }
	}

    private boolean handleReportFile(File file, long currentTime) {
        boolean handled = false;

        if (file.isDirectory()) {
            handled = true;

            if (currentTime - file.lastModified() > config.getCleanupWebRate()) {
                FileUtils.deleteQuietly(file);
            }
        }
        return handled;
    }
}
