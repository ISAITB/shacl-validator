package eu.europa.ec.itb.shacl;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.config.WebDomainConfigCache;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class DomainConfigCache extends WebDomainConfigCache<DomainConfig> {

    @Autowired
    private ApplicationConfig appConfig = null;

    @PostConstruct
    public void init() {
        super.init();
    }

    @Override
    protected DomainConfig newDomainConfig() {
        return new DomainConfig();
    }

    @Override
    protected ValidatorChannel[] getSupportedChannels() {
        return new ValidatorChannel[] {ValidatorChannel.FORM, ValidatorChannel.SOAP_API, ValidatorChannel.REST_API};
    }

    @Override
    protected void addDomainConfiguration(DomainConfig domainConfig, Configuration config) {
        super.addDomainConfiguration(domainConfig, config);
        addValidationArtifactInfo("validator.shaclFile", "validator.externalShapes", null, domainConfig, config);
        domainConfig.setUploadTitle(config.getString("validator.uploadTitle", "SHACL Validator"));
        domainConfig.setDefaultReportSyntax(config.getString("validator.defaultReportSyntax", appConfig.getDefaultReportSyntax()));
        domainConfig.setWebContentSyntax(Arrays.stream(StringUtils.split(config.getString("validator.contentSyntax", ""), ',')).map(String::trim).collect(Collectors.toList()));
        domainConfig.setReportsOrdered(config.getBoolean("validator.reportsOrdered", false));
        domainConfig.setMergeModelsBeforeValidation(config.getBoolean("validator.mergeModelsBeforeValidation", true));
        // Labels
        domainConfig.setDefaultLoadImportsType(parseBooleanMap("validator.loadImports", config, domainConfig.getType(), config.getBoolean("validator.loadImports", false)));
        domainConfig.setUserInputForLoadImportsType(parseEnumMap("validator.input.loadImports", ExternalArtifactSupport.byName(config.getString("validator.input.loadImports", ExternalArtifactSupport.NONE.getName())), config, domainConfig.getType(), ExternalArtifactSupport::byName));
        domainConfig.getLabel().setContentSyntaxLabel(config.getString("validator.label.contentSyntaxLabel", "Content syntax"));
        domainConfig.getLabel().setExternalShapesLabel(config.getString("validator.label.externalShapesLabel", "External shapes"));
        domainConfig.getLabel().setResultLocationLabel(config.getString("validator.label.resultLocationLabel", "Location:"));
        domainConfig.getLabel().setOptionDownloadReport(config.getString("validator.label.optionDownloadReport", "Validation report"));
        domainConfig.getLabel().setOptionDownloadContent(config.getString("validator.label.optionDownloadContent", "Validated content"));
        domainConfig.getLabel().setOptionDownloadShapes(config.getString("validator.label.optionDownloadShapes", "Shapes"));
        domainConfig.getLabel().setContentSyntaxTooltip(config.getString("validator.label.contentSyntaxTooltip", "Optional for content provided as a file or a URI if a known file extension is detected"));
        domainConfig.getLabel().setExternalRulesTooltip(config.getString("validator.label.externalRulesTooltip", "Additional shapes that will be considered for the validation"));
        domainConfig.getLabel().setSaveDownload(config.getString("validator.label.saveDownload", "Download"));
        domainConfig.getLabel().setSaveAs(config.getString("validator.label.saveAs", "as"));
        domainConfig.getLabel().setIncludeExternalShapes(config.getString("validator.label.includeExternalShapes", "Include external shapes"));
        domainConfig.getLabel().setReportItemFocusNode(config.getString("validator.label.reportItemFocusNode", "Focus node"));
        domainConfig.getLabel().setReportItemResultPath(config.getString("validator.label.reportItemResultPath", "Result path"));
        domainConfig.getLabel().setReportItemShape(config.getString("validator.label.reportItemShape", "Shape"));
        domainConfig.getLabel().setReportItemValue(config.getString("validator.label.reportItemValue", "Value"));
        domainConfig.getLabel().setLoadImportsLabel(config.getString("validator.label.loadImports", "Load imports defined in the input?"));
        addMissingDefaultValues(domainConfig.getWebServiceDescription(), appConfig.getDefaultLabels());
    }

}
