package eu.europa.ec.itb.shacl.upload.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "The requested resource could not be found")
public class NotFoundException extends RuntimeException {
}
