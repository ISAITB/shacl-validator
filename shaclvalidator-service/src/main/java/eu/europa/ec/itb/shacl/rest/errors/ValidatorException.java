package eu.europa.ec.itb.shacl.rest.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class ValidatorException extends RuntimeException {
    public static final String message_contentToValidate	= "The provided content could not be successfully parsed.";
    public static final String message_ruleSet     			= "The provided rules could not be successfully parsed.";
    public static final String message_multiple     		= "The type of validation to perform must be specified.";
    public static final String message_parameters    	 	= "Provided parameter value is invalid.";
    public static final String message_support     			= "The requested feature is not supported yet.";
    public static final String message_default     			= "An unexpected error was raised during validation.";
    public static final String message_syntaxRequired       = "The syntax must be defined for the provided content.";

	public ValidatorException(String message) {
		super(message, null, false, false);
	}	
}
