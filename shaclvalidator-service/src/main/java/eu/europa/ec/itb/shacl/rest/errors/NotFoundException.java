package eu.europa.ec.itb.shacl.rest.errors;

public class NotFoundException extends RuntimeException {

    private final String requestedDomain;

    public NotFoundException(String requestedDomain) {
        this.requestedDomain = requestedDomain;
    }

    public String getRequestedDomain() {
        return requestedDomain;
    }
}
