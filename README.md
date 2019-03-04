# Introduction

Application used for the validation of XML documents by means of:

* A GITB-compliance validation service (SOAP web service).
* A simple web form that can receive an XML file.
* Polling of an email address.
* Standalone validator.

The validator can be used with a single or multiple validation domains, i.e. validation cases that should be considered
as distinct. Note that each such domain can still contain within it multiple validation types - the validation domain
is used more to split distinct user groups, whereas the validation type is used for different types of validation within
a specific user group.

Configuration of each domain's validation is via a property file contained within the domain's subfolder under the 
configured resource root. 

The different application modes (above) can be disabled/enabled as configuration properties in this domain 
configuration.

The application is built using Spring-Boot. The project includes a sample definition of UBL invoices to work out of 
the box. This however should be replaced with the actual implementation depending on the installation. 

# Building

Issue `mvn clean install`

The resulting artefacts can then be retrieved from:
- `xmlvalidator-war` when running as a web app
- `xmlvalidator-jar` when running as a command line tool

## For development

An `application-dev.properties` configuration file is present in the `xmlvalidator-common` module. This can be adapted
to easily run from an IDE or a completely separate configuration file can be provided from an external config location.

To run change first to the required module:
- `xmlvalidator-war` to run as a web app
- `xmlvalidator-jar` to run as a command line tool

Then, from this directory do

```
mvn spring-boot:run
```

# Running the applications

Both web and standalone versions require at least Java 8 to run.

## Web application

The application is accessible at:

* Web form: http://localhost:8080/DOMAIN/upload
* GITB-compliant WS: http://localhost:8080/api/DOMAIN/validation?wsdl

Note that all configuration properties in `application.properties` can be overriden by means of environment variables
(e.g. set in a downstream Dockerfile). 

## Standalone

The standalone mode loads the validation resources from the jar file produced from the the resources' module that is
copied as an entry to the standalone jar's contents. Because of this however, the standalone version can't be ran from
within the IDE.

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

## Use as a standalone application

To use as a standalone application the simplest approach is as follows:
1. Copy `validator.war` in a target folder.
2. Create a folder (e.g. `resources`) in the target folder.
3. Place into the `resources` folder a sub-folder for each supported validation domain.
4. Launch the application using `java -jar validator.war` passing it the following system properties:
..* `validator.resourceRoot` with the full path to the target folder.
..* `logging.path` the folder to hold the application's log output.
..* `validator.reportFolder` the folder to hold temporarily created report files.

## Use via docker

To use as a docker container do the following:
1. Create a folder `my-app`.
2. In this folder copy create a folder named e.g. `domain`.
3. Copy in the `domain` folder the validation artefacts and domain configuration property file.
4. Create a Dockerfile as follows:
```
FROM isaitb/xml-validator:latest

ENV validator.resourceRoot /validator/
COPY domain /validator/domain/
```  
5. Build the docker image and proceed to use it.

**Important:** The naming of the `domain` folder in the above example is important as it will be used for the 
request paths for both the web UI (e.g. http://localhost:8080/domain/upload) and also for the web service endpoints
(e.g. http://localhost:8080/api/domain/validation?wsdl). 

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
| `logging.path` | Logging path. | String | `/validator/logs` |
| `validator.reportFolder` | Report path. | String | `/validator/reports` |
| `validator.cleanupPollingRate` | The rate at which the temp reports folder is polled for cleanup (in ms). | Integer | `60000` |
| `validator.mailPollingRate` | The rate at which the configured email addresses (if configured) are polled for received input files (in ms). | Integer | `60000` |
| `validator.inputFilePrefix` | Prefix of input files in the report folder. | String | `ITB-` |
| `validator.minimumCachedInputFileAge` | Time to keep XML input files in milliseconds (600000 = 10 minutes). | Integer | `600000` |
| `validator.reportFilePrefix` | Prefix of report files in the report folder. | String | `TAR-` |
| `validator.acceptedMimeTypes` | Accepted mime-types for input files.  | Comma-separated Strings | `application/xml, text/xml, text/plain` |
| `validator.acceptedSchematronExtensions` | Accepted schematron extensions.  | Comma-separated Strings | `xsl,xslt,sch` |

## Domain-level configuration

The properties here define how a specific validation domain is configured. They need to be placed in a property file
(any file name ending with `.properties`) within a domain sub-folder under the configured `validator.resourceRoot`.

| Property | Description | Type | Default value |
| --- | --- | --- | --- |
| `validator.channels` | Comma separated list of features to have enabled. Possible values are (`form`, `email`, `webservice`). | Comma-separated Strings | `form,webservice` |
| `validator.type` | A comma-separated list of supported invoice types. Values need to be reflected in properties `validator.typeLabel`, `validator.schemaFile`, `validator.schematronFolder`. | Comma-separated Strings | - |
| `validator.typeLabel.XYZ` | Label to display in the web form for a given invoice type (added as a postfix of validator.typeLabel). Only displayed if there are multiple types. | String | - |
| `validator.webServiceId` | The ID of the web service. | String | `ValidatorService` |
| `validator.webServiceDescription.xml` | The description of the web service for element "xml". | String | - |
| `validator.webServiceDescription.type` | The description of the web service for element "type". Only displayed if there are multiple types. | String | - |
| `validator.schemaFile.XYZ` | The XSD files loaded for a given invoice type (added as a postfix). This can be a file or folder (must never start with a '/'). | String | - |
| `validator.schematronFile.XYZ` | The schematron files loaded for a given invoice type (added as a postfix). This can be a file or folder (must never start with a '/'). | String | - |
| `validator.includeTestDefinition` | Whether tests should be included in the resulting reports. | Boolean | `true` |
| `validator.reportsOrdered` | Whether the reports are ordered. | Boolean | `false` |
| `validator.showAbout` | Whether or not to show the about panel on the web UI. | Boolean | `true` | 

In case the email channel is enabled the following properties need to be provided:

| Property | Description | Type | Default value |
| --- | --- | --- | --- |
| `validator.mailFrom` | The FROM address to use. | String | - |
| `validator.mailAuthEnable` | Whether authentication is needed. | Boolean | `false` |
| `validator.mailAuthUsername` | The username to authenticate with. | String | - |
| `validator.mailAuthPassword` | The password to authenticate with. | String | - |
| `validator.mailOutboundHost` | The SMTP server's host to send emails with. | String | - |
| `validator.mailOutboundPort` | The SMTP server's port to send emails with. | Integer | - |
| `validator.mailOutboundSSLEnable` | Whether SSL is needed to connect to the SMTP server. | Boolean | `false` |
| `validator.mailInboundHost` | The server's host name to read emails from. | String | - |
| `validator.mailInboundPort` | The server's port to read emails from. | String | - |
| `validator.mailInboundSSLEnable` | Whether SSL is needed to connect to the inbound service. | Boolean | `false` |
| `validator.mailInboundFolder` | The folder to read emails from. | String | `INBOX` |

To override labels on the web UI you can use the following properties:

| Property | Description | Type | Default value |
| --- | --- | --- | --- |
| `validator.uploadTitle` | Title for the validator web form. | String | `Validator` |
| `validator.reportTitle` | The title for the produced validation report. | String | `Validation report` |
| `validator.label.inputSectionTitle` | Label | String | `Validation input`
| `validator.label.resultSectionTitle` | Label | String | `Validation result`
| `validator.label.fileInputLabel` | Label | String | `File to validate`
| `validator.label.fileInputPlaceholder` | Label | String | `Select file...`
| `validator.label.typeLabel` | Label | String | `Validate as`
| `validator.label.uploadButton` | Label | String | `Upload`
| `validator.label.resultSubSectionOverviewTitle` | Label | String | `Overview`
| `validator.label.resultDateLabel` | Label | String | `Date:`
| `validator.label.resultFileNameLabel` | Label | String | `File name:`
| `validator.label.resultResultLabel` | Label | String | `Result:`
| `validator.label.resultErrorsLabel` | Label | String | `Errors:`
| `validator.label.resultWarningsLabel` | Label | String | `Warnings:`
| `validator.label.resultMessagesLabel` | Label | String | `Messages:`
| `validator.label.viewAnnotatedInputButton` | Label | String | `View annotated input`
| `validator.label.downloadXMLReportButton` | Label | String | `Download XML report`
| `validator.label.downloadPDFReportButton` | Label | String | `Download PDF report`
| `validator.label.resultSubSectionDetailsTitle` | Label | String | `Details`
| `validator.label.resultTestLabel` | Label | String | `Test:`
| `validator.label.popupTitle` | Label | String | `XML content`
| `validator.label.popupCloseButton` | Label | String | `Close`
