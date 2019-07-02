package eu.europa.ec.itb.shacl.upload;

/**
 * Created by simatosc on 09/08/2016.
 */
public class UploadTypes {

    private String key;
    private String label;

    public UploadTypes(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }
}
