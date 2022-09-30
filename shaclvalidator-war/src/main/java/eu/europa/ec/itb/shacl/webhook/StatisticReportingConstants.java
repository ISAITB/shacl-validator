package eu.europa.ec.itb.shacl.webhook;

/**
 * Constants linked to statistics reporting.
 */
public class StatisticReportingConstants {

    /**
     * Constructor to prevent instantiation.
     */
    private StatisticReportingConstants() { throw new IllegalStateException("Utility class"); }

    /** The web API. */
    public static final String WEB_API = "web";
    /** The web (minimal) API. */
    public static final String WEB_MINIMAL_API = "web_minimal";
    /** The SOAP API. */
    public static final String SOAP_API = "soap";
    /** The REST API. */
    public static final String REST_API = "rest";
    
}
