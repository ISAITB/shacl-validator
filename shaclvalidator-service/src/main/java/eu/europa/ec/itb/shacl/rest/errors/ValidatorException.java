package eu.europa.ec.itb.shacl.rest.errors;

public class ValidatorException extends RuntimeException {

    private static final String MESSAGE_DEFAULT = "An unexpected error was raised during validation.";

    public ValidatorException(Throwable cause) {
        this(MESSAGE_DEFAULT, cause);
    }

	public ValidatorException(String message) {
		this(message, null);
	}

    public ValidatorException(String message, Throwable cause) {
        super(message, cause);
    }

}
