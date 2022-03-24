package eu.europa.ec.itb.shacl.rest;

import eu.europa.ec.itb.shacl.rest.errors.ErrorInfo;
import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import eu.europa.ec.itb.validation.commons.web.errors.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Locale;

/**
 * Handle all errors linked to validator REST API calls.
 *
 * The validator explicitly raises NotFoundException for cases of invalid requested domains. Once the validation
 * is allowed to proceed all errors that are expected can be reported back to users are raised as ValidatorExceptions.
 * All other exceptions are considered unexpected and are raised with a generic error message.
 *
 * @see NotFoundException
 * @see ValidatorException
 */
@ControllerAdvice(assignableTypes = {ShaclController.class})
public class ErrorHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorHandler.class);

    /**
     * Handle the "not found" errors. These typically link to a domain being requested that is not configured or
     * using the API when not supported.
     *
     * @param ex The exception.
     * @param request The current request.
     * @return The response.
     */
    @ExceptionHandler(value = {NotFoundException.class})
    protected ResponseEntity<Object> handleNotFound(NotFoundException ex, WebRequest request) {
        if (LOG.isWarnEnabled()) {
            LOG.warn(String.format("Caught NotFoundException for domain [%s]", ex.getRequestedDomain()), ex);
        }
        return handleExceptionInternal(ex, new ErrorInfo("The requested resource could not be found"), new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    /**
     * Handle errors that are expected and can be shared with users.
     *
     * @param ex The exception.
     * @param request The current request.
     * @return The response.
     */
    @ExceptionHandler(value = {ValidatorException.class})
    protected ResponseEntity<Object> handleValidatorException(ValidatorException ex, WebRequest request) {
        if (LOG.isErrorEnabled()) {
            LOG.error(String.format("Caught ValidatorException: %s", ex.getMessageForLog()), ex);
        }
        return handleExceptionInternal(ex, new ErrorInfo(ex.getMessageForDisplay(new LocalisationHelper(Locale.ENGLISH))), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Handle all other exceptions.
     *
     * @param ex The exception.
     * @param request The current request.
     * @return The response.
     */
    @ExceptionHandler(value = {Exception.class})
    protected ResponseEntity<Object> handleUnexpectedErrors(Exception ex, WebRequest request) {
        LOG.error("Caught Exception", ex);
        return handleExceptionInternal(ex, new ErrorInfo("An unexpected error occurred during validation"), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

}
