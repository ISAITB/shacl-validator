package eu.europa.ec.itb.shacl;

public class SparqlQueryConfig {

    private String endpoint;
    private String query;
    private String username;
    private String password;
    private String preferredContentType;

    public SparqlQueryConfig() {
    }

    public SparqlQueryConfig(String endpoint, String query, String username, String password, String preferredContentType) {
        this.endpoint = endpoint;
        this.query = query;
        this.username = username;
        this.password = password;
        this.preferredContentType = preferredContentType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPreferredContentType() {
        return preferredContentType;
    }

    public void setPreferredContentType(String preferredContentType) {
        this.preferredContentType = preferredContentType;
    }
}
