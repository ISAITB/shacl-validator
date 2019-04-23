package eu.europa.ec.itb.shacl.rest.errors;

import java.time.LocalDateTime;

public class ErrorInfo {

    private String message;
    private LocalDateTime timestamp;

    public ErrorInfo(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
