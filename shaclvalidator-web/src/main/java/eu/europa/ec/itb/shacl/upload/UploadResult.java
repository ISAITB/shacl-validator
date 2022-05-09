package eu.europa.ec.itb.shacl.upload;

import eu.europa.ec.itb.validation.commons.web.KeyWithLabel;

import java.util.List;

/**
 * Class used to record the values for the result of a validation (through the UI). This is serialised to JSON as part of
 * the response.
 */
public class UploadResult extends eu.europa.ec.itb.validation.commons.web.dto.UploadResult<Translations> {

    private List<KeyWithLabel> contentSyntax;

    /**
     * @param contentSyntax The supported RDF content syntax values.
     */
    public void setContentSyntax(List<KeyWithLabel> contentSyntax) {
        this.contentSyntax = contentSyntax;
    }

    /**
     * @return The supported RDF content syntax values.
     */
    public List<KeyWithLabel> getContentSyntax() {
        return contentSyntax;
    }

}
