package eu.europa.ec.itb.shacl.upload;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
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

@RestController
public class FileController {

    @Autowired
    ApplicationConfig config;
    @Autowired
    FileManager fileManager;
    @Autowired
    DomainConfigCache domainConfigCache;
    @Autowired
    ReportGeneratorBean reportGenerator;

 /*   @RequestMapping(value = "/{domain}/shacl/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public FileSystemResource getXML(@PathVariable String domain, @PathVariable String id) {
        DomainConfig domainConfig = domainConfigCache.getConfigForDomain(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        File reportFile = new File(config.getReportFolder(), fileManager.getInputFileName(id));
        if (reportFile.exists() && reportFile.isFile()) {
            return new FileSystemResource(reportFile);
        } else {
            throw new NotFoundException();
        }
    }*/

    @GetMapping(value = "/{domain}/report", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public FileSystemResource getReport(@PathVariable String domain, @RequestParam String id, @RequestParam String contentid, @RequestParam String type, @RequestParam String syntax, HttpServletResponse response) {
        DomainConfig domainConfig = domainConfigCache.getConfigForDomainName(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        
        syntax = syntax.replace("_", "/");
        
        File reportFile = new File(config.getTmpFolder(), fileManager.getReportFileNameRdf(id, domainConfig.getDefaultReportSyntax()));

        try {
	        if (reportFile.exists() && reportFile.isFile()) {
	            if(type.equals("reportType")) {
	            	if(!domainConfig.getDefaultReportSyntax().equals(syntax) && !syntax.equals("pdfType")) {	
		                reportFile = getReportAsSyntax(reportFile, syntax, domainConfig.getDefaultReportSyntax());
		            }

	            	if(syntax.equals("pdfType")) {
	            		reportFile = getReportPdf(reportFile, id, domainConfig);
	            	}
	            }
	            if(type.equals("contentType") && !domainConfig.getDefaultReportSyntax().equals(syntax)) {
	            	reportFile = new File(config.getTmpFolder(), contentid);
		            reportFile = getReportAsSyntax(reportFile, syntax, FilenameUtils.getExtension(contentid));
	            }
	            
	            
	            if (response != null) {
	                response.setHeader("Content-Disposition", "attachment; filename=report_" + id + "." + fileManager.getLanguage(domainConfig.getDefaultReportSyntax()));
	            }
	            return new FileSystemResource(reportFile);
	        } else {
	            throw new NotFoundException();
	        }
       }catch(Exception e) {
           throw new NotFoundException();
	   }
    }
    
    private File getReportAsSyntax(File reportFile, String syntax, String defaultSyntax) throws IOException {
    	Model fileModel = JenaUtil.createMemoryModel();    	
        fileModel.read(new FileInputStream(reportFile), null, defaultSyntax);        
        String stringReport = fileManager.getShaclReport(fileModel, syntax);
        
        return fileManager.getStringFile(stringReport, syntax);
    }

    public File getReportPdf(File reportFile, String id, DomainConfig domainConfig) throws IOException {
        File reportPDFFile = new File(config.getTmpFolder(), fileManager.getReportFileNamePdf(id));
        
        File RDFXMLFile = getReportAsSyntax(reportFile, RDFLanguages.RDFXML.getContentType().getContentType(), domainConfig.getDefaultReportSyntax());

        if (!(reportFile.exists() && reportFile.isFile())) {
            // Generate the PDF.
            reportGenerator.writeReport(domainConfig, RDFXMLFile, reportPDFFile);
        }
        
        return reportPDFFile;
    }

   /* public FileSystemResource getReportXml(String domain, String id) {
        return this.getReportXml(domain, id, null);
    }

    public FileSystemResource getReportPdf(String domain, String id) {
        return this.getReportPdf(domain, id, null);
    }*/
/*
    @RequestMapping(value = "/{domain}/report/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteReport(@PathVariable String domain, @PathVariable String id) {
        DomainConfig domainConfig = domainConfigCache.getConfigForDomain(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        File reportFile = new File(config.getReportFolder(), fileManager.getReportFileNameXml(id));
        if (reportFile.exists() && reportFile.isFile()) {
            FileUtils.deleteQuietly(reportFile);
        }
        reportFile = new File(config.getReportFolder(), fileManager.getReportFileNamePdf(id));
        if (reportFile.exists() && reportFile.isFile()) {
            FileUtils.deleteQuietly(reportFile);
        }
    }
*/
   /* @RequestMapping(value = "/{domain}/xml/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public void deleteXML(@PathVariable String domain, @PathVariable String id) {
        DomainConfig domainConfig = domainConfigCache.getConfigForDomain(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        File reportFile = new File(config.getReportFolder(), fileManager.getInputFileName(id));
        if (reportFile.exists() && reportFile.isFile()) {
            FileUtils.deleteQuietly(reportFile);
        }
    }*/

    /*@Scheduled(fixedDelayString = "${validator.cleanupPollingRate}")
    public void cleanUpFiles() {
        long currentMillis = System.currentTimeMillis();
        File reportFolder = config.getReportFolder();
        if (reportFolder != null) {
            File[] files = reportFolder.listFiles();
            if (files != null) {
                for (File file: files) {
                    if (!handleReportFile(file, config.getInputFilePrefix(), currentMillis, config.getMinimumCachedInputFileAge())) {
                        handleReportFile(file, config.getReportFilePrefix(), currentMillis, config.getMinimumCachedReportFileAge());
                    }
                }
            }
        }
    }*/

    private boolean handleReportFile(File file, String prefixToConsider, long currentTime, long minimumCacheTime) {
        boolean handled = false;
        if (file.getName().startsWith(prefixToConsider)) {
            handled = true;
            if (currentTime - file.lastModified() > minimumCacheTime) {
                FileUtils.deleteQuietly(file);
            }
        }
        return handled;
    }

}
