package eu.europa.ec.itb.shacl.rest.errors;

import java.time.LocalDateTime;

/**
 * DTO to wrap an error message's information.
 */
public class ErrorInfo {

    private final String message;
    private final LocalDateTime timestamp;

    /**
     * Constructor.
     *
     * @param message The message to wrap.
     */
    public ErrorInfo(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * @return The message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return The timestamp this object was created.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
