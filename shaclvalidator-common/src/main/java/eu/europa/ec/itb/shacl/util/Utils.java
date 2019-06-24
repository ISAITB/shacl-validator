package eu.europa.ec.itb.shacl.util;

import com.gitb.tr.TAR;
import eu.europa.ec.itb.shacl.validation.SHACLReportHandler;
import org.apache.jena.rdf.model.Model;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.GregorianCalendar;
import java.util.Stack;

public class Utils {

    public static String LINE_NUMBER_KEY_NAME = "lineNumber";

    public static XMLGregorianCalendar getXMLGregorianCalendarDateTime() throws DatatypeConfigurationException {
        GregorianCalendar calendar = new GregorianCalendar();
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
    }

    public static byte[] serialize(Node content) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            transformer.transform(new DOMSource(content), new StreamResult(baos));
        } catch (TransformerException te) {
            throw new IllegalStateException(te);
        }
        return baos.toByteArray();
    }

    public static Document emptyDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Document readXMLWithLineNumbers(InputStream is) throws IOException, SAXException {
        final Document doc;
        SAXParser parser;
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
            final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
        }

        final Stack<Element> elementStack = new Stack<>();
        final StringBuilder textBuffer = new StringBuilder();
        final DefaultHandler handler = new DefaultHandler() {
            private Locator locator;

            @Override
            public void setDocumentLocator(final Locator locator) {
                this.locator = locator; // Save the locator, so that it can be used later for line tracking when traversing nodes.
            }

            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                addTextIfNeeded();
                final org.w3c.dom.Element el = doc.createElement(qName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    el.setAttribute(attributes.getQName(i), attributes.getValue(i));
                }
                el.setUserData(LINE_NUMBER_KEY_NAME, String.valueOf(this.locator.getLineNumber()), null);
                elementStack.push(el);
            }

            @Override
            public void endElement(final String uri, final String localName, final String qName) {
                addTextIfNeeded();
                final org.w3c.dom.Element closedEl = elementStack.pop();
                if (elementStack.isEmpty()) { // Is this the root element?
                    doc.appendChild(closedEl);
                } else {
                    final org.w3c.dom.Element parentEl = elementStack.peek();
                    parentEl.appendChild(closedEl);
                }
            }

            @Override
            public void characters(final char ch[], final int start, final int length) throws SAXException {
                textBuffer.append(ch, start, length);
            }

            // Outputs text accumulated under the current node
            private void addTextIfNeeded() {
                if (textBuffer.length() > 0) {
                    final org.w3c.dom.Element el = elementStack.peek();
                    final Node textNode = doc.createTextNode(textBuffer.toString());
                    el.appendChild(textNode);
                    textBuffer.delete(0, textBuffer.length());
                }
            }
        };
        parser.parse(is, handler);
        return doc;

    }
    
    public static InputStream getInputStreamForValidation(InputStream inputToValidate) {
        byte[] inputBytes;
        
        try {
            inputBytes = StreamUtils.copyToByteArray(inputToValidate);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
            
        return new ByteArrayInputStream(inputBytes);
    }

    public static TAR getTAR(Model report, Path inputFilePath, Model aggregatedShapes, boolean reportsOrdered) {
    	//SHACL report: from Model to TAR
		try {
			String contentToValidateString = new String(Files.readAllBytes(inputFilePath));
	    	
	    	SHACLReportHandler reportHandler = new SHACLReportHandler(contentToValidateString, aggregatedShapes, report, reportsOrdered);
	    	
	    	return reportHandler.createReport();
		} catch (IOException e) {
            throw new IllegalStateException("Error during the transformation of the report to TAR");
		}
    }
}
