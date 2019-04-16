package eu.europa.ec.itb.shacl;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by simatosc on 21/03/2016.
 */
public class DomainConfig {

    private boolean isDefined;
    private String domain;
    private String domainName;
    private List<String> type;
    private Map<String, String> typeLabel;
    private Set<ValidatorChannel> channels;
    private Map<String, ShaclFileInfo> shaclFile;
    private String defaultReportSyntax;
    private Map<String, Boolean> externalShapes;

    public DomainConfig() {
        this(true);
    }

    public DomainConfig(boolean isDefined) {
        this.isDefined = isDefined;
    }
    
    public void setExternalShapes(Map<String, Boolean> externalShapes) {
    	this.externalShapes = externalShapes;
    }
    
    public Map<String, Boolean> getExternalShapes() {
    	return this.externalShapes;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean hasMultipleValidationTypes() {
        return type != null && type.size() > 1;
    }

    public Map<String, ShaclFileInfo> getShaclFile() {
        return shaclFile;
    }

    public void setShaclFile(Map<String, ShaclFileInfo> shaclFile) {
        this.shaclFile = shaclFile;
    }

    public List<String> getType() {
        return type;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public Set<ValidatorChannel> getChannels() {
        return channels;
    }

    public void setChannels(Set<ValidatorChannel> channels) {
        this.channels = channels;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public void setDefined(boolean defined) {
        isDefined = defined;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Map<String, String> getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(Map<String, String> typeLabel) {
        this.typeLabel = typeLabel;
    }

    public String getDefaultReportSyntax() {
        return defaultReportSyntax;
    }

    public void setDefaultReportSyntax(String defaultReportSyntax) {
        this.defaultReportSyntax = defaultReportSyntax;
    }
    
    
    public static class ShaclFileInfo {
    	String localFolder;
    	List<RemoteInfo> remote;
    	
    	public String getLocalFolder() {
    		return localFolder;
    	}
    	
    	public void setLocalFolder(String localFolder) {
    		this.localFolder = localFolder;
    	}
    	
    	public List<RemoteInfo> getRemote() { 
    		return remote; 
    	}
    	
    	public void setRemote(List<RemoteInfo> remote) { 
    		this.remote = remote; 
    	}
    }
    
    public static class RemoteInfo{
    	String url;
    	String type;
    	
    	public String getUrl() {
    		return url;
    	}
    	
    	public void setUrl(String url) {
    		this.url = url;
    	}
    	
    	public String getType() {
    		return type;
    	}
    	
    	public void setType(String type) {
    		this.type = type;
    	}
    }
}
