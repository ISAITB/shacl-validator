package eu.europa.ec.itb.shacl;

import eu.europa.ec.itb.shacl.DomainConfig.RemoteInfo;
import eu.europa.ec.itb.shacl.DomainConfig.ShaclFileInfo;
import eu.europa.ec.itb.shacl.validation.ValidationConstants;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class DomainConfigCache {

    private static Logger logger = LoggerFactory.getLogger(DomainConfigCache.class);

    @Autowired
    private ApplicationConfig appConfig;
    private ConcurrentHashMap<String, DomainConfig> domainConfigs = new ConcurrentHashMap<>();
    private DomainConfig undefinedDomainConfig = new DomainConfig(false);

    private ExtensionFilter propertyFilter = new ExtensionFilter(".properties");

    @PostConstruct
    public void init() {
        getAllDomainConfigurations();
    }

    public DomainConfig[] getAllDomainConfigurations() {
        List<DomainConfig> configs = new ArrayList<>();
        for (String domain: appConfig.getDomain()) {
            DomainConfig domainConfig = getConfigForDomain(domain);
            if (domainConfig != null && domainConfig.isDefined()) {
                configs.add(domainConfig);
            }
        }
        return configs.toArray(new DomainConfig[0]);
    }

    public DomainConfig getConfigForDomainName(String domainName) {
        DomainConfig config = getConfigForDomain(appConfig.getDomainNameToDomainId().getOrDefault(domainName, ""));
        if (config == null) {
            logger.warn("Invalid domain name ["+domainName+"].");
        }
        return config;
    }

    public DomainConfig getConfigForDomain(String domain) {
        DomainConfig domainConfig = domainConfigs.get(domain);
        if (domainConfig == null) {
            String[] files = Paths.get(appConfig.getResourceRoot(), domain).toFile().list(propertyFilter);
            if (files == null || files.length == 0) {
                domainConfig = undefinedDomainConfig;
            } else {
                CompositeConfiguration config = new CompositeConfiguration();
                for (String file: files) {
                    Parameters params = new Parameters();
                    FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                            new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                                    .configure(params.properties().setFile(Paths.get(appConfig.getResourceRoot(), domain, file).toFile()));
                    try {
                        config.addConfiguration(builder.getConfiguration());
                    } catch (ConfigurationException e) {
                        throw new IllegalStateException("Unable to load property file ["+file+"]", e);
                    }
                }
                domainConfig = new DomainConfig();
                domainConfig.setDomain(domain);
                domainConfig.setUploadTitle(config.getString("validator.uploadTitle", "SHACL Validator"));
                domainConfig.setReportTitle(config.getString("validator.reportTitle", "Validation report"));
                domainConfig.setDomainName(appConfig.getDomainIdToDomainName().get(domain));
                domainConfig.setType(Arrays.stream(StringUtils.split(config.getString("validator.type"), ',')).map(String::trim).collect(Collectors.toList()));
                domainConfig.setTypeLabel(parseMap("validator.typeLabel", config, domainConfig.getType()));
                domainConfig.setChannels(Arrays.stream(StringUtils.split(config.getString("validator.channels", ValidatorChannel.REST_API.getName()+","+ValidatorChannel.SOAP_API.getName()+","+ValidatorChannel.FORM.getName()), ',')).map(String::trim).map(ValidatorChannel::byName).collect(Collectors.toSet()));
                domainConfig.setShaclFile(parseShaclMap("validator.shaclFile", config, domainConfig.getType()));
                domainConfig.setDefaultReportSyntax(config.getString("validator.defaultReportSyntax", appConfig.getDefaultReportSyntax()));
                domainConfig.setWebContentSyntax(Arrays.stream(StringUtils.split(config.getString("validator.contentSyntax", ""), ',')).map(String::trim).collect(Collectors.toList()));
                domainConfig.setExternalShapes(parseBooleanMap("validator.externalShapes", config, domainConfig.getType()));
                domainConfig.setWebServiceId(config.getString("validator.webServiceId", "ValidatorService"));
                domainConfig.setWebServiceDescription(parseMap("validator.webServiceDescription", config, Arrays.asList(ValidationConstants.INPUT_CONTENT, ValidationConstants.INPUT_SYNTAX, ValidationConstants.INPUT_VALIDATION_TYPE, ValidationConstants.INPUT_EXTERNAL_RULES, ValidationConstants.INPUT_EMBEDDING_METHOD)));
                domainConfig.setReportsOrdered(config.getBoolean("validator.reportsOrdered", false));
                domainConfigs.put(domain, domainConfig);
                domainConfig.setShowAbout(config.getBoolean("validator.showAbout", true));
                setLabels(domainConfig, config);
                logger.info("Loaded configuration for domain ["+domain+"]");
            }
        }
        return domainConfig;
    }

    private void setLabels(DomainConfig domainConfig, CompositeConfiguration config) {
        // If the required labels ever increase the following code should be transformed into proper resource management.
        domainConfig.getLabel().setResultSectionTitle(config.getString("validator.label.resultSectionTitle", "Validation result"));
        domainConfig.getLabel().setFileInputLabel(config.getString("validator.label.fileInputLabel", "Content to validate"));
        domainConfig.getLabel().setFileInputPlaceholder(config.getString("validator.label.fileInputPlaceholder", "Select file..."));
        domainConfig.getLabel().setTypeLabel(config.getString("validator.label.typeLabel", "Validate as"));
        domainConfig.getLabel().setContentSyntaxLabel(config.getString("validator.label.contentSyntaxLabel", "Content syntax"));
        domainConfig.getLabel().setExternalShapesLabel(config.getString("validator.label.externalShapesLabel", "External shapes"));
        domainConfig.getLabel().setUploadButton(config.getString("validator.label.uploadButton", "Upload"));
        domainConfig.getLabel().setResultSubSectionOverviewTitle(config.getString("validator.label.resultSubSectionOverviewTitle", "Overview"));
        domainConfig.getLabel().setResultDateLabel(config.getString("validator.label.resultDateLabel", "Date:"));
        domainConfig.getLabel().setResultFileNameLabel(config.getString("validator.label.resultFileNameLabel", "File name:"));
        domainConfig.getLabel().setResultResultLabel(config.getString("validator.label.resultResultLabel", "Result:"));
        domainConfig.getLabel().setResultErrorsLabel(config.getString("validator.label.resultErrorsLabel", "Errors:"));
        domainConfig.getLabel().setResultWarningsLabel(config.getString("validator.label.resultWarningsLabel", "Warnings:"));
        domainConfig.getLabel().setResultMessagesLabel(config.getString("validator.label.resultMessagesLabel", "Messages:"));
        domainConfig.getLabel().setResultSubSectionDetailsTitle(config.getString("validator.label.resultSubSectionDetailsTitle", "Details"));
        domainConfig.getLabel().setResultTestLabel(config.getString("validator.label.resultTestLabel", "Test:"));
        domainConfig.getLabel().setResultLocationLabel(config.getString("validator.label.resultLocationLabel", "Location:"));
        domainConfig.getLabel().setOptionDownloadReport(config.getString("validator.label.optionDownloadReport", "Validation report"));
        domainConfig.getLabel().setOptionDownloadContent(config.getString("validator.label.optionDownloadContent", "Validated content"));
        domainConfig.getLabel().setOptionDownloadShapes(config.getString("validator.label.optionDownloadShapes", "Shapes"));
        domainConfig.getLabel().setContentSyntaxTooltip(config.getString("validator.label.contentSyntaxTooltip", "Optional for content provided as a file or a URI if a known file extension is detected"));
        domainConfig.getLabel().setExternalRulesTooltip(config.getString("validator.label.externalRulesTooltip", "Additional shapes that will be considered for the validation"));
        domainConfig.getLabel().setSaveDownload(config.getString("validator.label.saveDownload", "Download"));
        domainConfig.getLabel().setSaveAs(config.getString("validator.label.saveAs", "as"));
        domainConfig.getLabel().setIncludeExternalShapes(config.getString("validator.label.includeExternalShapes", "Include external shapes"));
        domainConfig.getLabel().setOptionContentFile(config.getString("validator.label.optionContentFile", "File"));
        domainConfig.getLabel().setOptionContentURI(config.getString("validator.label.optionContentURI", "URI"));
        domainConfig.getLabel().setOptionContentDirectInput(config.getString("validator.label.optionContentDirectInput", "Direct input"));
    }

    private Map<String, ShaclFileInfo> parseShaclMap(String key, CompositeConfiguration config, List<String> types){
        Map<String, DomainConfig.ShaclFileInfo> map = new HashMap<>();
        for (String type: types) {
            DomainConfig.ShaclFileInfo shaclFileInfo = new ShaclFileInfo();
            List<RemoteInfo> remoteInfo = new ArrayList<>();
            Set<String> processedRemote = new HashSet<>();
            
            String internalShaclFile = config.getString(key+"."+type, null);            
            shaclFileInfo.setLocalFolder(internalShaclFile);
            
            Iterator<String> it = config.getKeys("validator.shaclFile." + type + ".remote");
            while(it.hasNext()) {
            	String remoteKeys = it.next(); 
            	String remoteInt = remoteKeys.replaceAll("(validator.shaclFile." + type + ".remote.)([0-9]{1,})(.[a-zA-Z]*)", "$2");

            	if(!processedRemote.contains(remoteInt)) {
                    RemoteInfo ri = new RemoteInfo();
                	ri.setType(config.getString("validator.shaclFile." + type + ".remote."+remoteInt+".type"));
                	ri.setUrl(config.getString("validator.shaclFile." + type + ".remote."+remoteInt+".url"));
                	
                	remoteInfo.add(ri);
                	processedRemote.add(remoteInt);
            	}
            }
            
            shaclFileInfo.setRemote(remoteInfo);
            map.put(type, shaclFileInfo);
        }
    	
    	return map;
    }

    private Map<String, Boolean> parseBooleanMap(String key, CompositeConfiguration config, List<String> types) {
        Map<String, Boolean> map = new HashMap<>();
        for (String type: types) {
            boolean value = false;
            
            try {
            	value = config.getBoolean(key+"."+type);
            }catch(Exception e){
            	value = false;
            }
            finally {
                map.put(type, value);
            }
        }
        return map;
    }

    private Map<String, String> parseMap(String key, CompositeConfiguration config, List<String> types) {
        Map<String, String> map = new HashMap<>();
        for (String type: types) {
            String defaultValue = appConfig.getDefaultLabels().get(type);
            String val = config.getString(key+"."+type, defaultValue);
            if (val != null) {
                map.put(type, val.trim());
            }
        }
        return map;
    }

    private class ExtensionFilter implements FilenameFilter {

        private String ext;

        public ExtensionFilter(String ext) {
            this.ext = ext;
        }

        public boolean accept(File dir, String name) {
            return (name.endsWith(ext));
        }
    }

}
