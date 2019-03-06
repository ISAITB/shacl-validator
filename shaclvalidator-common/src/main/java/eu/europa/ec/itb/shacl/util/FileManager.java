package eu.europa.ec.itb.shacl.util;

import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.ApplicationConfig;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by simatosc on 12/08/2016.
 */
@Component
public class FileManager {

    private static JAXBContext REPORT_CONTEXT;
    private static ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private static Logger logger = LoggerFactory.getLogger(FileManager.class);

    static {
        try {
            REPORT_CONTEXT = JAXBContext.newInstance(TAR.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create JAXB context for TAR class", e);
        }
    }

    @Autowired
    ApplicationConfig config;

    public String writeXML(String domain, String xml) throws IOException {
        UUID fileUUID = UUID.randomUUID();
        String xmlID = domain+"_"+fileUUID.toString();
        File outputFile = new File(config.getReportFolder(), getInputFileName(xmlID));
        outputFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile(outputFile, xml, Charset.defaultCharset());
        return xmlID;
    }

    public String getInputFileName(String uuid) {
        return config.getInputFilePrefix()+uuid+".xml";
    }

    public String getReportFileNameXml(String uuid) {
        return config.getReportFilePrefix()+uuid+".xml";
    }

    public String getReportFileNamePdf(String uuid) {
        return config.getReportFilePrefix()+uuid+".pdf";
    }

    public void saveReport(TAR report, String xmlID) {
        File outputFile = new File(config.getReportFolder(), getReportFileNameXml(xmlID));
        saveReport(report, outputFile);
    }
    
    public void saveReport(Model report, File outputFile, String lang) { 
    	FileWriter out = null;   	
    	try {
        	out = new FileWriter(outputFile);   	
    	    report.write(out, lang);
    	    
    	} catch (IOException e) {
			logger.error("Error when saving the report: "+e.getMessage());
			e.printStackTrace();
		}
    	finally {
    	   try {
    	       out.close();
    	   }
    	   catch (IOException closeException) {
   			logger.error("Error when saving the report: "+closeException.getMessage());
    	   }
    	}
    	
    }

    public void saveReport(TAR report, File outputFile) {
        try {
            Marshaller m = REPORT_CONTEXT.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            outputFile.getParentFile().mkdirs();
            
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            Document document = docBuilderFactory.newDocumentBuilder().newDocument();
            m.marshal(OBJECT_FACTORY.createTestStepReport(report), document);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "{http://www.gitb.com/core/v1/}value");
            try (OutputStream fos = new FileOutputStream(outputFile)) {
                transformer.transform(new DOMSource(document), new StreamResult(fos));
                fos.flush();
            } catch(IOException e) {
                logger.warn("Unable to save XML report", e);
            }

        } catch (Exception e) {
            logger.warn("Unable to marshal XML report", e);
        }
    }

    public boolean checkFileType(InputStream stream) throws IOException {
        Tika tika = new Tika();
        String type = tika.detect(stream);
        return config.getAcceptedMimeTypes().contains(type);
    }

}
