![Banner](https://www.itb.ec.europa.eu/files/banners/itb_banner.png)

# SHACL validator

![BuildStatus](https://github.com/ISAITB/shacl-validator/actions/workflows/main.yml/badge.svg)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_shacl-validator&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=ISAITB_shacl-validator)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_shacl-validator&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=ISAITB_shacl-validator)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_shacl-validator&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=ISAITB_shacl-validator)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_shacl-validator&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=ISAITB_shacl-validator)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_shacl-validator&metric=bugs)](https://sonarcloud.io/summary/new_code?id=ISAITB_shacl-validator)
[![licence](https://img.shields.io/github/license/ISAITB/shacl-validator.svg?color=blue)](https://github.com/ISAITB/shacl-validator/blob/master/LICENCE.txt)
[![docs](https://img.shields.io/static/v1?label=docs&message=Test%20Bed%20guides&color=blue)](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/)
[![Gurubase](https://img.shields.io/badge/Gurubase-Ask%20ITB%20Guru-006BFF?color=blue)](https://gurubase.io/g/itb)
[![docker](https://img.shields.io/docker/pulls/isaitb/shacl-validator?color=blue&logo=docker&logoColor=white)](https://hub.docker.com/r/isaitb/shacl-validator)

The **SHACL validator** is a web application to validate RDF data against [SHACL shapes](https://www.w3.org/TR/shacl/).
The application provides a fully reusable core that requires only configuration to determine the supported specifications,
configured validation types and other validator customisations. The web application allows validation via:

* A SOAP web service API for contract-based machine-machine integrations.
* A REST web service API for machine-machine integrations.
* A web form for validation via user interface.

The SOAP web service API conforms to the [GITB validation service API](https://www.itb.ec.europa.eu/docs/services/latest/validation/)
which makes it usable as a building block in [GITB Test Description Language (TDL)](https://www.itb.ec.europa.eu/docs/tdl/latest/)
conformance test cases for the verification of content (as a [verify step](https://www.itb.ec.europa.eu/docs/tdl/latest/constructs/index.html#verify)
handler). Note additionally that the validator can also be used to build a command-line tool as an executable JAR with
pre-packaged or provided configuration.

This validator is maintained by the **European Commission's DIGIT** and specifically the **Interoperability Test Bed**,
a conformance testing service for projects involved in the delivery of cross-border public services. Find out more
[here](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed).

# Usage

Usage and configuration of this validator is documented as a step-by-step tutorial in the Test Bed's
[RDF validation guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/).

The validator's key principle is that the software is built as a generic core that can be configured to validate any
supported specifications. Configuration is organised in **domains** which represent logically separate setups supported
by the same application instance. Each such domain defines the offered **validation types** and their **options** along
with the validation artefacts needed to carry out validation (local, remote or user-provided). A domain's configuration
is grouped in a folder that contains a [configuration property file](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/index.html#step-3-prepare-validator-configuration)
along with any other necessary resources.

When built from source, the simplest way to get started using the validator is to use the **all-in-one executable JAR**
built from the `shaclvalidator-war` module. You can use and configure this as described in the
[validator installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingValidatorProduction/index.html#approach-1-using-jar-file).

If you do not plan on modifying the validator's source you can reuse the Test Bed's provided packages. Specifically:

* Via **Docker**, using the [isaitb/shacl-validator](https://hub.docker.com/r/isaitb/shacl-validator) image from the Docker Hub.
* Via **JAR file**, using the [generic web application package](https://www.itb.ec.europa.eu/shacl-jar/any/validator.zip).
* Via **command line tool**, using the [generic command line tool package](https://www.itb.ec.europa.eu/shacl-offline/any/validator.zip).

It is interesting to note that the second option (executable web application JAR) matches what you would build from this
repository. The command line package is produced from the `shaclvalidator-jar` although this requires an additional step
of JAR post-processing to configure the validator's domain(s).

Once the validator's web application is up you can use it as follows:

* SOAP API: http://localhost:8080/shacl/soap/DOMAIN/validation?wsdl
* REST API: http://localhost:8080/shacl/DOMAIN/api/validate (Swagger docs at http://localhost:8080/shacl/swagger-ui/index.html)
* Web form: http://localhost:8080/shacl/DOMAIN/upload

Note that the `DOMAIN` placeholder in the above URLs is the name of a domain configuration folder beneath your configured `validator.resourceRoot`.
This can be adapted by providing `validator.domainName.DOMAIN` mapping(s) for your domain(s). These are [application-level configuration properties](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/index.html#application-level-configuration)
that can be set in the default [application.properties](shaclvalidator-common/src/main/resources/application.properties)
or via environment variables and system properties.

# Building

The validator is a multi-module Maven project from which the artefact to use is the web application package, produced
from module `shaclvalidator-war`. This is an all-in-one Spring Boot web application. To build issue `mvn clean install`.

## Prerequisites

To build this project's libraries you require:
* A JDK installation (17+).
* Maven (3+)
* Locally installed [itb-commons](https://github.com/ISAITB/itb-commons) dependencies (see below).

Building this validator from source depends on libraries that are available on public repositories. The exception is
currently the set of [itb-commons](https://github.com/ISAITB/itb-commons) dependencies, common libraries that are shared
by all Test Bed validators. To be able to build you need to first clone [itb-commons](https://github.com/ISAITB/itb-commons)
and install its artefacts in your local Maven repository.

## Configuration for development

When building for development you should provide the validator's basic configuration to allow it to bootstrap.
The simplest approach to do this is to use environment variables that set the validator's
[configuration properties](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/index.html#validator-configuration-properties).

The minimum properties you should define this way are:

* `validator.resourceRoot`: The root folder from which all domain configurations will be loaded.
* `logging.file.path`: The validator’s log output folder.
* `validator.tmpFolder`: The validator’s temporary work folder.

In addition, you should include within the `validator.resourceRoot` folder additional folder(s) for your configuration domains,
each with its configuration property file and any other needed resources. A simple example of such configuration that you
can also download and reuse, is provided in the RDF validation guide's [configuration step](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/index.html#step-3-prepare-validator-configuration).

If you decide not to load any configurations, the generic SHACL validator configuration will be loaded instead.
This generic configuration does not contain any validation artefacts and will instead allow you to upload your own validation schemas from the validator's UI and APIs.
The domain of this generic validator is called **any**. you can access it like any other domain, from http://localhost:8080/shacl/any/upload.

## Using Docker

If you choose Docker to run your validator you can use the [sample Dockerfile](etc/docker/shacl-validator/Dockerfile)
as a starting point. To use this:
1. Create a folder and copy within it the Dockerfile and JAR produced from the `shaclvalidator-war` module.
2. (Optional) Create a sub-folder (e.g. `resources`) as your resource root within which you place your domain configuration folder(s).
3. (Optional) Adapt the Dockerfile to also copy the `resources` folder and set its path within the image as the `validator.resourceRoot`:

```
...
COPY resources /validator/resources/
ENV validator.resourceRoot /validator/resources/
...
```  

Note: If you skip step 2 and 3, only the generic validator will be available under the `any` domain.

# Plugin development

The SHACL validator supports custom plugins to extend the validation report. Plugins are implementations of the
[GITB validation service API](https://www.itb.ec.europa.eu/docs/services/latest/validation/) for which the following
applies. Note that plugin JAR files need to be built as "all-in-one" JARs.

## Input to plugins

The SHACL validator calls plugins in sequence passing in the following input:

| Input name          | Type     | Description                                                                                                                             |
|---------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `contentToValidate` | `String` | The absolute and full path to the input provided to the validator. This is stored in the file system as an RDF/XML file.                |
| `domain`            | `String` | The validation domain relevant to the specific validation call.                                                                         |
| `validationType`    | `String` | The validation type of the domain that is selected for the specific validation call.                                                    |
| `tempFolder`        | `String` | The absolute and full path to a temporary folder for plugins. This will be automatically deleted after all plugins complete validation. |
| `locale`            | `String` | The locale (language code) to use for reporting of results (e.g. "fr", "fr_FR").                                                        |

## Output from plugins

The output of plugins is essentially a GITB `ValidationResponse` that wraps a `TAR` instance. The report items within this `TAR` instance are merged
with any reports produced by SHACL validation.

## Plugin configuration

Plugin configuration for a validator instance is part of its [domain configuration](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/index.html#domain-level-configuration).
Once you have your plugins implemented you can configure them using the `validator.defaultPlugins`
and `validator.plugins` properties where you list each plugin by providing:

* The path to its JAR file.
* The fully qualified class name of the plugin entry point.

# Licence

This software is shared using the [European Union Public Licence (EUPL) version 1.2](https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12).

Third-party library licences and attributions are listed in [NOTICE.md](NOTICE.md).

# Legal notice

The authors of this library waive any and all liability linked to its usage or the interpretation of results produced
by its downstream validators.

# Contact

For feedback or questions regarding this library you are invited to post issues in the current repository. In addition,
feel free to contact the Test Bed team via email at [DIGIT-ITB@ec.europa.eu](mailto:DIGIT-ITB@ec.europa.eu).

# See also

The Test Bed provides similar validators for other content types. Check these out for more information:
* The **XML validator**: see [overview](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/xml-validator), [source](https://github.com/ISAITB/xml-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingXML/) and [Docker Hub image](https://hub.docker.com/r/isaitb/xml-validator).
* The **JSON validator**: see [overview](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/json-validator), [source](https://github.com/ISAITB/json-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingJSON/) and [Docker Hub image](https://hub.docker.com/r/isaitb/json-validator).
* The **CSV validator**: see [overview](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/csv-validator), [source](https://github.com/ISAITB/csv-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingCSV/) and [Docker Hub image](https://hub.docker.com/r/isaitb/csv-validator).