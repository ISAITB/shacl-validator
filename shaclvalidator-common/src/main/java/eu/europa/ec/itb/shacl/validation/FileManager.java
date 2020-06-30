package eu.europa.ec.itb.shacl.validation;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.BaseFileManager;
import eu.europa.ec.itb.validation.commons.FileInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.Writer;
import java.util.List;

@Component
public class FileManager extends BaseFileManager<ApplicationConfig> {

	private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

	@Override
	public String getFileExtension(String contentType) {
		String extension = null;
		Lang language = RDFLanguages.contentTypeToLang(contentType);
		if (language != null) {
			extension = language.getFileExtensions().get(0);
		}
		return extension;
	}

	@Override
	protected String getContentTypeForFile(File file, String declaredContentType) {
		if (StringUtils.isBlank(declaredContentType)) {
			ContentType detectedContentType = RDFLanguages.guessContentType(file.getName());
			if (detectedContentType != null) {
				declaredContentType = detectedContentType.getContentType();
			}
		}
		return declaredContentType;
	}

	public void writeRdfModel(Writer outputWriter, Model rdfModel, String outputSyntax) {
		Lang lang = RDFLanguages.contentTypeToLang(outputSyntax);
		if (lang == null) {
			rdfModel.write(outputWriter, null);
		} else {
			try {
				rdfModel.write(outputWriter, lang.getName());
			} catch(Exception e) {
				logger.error("Error writing RDF model", e);
				throw new IllegalStateException("Error writing RDF model", e);
			}
		}
	}

	/**
	 * Remove the external files linked to the validation.
	 */
	public void removeContentToValidate(File inputFile, List<FileInfo> externalShaclFiles) {
		// TODO see if this is needed.
		// Remove content that was validated.
		if (inputFile != null && inputFile.exists() && inputFile.isFile()) {
			FileUtils.deleteQuietly(inputFile);
		}
		// Remove externally provided SHACL files.
		if (externalShaclFiles != null) {
			for (FileInfo externalFile: externalShaclFiles) {
				FileUtils.deleteQuietly(externalFile.getFile());
			}
		}
	}

	public File getHydraDocsRootFolder() {
		return new File(config.getTmpFolder(), "hydra");
	}

	public File getHydraDocsFolder(String domainName) {
		return new File(getHydraDocsRootFolder(), domainName);
	}

}
