package eu.europa.ec.itb.shacl.validation;

import eu.europa.ec.itb.shacl.ApplicationConfig;
import eu.europa.ec.itb.shacl.DomainConfig;
import eu.europa.ec.itb.shacl.DomainConfigCache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class FileManager {

	private static final Logger logger = LoggerFactory.getLogger(FileManager.class);

	@Autowired
	private ApplicationConfig config;

	@Autowired
	private DomainConfigCache domainConfigCache;

	private ConcurrentHashMap<String, ReadWriteLock> externalDomainFileCacheLocks = new ConcurrentHashMap<>();

	private File getURLFile(File targetFolder, String URLConvert) throws IOException {
		return getURLFile(targetFolder.getAbsolutePath(), URLConvert);
	}

	private File getURLFile(String targetFolder, String URLConvert) throws IOException {
		URL url = new URL(URLConvert);
		String extension = FilenameUtils.getExtension(url.getFile());

		if(extension!=null) {
			extension = "." + extension;
		}

		Path tmpPath = getFilePath(targetFolder, extension);

		try(InputStream in = new URL(URLConvert).openStream()){
			Files.copy(in, tmpPath, StandardCopyOption.REPLACE_EXISTING);
		}

		return tmpPath.toFile();
	}

	private File getStringFile(String targetFolder, String stringConvert, String contentSyntax) throws IOException {
		Lang langExtension = RDFLanguages.contentTypeToLang(contentSyntax);
		String extension = null;

		if(langExtension!=null) {
			extension = "." + langExtension.getName();
		}

		Path tmpPath = getFilePath(targetFolder, extension);

		try(InputStream in = new ByteArrayInputStream(stringConvert.getBytes())){
			Files.copy(in, tmpPath, StandardCopyOption.REPLACE_EXISTING);
		}

		return tmpPath.toFile();
	}

    /**
     * From a URL, it gets the File
     * @return File
     * @throws IOException 
     */
    public File getURLFile(String url) throws IOException {
    	return getURLFile(config.getTmpFolder(), url);
    }
    
    public File getStringFile(String contentToValidate, String contentSyntax) throws IOException {
    	return getStringFile(config.getTmpFolder(), contentToValidate, contentSyntax);
    }
    public File getStringFile(File targetFolder, String contentToValidate, String contentSyntax) throws IOException {
    	return getStringFile(targetFolder.getAbsolutePath(), contentToValidate, contentSyntax);
    }
    
    /**
     * Get a temporary path and create the directory
     * @param extension Extension of the file (optional)
     * @return Path
     */
    public Path getTmpPath(String extension) {
    	return getFilePath(config.getTmpFolder(), extension);
    }

	private Path getFilePath(String folder, String extension) {
		Path tmpPath = Paths.get(folder, UUID.randomUUID().toString() + extension);
		tmpPath.toFile().getParentFile().mkdirs();

		return tmpPath;
	}

    /**
     * Remove the validated file
     */
    public void removeContentToValidate(File inputFile, List<FileInfo> externalShaclFiles) {
    	// Remove content that was validated.
    	if (inputFile != null && inputFile.exists() && inputFile.isFile()) {
            FileUtils.deleteQuietly(inputFile);
        }
    	// Remove externally provided SHACL files.
		if (externalShaclFiles != null && !externalShaclFiles.isEmpty()) {
			// All such files are stored under a single root folder (per validation run).
			FileUtils.deleteQuietly(externalShaclFiles.get(0).getFile().getParentFile());
		}
    }

    private File getTempFolder() {
    	return new File(config.getTmpFolder());
	}

	public File getHydraDocsRootFolder() {
		return new File(config.getTmpFolder(), "hydra");
	}

	public File getHydraDocsFolder(String domainName) {
		return new File(getHydraDocsRootFolder(), domainName);
	}

    private File getRemoteFileCacheFolder() {
    	return new File(getTempFolder(), "remote_config");
	}

	private File getBase64File(File targetFolder, String base64Convert) {
		if (targetFolder == null) {
			targetFolder = getTempFolder();
		}
		File tempFile;
		try {
			tempFile = getFilePath(targetFolder.getAbsolutePath(), "").toFile();
			// Construct the string from its BASE64 encoded bytes.
			byte[] decodedBytes = Base64.getDecoder().decode(base64Convert);
			FileUtils.writeByteArrayToFile(tempFile, decodedBytes);
		} catch (IOException e) {
			throw new IllegalStateException("Error when transforming the Base64 into File.", e);
		}
		return tempFile;
	}

	/**
	 * From Base64 string to File
	 * @param base64Convert Base64 as String
	 * @return File
	 */
	public File getBase64File(String base64Convert) {
		return getBase64File(null, base64Convert);
	}

	private File getFileAsUrlOrBase64(File targetFolder, String content) {
		if (targetFolder == null) {
			targetFolder = getTempFolder();
		}
		File outputFile;
		try {
			outputFile = getURLFile(targetFolder, content);
		}catch(Exception e) {
			outputFile = getBase64File(targetFolder, content);
		}
		return outputFile;
	}

	/**
	 * Get the content from URL or BASE64
	 * @param content URL or BASE64 as String
	 * @return File
	 */
	public File getFileAsUrlOrBase64(String content) {
		return getFileAsUrlOrBase64(null, content);
	}

	private List<FileInfo> getRemoteShaclFiles(DomainConfig domainConfig, String validationType) {
		File remoteConfigFolder = new File(new File(getRemoteFileCacheFolder(), domainConfig.getDomainName()), validationType);
		if (remoteConfigFolder.exists()) {
			return getLocalShaclFiles(remoteConfigFolder);
		} else {
			return Collections.emptyList();
		}
	}

	private File getExternalShapeFolder() {
		return new File(getTempFolder(), "external_config");
	}

	public List<FileInfo> getRemoteExternalShapes(List<FileContent> externalRules) {
		List<FileInfo> externalShapeFiles = new ArrayList<>();
    	if (externalRules != null && !externalRules.isEmpty()) {
    		File externalShapeFolder = new File(getExternalShapeFolder(), UUID.randomUUID().toString());
			for (FileContent externalRule: externalRules) {
				File contentFile = null;
				if (externalRule.getEmbeddingMethod() != null) {
					switch(externalRule.getEmbeddingMethod()) {
						case FileContent.embedding_URL:
							try{
								contentFile = getURLFile(externalShapeFolder, externalRule.getContent());
							} catch(IOException e) {
								throw new IllegalArgumentException("Error when transforming the URL into File.", e);
							}
							break;
						case FileContent.embedding_BASE64:
							contentFile = getBase64File(externalShapeFolder, externalRule.getContent());
							break;
		    			case FileContent.embedding_STRING:
		    				try{
		        				contentFile = getStringFile(externalShapeFolder, externalRule.getContent(), externalRule.getSyntax());
		    				}catch(IOException e) {
								throw new IllegalArgumentException("Error when transforming the STRING into File.", e);
							}
		    				break;
						default:
							throw new IllegalArgumentException("Unexpected embedding method ["+externalRule.getEmbeddingMethod()+"]");
					}
				} else {
					contentFile = getFileAsUrlOrBase64(externalShapeFolder, externalRule.getContent());
				}
				externalShapeFiles.add(new FileInfo(contentFile, getContentLang(contentFile, externalRule.getSyntax())));
			}
		}
		return externalShapeFiles;
	}

	public List<FileInfo> getAllShaclFiles(DomainConfig domainConfig, String validationType, List<FileInfo> filesInfo){
		List<FileInfo> shaclFiles = new ArrayList<>();
		shaclFiles.addAll(getLocalShaclFiles(getShaclFile(domainConfig, validationType)));
		shaclFiles.addAll(getRemoteShaclFiles(domainConfig, validationType));
		shaclFiles.addAll(filesInfo);
		return shaclFiles;
	}

	/**
	 * Return the SHACL files loaded for a given validation type
	 * @return File
	 */
	private File getShaclFile(DomainConfig domainConfig, String validationType) {
		String localFolder = domainConfig.getShaclFile().get(validationType).getLocalFolder();
		File f = null;
		if (StringUtils.isNotEmpty(localFolder)) {
			f = Paths.get(config.getResourceRoot(), domainConfig.getDomain(), domainConfig.getShaclFile().get(validationType).getLocalFolder()).toFile();
		}
		return f;
	}

	/**
	 * Returns the list of files (if it is a directory) or the file and the corresponding content type.
	 * @return Map<File, String> with all files and content types
	 */
	private List<FileInfo> getLocalShaclFiles(File shaclFileOrFolder){
		List<FileInfo> fileInfo = new ArrayList<>();
		if (shaclFileOrFolder != null && shaclFileOrFolder.exists()) {
			if (shaclFileOrFolder.isFile()) {
				// We are pointing to a single master SHACL file.
				fileInfo.add(new FileInfo(shaclFileOrFolder, getContentLang(shaclFileOrFolder)));
			} else {
				// All SHACL are processed.
				File[] files = shaclFileOrFolder.listFiles();
				if (files != null) {
					for (File aShaclFile: files) {
						if (aShaclFile.isFile()) {
							fileInfo.add(new FileInfo(aShaclFile, getContentLang(aShaclFile)));
						}
					}
				}
			}
		}

		return fileInfo;
	}

	private String getContentLang(File file) {
		return getContentLang(file, null);
	}

	/**
	 * Return the content type as Lang of the File
	 * @return String content type as String
	 */
	private String getContentLang(File file, String contentSyntax) {
		String stringLang = null;
		Lang contentLang;
		if (StringUtils.isEmpty(contentSyntax)) {
			contentLang = RDFLanguages.contentTypeToLang(RDFLanguages.guessContentType(file.getName()));
		} else {
			contentLang = RDFLanguages.contentTypeToLang(contentSyntax);
		}
		
		if(contentLang != null) {
			stringLang = contentLang.getName();
		}
		return stringLang;
	}

	@PostConstruct
	public void init() {
		FileUtils.deleteQuietly(getTempFolder());
		for (DomainConfig config: domainConfigCache.getAllDomainConfigurations()) {
			externalDomainFileCacheLocks.put(config.getDomainName(), new ReentrantReadWriteLock());
		}
		FileUtils.deleteQuietly(getRemoteFileCacheFolder());
		resetRemoteFileCache();
	}

	@Scheduled(fixedDelayString = "${validator.cleanupRate}")
	public void resetRemoteFileCache() {
		logger.debug("Resetting remote SHACL file cache");
		for (DomainConfig domainConfig: domainConfigCache.getAllDomainConfigurations()) {
			try {
				// Get write lock for domain.
				logger.debug("Waiting for lock to reset cache for ["+domainConfig.getDomainName()+"]");
				externalDomainFileCacheLocks.get(domainConfig.getDomainName()).writeLock().lock();
				logger.debug("Locked cache for ["+domainConfig.getDomainName()+"]");
				for (String validationType: domainConfig.getType()) {
					// Empty cache folder.
					File remoteConfigFolder = new File(new File(getRemoteFileCacheFolder(), domainConfig.getDomainName()), validationType);
					FileUtils.deleteQuietly(remoteConfigFolder);
					remoteConfigFolder.mkdirs();
					// Download remote SHACL files (if needed).
					List<DomainConfig.RemoteInfo> ri = domainConfig.getShaclFile().get(validationType).getRemote();
					if (ri != null) {
						try {
							for (DomainConfig.RemoteInfo info: ri) {
								getURLFile(remoteConfigFolder.getAbsolutePath(), info.getUrl());
							}
						} catch (IOException e) {
							logger.error("Error to load the remote SHACL file", e);
							throw new IllegalStateException("Error to load the remote SHACL file", e);
						}
					}
				}
			} finally {
				// Unlock domain.
				externalDomainFileCacheLocks.get(domainConfig.getDomainName()).writeLock().unlock();
				logger.debug("Reset remote SHACL file cache for ["+domainConfig.getDomainName()+"]");
			}
		}
	}

	public void signalValidationStart(String domainName) {
		logger.debug("Signalling validation start for ["+domainName+"]");
		externalDomainFileCacheLocks.get(domainName).readLock().lock();
		logger.debug("Signalled validation start for ["+domainName+"]");
	}

	public void signalValidationEnd(String domainName) {
		logger.debug("Signalling validation end for ["+domainName+"]");
		externalDomainFileCacheLocks.get(domainName).readLock().unlock();
		logger.debug("Signalled validation end for ["+domainName+"]");
	}

}