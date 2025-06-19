/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.shacl;

/**
 * Class encapsulating the SPARQL endpoint query configuration to be used in a validator call.
 */
public class SparqlQueryConfig {

    private String endpoint;
    private String query;
    private String username;
    private String password;
    private String preferredContentType;

    /**
     * Constructor.
     */
    public SparqlQueryConfig() {
        this(null, null, null, null, null);
    }

    /**
     * Constructor.
     *
     * @param endpoint The endpoint's URL.
     * @param query The CONSTRUCT query to use.
     * @param username The username for the endpoint's authentication.
     * @param password The password for the endpoint's authentication.
     * @param preferredContentType The RDF syntax to request from the endpoint.
     */
    public SparqlQueryConfig(String endpoint, String query, String username, String password, String preferredContentType) {
        this.endpoint = endpoint;
        this.query = query;
        this.username = username;
        this.password = password;
        this.preferredContentType = preferredContentType;
    }

    /**
     * @return The endpoint URL.
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * @param endpoint The endpoint URL.
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @return The CONSTRUCT query to use.
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query The CONSTRUCT query to use.
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * @return The username for the endpoint's authentication.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username The username for the endpoint's authentication.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return The password for the endpoint's authentication.
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password The password for the endpoint's authentication.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return The RDF syntax to request from the endpoint.
     */
    public String getPreferredContentType() {
        return preferredContentType;
    }

    /**
     * @param preferredContentType The RDF syntax to request from the endpoint.
     */
    public void setPreferredContentType(String preferredContentType) {
        this.preferredContentType = preferredContentType;
    }
}
