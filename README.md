# Introduction

Application used for the validation of RDF documents by means of:

* A REST API.
* A SOAP API.
* A web form.
* Standalone validator.

The validator can be used with a single or multiple validation domains, i.e. validation cases that should be considered
as distinct. Note that each such domain can still contain within it multiple validation types - the validation domain
is used more to split distinct user groups, whereas the validation type is used for different types of validation within
a specific user group.

Configuration of each domain's validation is via a property file contained within the domain's subfolder under the 
configured resource root. 

The different application modes can be disabled/enabled as configuration properties in this domain configuration.

The application is built using Spring-Boot. The project includes a sample definition of CPSV-AP to work out of 
the box (under the `etc` folder). This however should be replaced with the actual implementation depending on the 
installation. 

# Building

Issue `mvn clean install`

The resulting artefacts can then be retrieved from:
- `shaclvalidator-war` when running as a web app
- `shaclvalidator-jar` when running as a command line tool

## For development

An `application-dev.properties` configuration file is present in the `shaclvalidator-common` module. This can be adapted
to easily run from an IDE or a completely separate configuration file can be provided from an external config location.

To run change first to the required module:
- `shaclvalidator-war` to run as a web app
- `shaclvalidator-jar` to run as a command line tool

Then, from this directory do

```
mvn spring-boot:run
```

# Running the applications

Both web and standalone versions require at least Java 8 to run.

## Web application

The application is accessible at:

* REST-API: http://localhost:8080/shacl/DOMAIN/api/validate
* SOAP-API: http://localhost:8080/shacl/soap/DOMAIN/validation?wsdl
* Web form: http://localhost:8080/shacl/DOMAIN/upload

Note that all configuration properties in `application.properties` can be overriden by means of environment variables
(e.g. set in a downstream Dockerfile). 

## Standalone

The standalone mode loads the validation resources from the jar file produced from the the resources' module that is copied as an entry to the standalone jar's contents. Because of this however, the standalone version can't be ran from within the IDE.

To build the standalone validator issue

```
mvn clean install
```
And get `validator.jar` from the jar module's target folder. To run this issue:

```
java -jar validator.jar
```

# Configuration and use

The key point to consider when using this validator is the validation domains it will consider. This is defined through
the **mandatory configuration property** `validator.resourceRoot` which needs to point to a folder that contains for 
each supported domain, a sub-folder with the validation artefacts (that can be arbitrarily organised) and a property
file (e.g. `config.properties`) that defines the configuration for that specific domain. This property can be defined
either in a separate `application.properties` file, or more simply, passed as a system property or environment variable
(using an environment variable a particularly easy approach when creating a downstream docker image).

The name of the domain sub-folder will be used as the domain name. In addition, if the `validator.resourceRoot` parent
folder contains also directories that should not be considered, the specific domains (i.e. sub-folder named) to be 
considered can be specified explicitly through the `validator.domain` property, a comma-separate listing of the domain
folder named to consider.

## Use via docker

To use as a docker container do the following:
1. Create a folder `my-app`.
2. In this folder copy create a folder named e.g. `domain`.
3. Copy in the `domain` folder the validation artefacts and domain configuration property file.
4. Create a Dockerfile as follows:
```
FROM isaitb/shacl-validator:latest

ENV validator.resourceRoot /validator/
COPY domain /validator/domain/
```  
5. Build the docker image and proceed to use it.

**Important:** The naming of the `domain` folder in the above example is important as it will be used for the 
request paths for all communication channels both the REST API (e.g. http://localhost:8080/api/domain/) and also for 
the SOAP web service endpoints (e.g. http://localhost:8080/shacl/soap/DOMAIN/validation?wsdl). 

## Configuration property reference

The tool supports configuration at two levels:
* The overall application.
* Each configured validation domain.

## Application-level configuration

The properties defined here can be specified in a separate Spring Boot `application.properties` file or passed in via
system properties or environment variables. Apart from what is listed, any Spring Boot configuration property can be
defined.

| Property | Description | Type | Default value |
| --- | --- | --- | --- |
| `validator.resourceRoot` | The root folder under which domain sub-folders will be loaded. | String | - |
| `validator.domain` | The names of the domain sub-folders to consider. | Comma-separated Strings | - |
| `validator.domainName.XYZ` | The name to display for a given domain folder. | String | The folder name is used |  
| `logging.path` | Logging path. | String | `/validator/logs` |
| `validator.tmpFolder` | Temp folder path. | String | `/validator/tmp` |
| `validator.acceptedShaclExtensions` | Accepted SHACL extensions.  | Comma-separated Strings | `ttl,rdf` |
| `validator.acceptedHeaderAcceptTypes` | Accepted content types requested via the Accepts header.  | Comma-separated Strings | `application/ld+json, application/rdf+xml, text/turtle, application/n-triples` |
| `validator.defaultReportSyntax` | The default report syntax (mime type) if none is requested. | Comma-separated Strings | `application/rdf+xml` |

## Domain-level configuration

The properties here define how a specific validation domain is configured. They need to be placed in a property file
(any file name ending with `.properties`) within a domain sub-folder under the configured `validator.resourceRoot`.

| Property | Description | Type | Default value |
| --- | --- | --- | --- |
| `validator.channels` | Comma separated list of features to have enabled. Possible values are (`form`, `rest_api`, `soap_api`). | Comma-separated Strings | `form,rest_api,soap_api` |
| `validator.type` | A comma-separated list of supported invoice types. Values need to be reflected in properties `validator.typeLabel`, `validator.shaclFile`, `validator.externalShapes`. | Comma-separated Strings | - |
| `validator.typeLabel.XYZ` | Label to display for a given validator type (added as a postfix of validator.typeLabel). | String | - |
| `validator.shaclFile.XYZ` | A comma-separated list of SHACL files loaded for a given validation type (added as a postfix). These can be file or folders. | Comma-separated Strings | - |
| `validator.shaclFile.XYZ.remote.A.url` | The SHACL files loaded for a given validation type (added as a postfix) as URL. | String | - |
| `validator.shaclFile.XYZ.remote.A.type` | The content syntax (mime type) of the SHACL files loaded for a given validation type (added as a postfix). | String | - |
| `validator.externalShapes.XYZ` | External shapes are allowed for a given validation type (added as a postfix) as Boolean. | Boolean | `false` |
| `validator.includeTestDefinition` | Whether tests should be included in the resulting reports. | Boolean | `true` |
| `validator.reportsOrdered` | Whether the reports are ordered. | Boolean | `false` |
| `validator.defaultReportSyntax` | The default report syntax (mime type) if none is requested (otherwise the global default applies). | `application/rdf+xml` |
| `validator.contentSyntax` | The accepted content syntaxes (mime types) in the web form. | `application/ld+json, application/rdf+xml, text/turtle, application/n-triples` |
| `validator.webServiceId` | The ID of the web service. | String | `ValidatorService` |
| `validator.webServiceDescription.contentToValidate` | The description of the web service for element "contentToValidate". | String | - |
| `validator.webServiceDescription.contentSyntax` | The description of the web service for element "contentSyntax". | String | - |
| `validator.webServiceDescription.validationType` | The description of the web service for element "validationType". | String | - |
| `validator.webServiceDescription.externalRules` | The description of the web service for element "externalRules". | String | - |
| `validator.supportMinimalUserInterface` | A minimal UI is available if this is enabled. | Boolean | `false` |
| `validator.bannerHtml` | Configurable HTML banner replacing the text title. | String | - |

# Plugin development

The SHACL validator supports custom plugins to extend the validation report. Plugins are implementations of the GITB validation service API for which the following
applies. Note that plugin JAR files need to be built as "all-in-one" JARs.

## Input to plugins

The SHACL validator calls plugins in sequence passing in the following input:

| Input name | Type | Description |
| --- | --- | --- |
| `contentToValidate` | `String` | The absolute and full path to the input provided to the validator. This is stored in the file system as an RDF/XML file. |
| `domain` | `String` | The validation domain relevant to the specific validation call. |
| `validationType` | `String` | The validation type of the domain that is selected for the specific validation call. |
| `tempFolder` | `String` | The absolute and full path to a temporary folder for plugins. This will be automatically deleted after all plugins complete validation. |

## Output from plugins

The output of plugins is essentially a GITB `ValidationResponse` that wraps a `TAR` instance. The report items within this `TAR` instance are mapped to a SHACL
validation report as follows (where `sh` below this is a shorthand for `http://www.w3.org/ns/shacl`):

| TAR report item property | sh:ValidationResult property | Required? | Description |
| --- | --- | --- | --- |
| `item.location` | `sh#focusNode` | Yes | Set with the IRI of the relevant resource (e.g. `http://my.sample.po/po#item3`). |
| `item.description` | `sh#resultMessage` | No | The message to be used in the report (this is optional but should be provided). |
| - | `sh#resultSeverity` | Yes | This is determined by the item's type given its element name (mapping `error`, `warning`, `info` to `sh#Violation`, `sh#Warning`, `sh#Info` respectively). |
| `item.test` | `sh#resultPath` | No | This is set as an IRI to identify the specific path linked to this (e.g. `http://itb.ec.europa.eu/sample/po#quantity`). |
| `item.assertionID` | `sh#sourceConstraintComponent` | Yes | Set as an IRI for the source contraint component using the values defined in the SHACL specification (e.g. `sh#MinExclusiveConstraintComponent`). |
| `item.value` | `sh#value` | No | Optionally set as a literal string in case a value should be included. |