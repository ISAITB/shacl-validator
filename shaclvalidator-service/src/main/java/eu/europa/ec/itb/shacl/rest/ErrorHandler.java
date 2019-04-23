package eu.europa.ec.itb.shacl.rest;

import eu.europa.ec.itb.shacl.rest.errors.ErrorInfo;
import eu.europa.ec.itb.shacl.rest.errors.NotFoundException;
import eu.europa.ec.itb.shacl.rest.errors.ValidatorException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {NotFoundException.class})
    protected ResponseEntity<Object> handleNotFound(RuntimeException ex, WebRequest request) {
        return handleExceptionInternal(ex, new ErrorInfo("The requested resource could not be found"), new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(value = {ValidatorException.class})
    protected ResponseEntity<Object> handleValidatorException(RuntimeException ex, WebRequest request) {
        return handleExceptionInternal(ex, new ErrorInfo(ex.getMessage()), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(value = {IllegalArgumentException.class, IllegalStateException.class})
    protected ResponseEntity<Object> handleUnexpectedErrors(RuntimeException ex, WebRequest request) {
        return handleExceptionInternal(ex, new ErrorInfo("An unexpected error occurred during validation"), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

}
