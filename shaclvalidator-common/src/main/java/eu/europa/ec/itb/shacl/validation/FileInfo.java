package eu.europa.ec.itb.shacl.validation;

import java.io.File;

public class FileInfo {

    private File file;
    private String contentLang;

    public FileInfo(File file, String contentLang) {
        this.file = file;
        this.contentLang = contentLang;
    }

    public File getFile() {
        return file;
    }

    public String getContentLang() {
        return contentLang;
    }
}
