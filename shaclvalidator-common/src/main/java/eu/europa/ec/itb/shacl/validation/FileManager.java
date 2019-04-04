package eu.europa.ec.itb.shacl.validation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class FileManager {
    /**
     * From a URL, it gets the File
     * @param URLConvert URL as String
     * @return File
     * @throws IOException 
     */
    public static File getURLFile(String URLConvert, String tmpFolder) throws IOException {		
		URL url = new URL(URLConvert);
		String extension = FilenameUtils.getExtension(url.getFile());
		
		if(extension!=null) {				
			extension = "." + extension;
		}
		
		Path tmpPath = getTmpPath(extension, tmpFolder);
		
		try(InputStream in = new URL(URLConvert).openStream()){
			Files.copy(in, tmpPath, StandardCopyOption.REPLACE_EXISTING);
		}
		
		return tmpPath.toFile();
    }
    
    /**
     * Get a temporary path and create the directory
     * @param extension Extension of the file (optional)
     * @return Path
     */
    public static Path getTmpPath(String extension, String tmpFolder) {
		Path tmpPath = Paths.get(tmpFolder, UUID.randomUUID().toString() + extension);
		tmpPath.toFile().getParentFile().mkdirs();
		
		return tmpPath;
    }
    
    /**
     * Remove the validated file
     * @param File to remove
     */
    public static void removeContentToValidate(File inputFile) {
    	if (inputFile != null && inputFile.exists() && inputFile.isFile()) {
            FileUtils.deleteQuietly(inputFile);
        }
    }
}
