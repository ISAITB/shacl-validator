package eu.europa.ec.itb.shacl;

import com.gitb.core.AnyContent;
import eu.europa.ec.itb.shacl.validation.FileManager;
import eu.europa.ec.itb.shacl.validation.ValidationConstants;
import eu.europa.ec.itb.validation.commons.BaseInputHelper;
import eu.europa.ec.itb.validation.commons.FileContent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class InputHelper extends BaseInputHelper<FileManager, DomainConfig, ApplicationConfig> {

    @Override
    public void populateFileContentFromInput(FileContent fileContent, AnyContent inputItem) {
        if (StringUtils.equals(inputItem.getName(), ValidationConstants.INPUT_RULE_SYNTAX)) {
            fileContent.setContentType(inputItem.getValue());
        }
    }
}
