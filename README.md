![Banner](https://www.itb.ec.europa.eu/files/banners/itb_banner.png?v=1)

# RDF validator

[![BuildStatus](https://github.com/ISAITB/shacl-validator/actions/workflows/main.yml/badge.svg)](https://github.com/ISAITB/shacl-validator/actions/workflows/main.yml)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_shacl-validator&metric=security_rating)](https://sonarcloud.io/summary/overall?id=ISAITB_shacl-validator)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_shacl-validator&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=ISAITB_shacl-validator)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_shacl-validator&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=ISAITB_shacl-validator)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_shacl-validator&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=ISAITB_shacl-validator)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=ISAITB_shacl-validator&metric=bugs)](https://sonarcloud.io/summary/overall?id=ISAITB_shacl-validator)
[![licence](https://img.shields.io/github/license/ISAITB/shacl-validator.svg?color=blue)](https://github.com/ISAITB/shacl-validator/blob/master/LICENCE.txt)
[![docs](https://img.shields.io/static/v1?label=docs&message=Test%20Bed%20guides&color=blue)](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/)
[![reports](https://img.shields.io/static/v1?label=reports&message=SBOM%20%26%20VDR&color=blue)](https://github.com/ISAITB/release-reports/tree/master/reports/shacl-validator)
[![Gurubase](https://img.shields.io/badge/Gurubase-Ask%20ITB%20Guru-006BFF?color=blue)](https://gurubase.io/g/itb)
[![docker](https://img.shields.io/docker/pulls/isaitb/shacl-validator?color=blue&logo=docker&logoColor=white)](https://hub.docker.com/r/isaitb/shacl-validator)

The **RDF validator** (a.k.a. SHACL validator) is a web application that validates RDF data against [SHACL shapes](https://www.w3.org/TR/shacl/).
Its core is fully reusable: configuration alone determines the supported specifications, the validation types and other
customisations. You can validate content through:

* A **web form**, for validation through the user interface.
* A **REST API**, for machine-to-machine integrations.
* A **SOAP API**, for contract-based machine-to-machine integrations.

The SOAP API conforms to the [GITB validation service API](https://www.itb.ec.europa.eu/docs/services/latest/validation/).
This lets you use the validator in [GITB Test Description Language (TDL)](https://www.itb.ec.europa.eu/docs/tdl/latest/)
conformance test cases, as a [verify step](https://www.itb.ec.europa.eu/docs/tdl/latest/constructs/index.html#verify)
handler. You can also build the validator as a command-line tool (an executable JAR) with pre-packaged or provided configuration.

This validator is maintained by the **European Commission's DIGIT** and specifically the **Interoperability Test Bed**,
a conformance testing service for projects involved in the delivery of cross-border public services. Learn more on the
[Interoperability Test Bed page](https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed).

## Quick start

With [Docker](https://www.docker.com/) installed, run:

```sh
docker run -p 8080:8080 isaitb/shacl-validator
```

Then open <http://localhost:8080/shacl/any/upload>, upload an RDF file and its SHACL shapes, and validate. The generic
`any` domain you see here has no built-in validation artefacts, so you provide your own shapes.

To validate against your own preconfigured shapes, see [Configuration](#configuration).

## How it works

The validator is a generic core that you configure to validate any supported specification. Configuration is organised
in **domains**: logically separate setups served by the same application instance. Each domain defines:

* The **validation types** it offers and their **options**.
* The **validation artefacts** needed for validation, which can be local, remote or user-provided.

A domain's configuration lives in a folder containing a [configuration property file](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/index.html#step-3-prepare-validator-configuration)
and any other resources it needs.

The Test Bed's [RDF validation guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/) documents setup and
configuration as a step-by-step tutorial.

## Running the validator

### Prebuilt packages

If you don't plan to modify the validator's source, use the Test Bed's packages:

* **Docker**: the [isaitb/shacl-validator](https://hub.docker.com/r/isaitb/shacl-validator) image on Docker Hub.
* **JAR file**: the [generic web application package](https://www.itb.ec.europa.eu/shacl-jar/any/validator.zip). This is the same executable JAR you get when building from this repository. See the [installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingValidatorProduction/index.html#approach-1-using-jar-file) for how to run and configure it.
* **Command-line tool**: the [generic command-line package](https://www.itb.ec.europa.eu/shacl-offline/any/validator.zip). This is built from the `shaclvalidator-jar` module, with an extra post-processing step to configure the domain(s).

### Endpoints

Once the web application is running, it is available at:

| Channel  | URL                                                                                                      |
|----------|----------------------------------------------------------------------------------------------------------|
| Web form | `http://localhost:8080/shacl/DOMAIN/upload`                                                              |
| REST API | `http://localhost:8080/shacl/DOMAIN/api/validate` (Swagger docs: `http://localhost:8080/shacl/swagger-ui/index.html`) |
| SOAP API | `http://localhost:8080/shacl/soap/DOMAIN/validation?wsdl`                                                |

`DOMAIN` is the name of a domain's configuration folder under your `validator.resourceRoot`. You can change it by
providing `validator.domainName.DOMAIN` mapping(s). These are [application-level configuration properties](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/index.html#application-level-configuration)
that you can set in the default [application.properties](shaclvalidator-common/src/main/resources/application.properties),
or through environment variables and system properties.

### Configuration

To run the validator you need a few basic properties. Set them as environment variables or system properties (see the
full list of [configuration properties](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/index.html#validator-configuration-properties)):

| Property                 | Purpose                                                          |
|--------------------------|------------------------------------------------------------------|
| `validator.resourceRoot` | The root folder from which all domain configurations are loaded. |
| `logging.file.path`      | The validator's log output folder.                               |
| `validator.tmpFolder`    | The validator's temporary work folder.                           |

Within `validator.resourceRoot`, add a folder for each domain, with its configuration property file and any other
resources. The RDF validation guide's [configuration step](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/index.html#step-3-prepare-validator-configuration)
includes a simple example you can download and reuse.

If you don't load any configuration, the validator falls back to a generic configuration with a single domain called
`any`. It has no validation artefacts, so you upload your own shapes through the UI and APIs. Open it like any other
domain, at <http://localhost:8080/shacl/any/upload>.

### Custom Docker image

To build your own image, use the [sample Dockerfile](etc/docker/shacl-validator/Dockerfile) as a starting point:

1. Create a folder and copy into it the Dockerfile and the JAR produced by the `shaclvalidator-war` module.
2. (Optional) Create a sub-folder (for example `resources`) as your resource root, and place your domain configuration folder(s) in it.
3. (Optional) Adapt the Dockerfile to copy the `resources` folder and set it as the `validator.resourceRoot`:

```dockerfile
COPY resources /validator/resources/
ENV validator.resourceRoot=/validator/resources/
```

If you skip steps 2 and 3, only the generic validator is available, under the `any` domain.

## Building from source

The validator is a multi-module Maven project. The artefact to use is the all-in-one Spring Boot web application
produced by the `shaclvalidator-war` module.

### Prerequisites

* A JDK installation (17+).
* Maven (3+).
* The [itb-commons](https://github.com/ISAITB/itb-commons) libraries, installed in your local Maven repository.

All dependencies are on public repositories except the itb-commons libraries, which are shared by all Test Bed validators
and are not published publicly. Install them first:

```sh
git clone https://github.com/ISAITB/itb-commons.git
cd itb-commons
mvn install -DskipTests=true
```

### Build

```sh
git clone https://github.com/ISAITB/shacl-validator.git
cd shacl-validator
mvn clean install
```

The web application is `shaclvalidator-war/target/validator.jar`. Run it with the [basic properties](#configuration), for example:

```sh
java -Dvalidator.resourceRoot=/path/to/resources \
     -Dlogging.file.path=/path/to/logs \
     -Dvalidator.tmpFolder=/path/to/tmp \
     -jar shaclvalidator-war/target/validator.jar
```

## Plugin development

The RDF validator supports custom plugins that extend the validation report. Plugins are implementations of the
[GITB validation service API](https://www.itb.ec.europa.eu/docs/services/latest/validation/) and must be built as
"all-in-one" JARs.

### Input to plugins

The validator calls plugins in sequence, passing the following input:

| Input name          | Type     | Description                                                                                                                             |
|---------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `contentToValidate` | `String` | The absolute and full path to the input provided to the validator. This is stored in the file system as an RDF/XML file.                |
| `domain`            | `String` | The validation domain relevant to the specific validation call.                                                                         |
| `validationType`    | `String` | The validation type of the domain that is selected for the specific validation call.                                                    |
| `tempFolder`        | `String` | The absolute and full path to a temporary folder for plugins. This will be automatically deleted after all plugins complete validation. |
| `locale`            | `String` | The locale (language code) to use for reporting of results (e.g. "fr", "fr_FR").                                                        |

### Output from plugins

A plugin's output is a GITB `ValidationResponse` that wraps a `TAR` instance. The report items in this `TAR` are merged
with the reports produced by SHACL validation.

### Plugin configuration

Plugin configuration is part of a validator's [domain configuration](https://www.itb.ec.europa.eu/docs/guides/latest/validatingRDF/index.html#domain-level-configuration).
Once your plugins are implemented, configure them with the `validator.defaultPlugins` and `validator.plugins` properties,
listing for each plugin:

* The path to its JAR file.
* The fully qualified class name of its entry point.

## Security

To report a security vulnerability, please follow this repository's [security policy](SECURITY.md).

To support transparency and the security monitoring needs of downstream users, signed **Software Bill of Materials (SBOM)**
and **Vulnerability Disclosure Report (VDR)** documents in [CycloneDX](https://cyclonedx.org/) format are published for every
release of the RDF validator in the [release-reports](https://github.com/ISAITB/release-reports) repository:

* The [reports of the RDF validator](https://github.com/ISAITB/release-reports/tree/master/reports/shacl-validator).
* An [overview of the vulnerability status](https://github.com/ISAITB/release-reports/blob/master/VULNERABILITY_STATUS.md) of all releases.
* A mirror of the reports for automated integrations, with a machine-readable index, at <https://www.itb.ec.europa.eu/release-reports/reports/index.json>.

## Contributing

The best way to contribute is to share your feedback, questions and suggestions by [opening an issue](https://github.com/ISAITB/shacl-validator/issues)
in this repository. For more elaborate contributions, and for more information, see [CONTRIBUTING.md](CONTRIBUTING.md).

## Licence

This software is shared using the [European Union Public Licence (EUPL) version 1.2](https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12).

Third-party library licences and attributions are listed in [NOTICE.md](NOTICE.md).

## Legal notice

The authors of this software waive any and all liability linked to its usage or the interpretation of results it produces.

## Contact

For feedback or questions about this validator, please post an issue in this repository. You can also contact the Test Bed
team by email at [DIGIT-ITB@ec.europa.eu](mailto:DIGIT-ITB@ec.europa.eu).

## See also

The Test Bed provides similar validators for other content types. Check these out for more information:

* The **XML validator**: see [overview](https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/xml-validator), [source](https://github.com/ISAITB/xml-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingXML/) and [Docker Hub image](https://hub.docker.com/r/isaitb/xml-validator).
* The **JSON validator**: see [overview](https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/json-validator), [source](https://github.com/ISAITB/json-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingJSON/) and [Docker Hub image](https://hub.docker.com/r/isaitb/json-validator).
* The **CSV validator**: see [overview](https://interoperable-europe.ec.europa.eu/collection/interoperability-test-bed-repository/solution/csvvalidator), [source](https://github.com/ISAITB/csv-validator), [detailed guide](https://www.itb.ec.europa.eu/docs/guides/latest/validatingCSV/) and [Docker Hub image](https://hub.docker.com/r/isaitb/csv-validator).
