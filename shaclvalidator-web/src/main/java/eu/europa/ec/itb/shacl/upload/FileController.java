package eu.europa.ec.itb.shacl.upload;

import org.apache.commons.io.FileUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;
import eu.europa.ec.itb.shacl.ValidatorChannel;
import eu.europa.ec.itb.shacl.upload.errors.NotFoundException;
import eu.europa.ec.itb.shacl.validation.FileManager;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

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
/*
    @RequestMapping(value = "/{domain}/xml/{id}", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
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
    }
*/
    @GetMapping(value = "/{domain}/report/{id}/rdf", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public FileSystemResource getReportRDF(@PathVariable String domain, @PathVariable String id, HttpServletResponse response) {
        DomainConfig domainConfig = domainConfigCache.getConfigForDomain(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        
        File reportFile = new File(config.getTmpFolder(), fileManager.getReportFileNameRdf(id, domainConfig.getDefaultReportSyntax()));
        
        if (reportFile.exists() && reportFile.isFile()) {
            if (response != null) {
                response.setHeader("Content-Disposition", "attachment; filename=report_" + id + "." + fileManager.getLanguage(domainConfig.getDefaultReportSyntax()));
            }
            return new FileSystemResource(reportFile);
        } else {
            throw new NotFoundException();
        }
    }

    @GetMapping(value = "/{domain}/report/{id}/pdf", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public FileSystemResource getReportPdf(@PathVariable String domain, @PathVariable String id, HttpServletResponse response) {
        DomainConfig domainConfig = domainConfigCache.getConfigForDomain(domain);
        if (domainConfig == null || !domainConfig.getChannels().contains(ValidatorChannel.FORM)) {
            throw new NotFoundException();
        }
        MDC.put("domain", domain);
        File reportFile = new File(config.getTmpFolder(), fileManager.getReportFileNamePdf(id));
        if (!(reportFile.exists() && reportFile.isFile())) {
            // Generate the PDF.
            File reportFileRdf = new File(config.getTmpFolder(), fileManager.getReportFileNameRdf(id, domainConfig.getDefaultReportSyntax()));
            if (reportFileRdf.exists() && reportFileRdf.isFile()) {
                reportGenerator.writeReport(domainConfig, reportFileRdf, reportFile);
            } else {
                throw new NotFoundException();
            }
        }
        if (response != null) {
            response.setHeader("Content-Disposition", "attachment; filename=report_"+id+".pdf");
        }
        return new FileSystemResource(reportFile);
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
