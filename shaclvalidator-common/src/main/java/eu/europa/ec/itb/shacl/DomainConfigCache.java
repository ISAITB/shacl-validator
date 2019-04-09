package eu.europa.ec.itb.shacl;

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

import eu.europa.ec.itb.shacl.DomainConfig.ShaclFileInfo;
import eu.europa.ec.itb.shacl.DomainConfig.RemoteInfo;

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
            if (domainConfig != null) {
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
                domainConfig.setDomainName(appConfig.getDomainIdToDomainName().get(domain));
                domainConfig.setType(Arrays.stream(StringUtils.split(config.getString("validator.type"), ',')).map(String::trim).collect(Collectors.toList()));
                domainConfig.setTypeLabel(parseMap("validator.typeLabel", config, domainConfig.getType()));
                domainConfig.setChannels(Arrays.stream(StringUtils.split(config.getString("validator.channels", ValidatorChannel.REST_API.getName()), ',')).map(String::trim).map(ValidatorChannel::byName).collect(Collectors.toSet()));
                domainConfig.setShaclFile(parseShaclMap("validator.shaclFile", config, domainConfig.getType()));
                domainConfig.setDefaultReportSyntax(config.getString("validator.defaultReportSyntax", appConfig.getDefaultReportSyntax()));
                domainConfigs.put(domain, domainConfig);
                logger.info("Loaded configuration for domain ["+domain+"]");
            }
        }
        return domainConfig;
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

    private Map<String, String> parseMap(String key, CompositeConfiguration config, List<String> types) {
        Map<String, String> map = new HashMap<>();
        for (String type: types) {
            String val = config.getString(key+"."+type, null);
            if (val != null) {
                map.put(type, config.getString(key+"."+type).trim());
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
