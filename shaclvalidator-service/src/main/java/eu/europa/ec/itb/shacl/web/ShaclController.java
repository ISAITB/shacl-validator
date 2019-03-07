package eu.europa.ec.itb.shacl.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.topbraid.shacl.util.ModelPrinter;

import com.gitb.core.AnyContent;
import com.gitb.vs.ValidateRequest;

import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.validation.SHACLValidator;
import eu.europa.ec.itb.shacl.validation.ValidationConstants;

/**
 * Simple REST controller to allow an easy way of providing a message for the test bed.
 *
 * This implementation acts a sample of how messages could be sent to the test bed. In this case this is done
 * via a simple HTTP GET service that accepts two parameters:
 * <ul>
 *     <li>session: The test session ID. Not providing this will send notifications for all active sessions.</li>
 *     <li>message: The message to send. Not providing this will consider an empty string.</li>
 * </ul>
 * One of the key points to define when using a messaging service is the approach to match received messages to
 * waiting test bed sessions. In this example a very simple approach is foreseen, expecting the session ID to be
 * passed as a parameter (or be omitted to signal all sessions). A more realistic approach would be as follows:
 * <ol>
 *     <li>The messaging service records as part of the state for each session a property that will serve to
 *     uniquely identify it. This could be a transaction identifier, an endpoint address, or some other metadata.</li>
 *     <li>The input provided to the messaging service includes the identifier to use for session matching.</li>
 *     <li>Given such an identifier, the current session state is scanned to find the corresponding session.</li>
 * </ol>
 * In addition, keep in mind that the communication protocol involved in sending and receiving messages could be anything.
 * In this example we use a HTTP GET request but this could be an email, a SOAP web service call, a polled endpoint or
 * filesystem location; anything that corresponds to the actual messaging needs.
 */
@RestController
public class ShaclController {

    @Autowired
    ApplicationContext ctx;
    
    /*private final DomainConfig domainConfig;

    public ShaclController(DomainConfig domainConfig) {
        this.domainConfig = domainConfig;
    }*/
    /**
     * HTTP GET service to receive input for the test bed.
     *
     * Input received here will be provided back to the test bed as a response to its 'receive' step.
     *
     * @param session The test session ID this relates to. Omitting this will consider all active sessions.
     * @param message The message to send. No message will result in an empty string.
     * @return A text configuration message.
     */
    @RequestMapping(value = "/validator", method = RequestMethod.GET)
    //public String provideMessage(@RequestParam(value="domain", required = true) String domain, @RequestParam(value="file") MultipartFile  file, 
    //		@RequestParam(value="type") String  validatorType, @RequestParam(value="ValidateRequest") ValidateRequest validateRequest) {
    public String provideMessage(@RequestParam(value="ValidateRequest") ValidateRequest validateRequest) {
    	
    	//MDC.put("domain", domainConfig.getDomain());
    	
    	List<AnyContent> fileInputs = getXMLInput(validateRequest);
		String validationType = null;
		if (fileInputs.isEmpty()) {
			throw new IllegalArgumentException("You must provide the file to validate");
		}
		if (fileInputs.size() > 1) {
			throw new IllegalArgumentException("A single input file is expected");
		}
		
       // if (domainConfig.hasMultipleValidationTypes()) {
            List<AnyContent> validationTypeInputs = getTypeInput(validateRequest);
            if (validationTypeInputs.isEmpty()) {
                throw new IllegalArgumentException("You must provide the type of invoice to perform");
            }
            if (validationTypeInputs.size() > 1) {
                throw new IllegalArgumentException("A single invoice type is expected");
            }
            validationType = validationTypeInputs.get(0).getValue();
        /*    if (!domainConfig.getType().contains(validationType)) {
                throw new IllegalArgumentException("Invalid invoice type provided ["+validationType+"]");
            }
        }*/
        
       // SHACLValidator validator = ctx.getBean(SHACLValidator.class, fileInputs.get(0), validationType, domainConfig);
        SHACLValidator validator = ctx.getBean(SHACLValidator.class, fileInputs.get(0), validationType);
        
        Model shaclReport = validator.validateAll();
        
        String shaclReportstr = ModelPrinter.get().print(shaclReport);
                
        return String.format("Sent message [%s]", shaclReportstr);
    }
    

    private List<AnyContent> getXMLInput(ValidateRequest validateRequest) {
        return getInputFor(validateRequest, ValidationConstants.INPUT_XML);
    }

    private List<AnyContent> getTypeInput(ValidateRequest validateRequest) {
        return getInputFor(validateRequest, ValidationConstants.INPUT_TYPE);
    }

    private List<AnyContent> getInputFor(ValidateRequest validateRequest, String name) {
        List<AnyContent> inputs = new ArrayList<AnyContent>();
        if (validateRequest != null) {
            if (validateRequest.getInput() != null) {
                for (AnyContent anInput: validateRequest.getInput()) {
                    if (name.equals(anInput.getName())) {
                        inputs.add(anInput);
                    }
                }
            }
        }
        return inputs;
    }

}
