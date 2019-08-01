package eu.europa.ec.itb.shacl.standalone;

import java.io.File;

/**
 * Created by simatosc on 12/08/2016.
 */
public class ValidationInput {

    private File inputFile;
    private String validationType;
    private String filename;

    public ValidationInput(File inputFile, String validationType, String filename) {
        this.inputFile = inputFile;
        this.validationType = validationType;
        this.filename = filename;
    }

    public File getInputFile() {
        return inputFile;
    }

    public String getValidationType() {
        return validationType;
    }
    
    public String getFilename() {
    	return filename;
    }
}
