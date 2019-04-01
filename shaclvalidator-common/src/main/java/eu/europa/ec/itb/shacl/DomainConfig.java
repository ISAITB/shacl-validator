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
    private Map<String,String> shaclFile;
    private String defaultReportSyntax;

    public DomainConfig() {
        this(true);
    }

    public DomainConfig(boolean isDefined) {
        this.isDefined = isDefined;
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

    public Map<String, String> getShaclFile() {
        return shaclFile;
    }

    public void setShaclFile(Map<String, String> shaclFile) {
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
}
