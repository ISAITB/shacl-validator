package eu.europa.ec.itb.shacl.upload;

import com.gitb.reports.ReportGenerator;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.DomainConfig;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

@Component
public class ReportGeneratorBean {

    private ReportGenerator reportGenerator = new ReportGenerator();

    public void writeReport(DomainConfig config, TAR report, File outFile) {
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            reportGenerator.writeTARReport(report, config.getReportTitle(), fos);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate PDF report", e);
        }
    }

}
