package eu.europa.ec.itb.shacl;

import java.util.List;

/**
 * Exception that takes additional messages to report.
 */
public class ExtendedValidatorException extends eu.europa.ec.itb.validation.commons.error.ValidatorException {

    private final List<String> additionalInformation;

    /**
     * @param message The main message.
     * @param additionalInformation The additional messages to report.
     */
    public ExtendedValidatorException(String message, List<String> additionalInformation) {
        super(message);
        this.additionalInformation = additionalInformation;
    }

    /**
     * @return The additional messages to report.
     */
    public List<String> getAdditionalInformation() {
        return additionalInformation;
    }

}
