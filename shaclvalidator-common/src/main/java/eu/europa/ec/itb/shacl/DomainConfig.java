package eu.europa.ec.itb.shacl;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by simatosc on 21/03/2016.
 */
public class DomainConfig {

    private String domain;
    private String uploadTitle = "Validator";
    private String webServiceId = "UBLValidationService";
    private String reportTitle = "Validation report";
    private Map<String, String> webServiceDescription;
    private List<String> type;
    private Set<ValidatorChannel> channels;
    private Map<String, String> typeLabel;
    private Map<String,String> shaclFile;

    private String mailFrom;
    private boolean mailAuthEnable = true;
    private String mailAuthUsername = "validate.invoice@gmail.com";
    private String mailAuthPassword = "Admin12345_";
    private String mailOutboundHost = "smtp.gmail.com";
    private int mailOutboundPort = 465;
    private boolean mailOutboundSSLEnable = true;
    private String mailInboundHost = "imap.gmail.com";
    private int mailInboundPort = 993;
    private boolean mailInboundSSLEnable = true;
    private String mailInboundFolder = "INBOX";
    private boolean showAbout;

    private boolean includeTestDefinition;
    private boolean reportsOrdered;

    private Label label = new Label();

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean isIncludeTestDefinition() {
        return includeTestDefinition;
    }

    public void setIncludeTestDefinition(boolean includeTestDefinition) {
        this.includeTestDefinition = includeTestDefinition;
    }

    public boolean isReportsOrdered() {
        return reportsOrdered;
    }

    public void setReportsOrdered(boolean reportsOrdered) {
        this.reportsOrdered = reportsOrdered;
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

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public boolean isMailAuthEnable() {
        return mailAuthEnable;
    }

    public void setMailAuthEnable(boolean mailAuthEnable) {
        this.mailAuthEnable = mailAuthEnable;
    }

    public String getMailAuthUsername() {
        return mailAuthUsername;
    }

    public void setMailAuthUsername(String mailAuthUsername) {
        this.mailAuthUsername = mailAuthUsername;
    }

    public String getMailAuthPassword() {
        return mailAuthPassword;
    }

    public void setMailAuthPassword(String mailAuthPassword) {
        this.mailAuthPassword = mailAuthPassword;
    }

    public String getMailOutboundHost() {
        return mailOutboundHost;
    }

    public void setMailOutboundHost(String mailOutboundHost) {
        this.mailOutboundHost = mailOutboundHost;
    }

    public int getMailOutboundPort() {
        return mailOutboundPort;
    }

    public void setMailOutboundPort(int mailOutboundPort) {
        this.mailOutboundPort = mailOutboundPort;
    }

    public boolean isMailOutboundSSLEnable() {
        return mailOutboundSSLEnable;
    }

    public void setMailOutboundSSLEnable(boolean mailOutboundSSLEnable) {
        this.mailOutboundSSLEnable = mailOutboundSSLEnable;
    }

    public String getMailInboundHost() {
        return mailInboundHost;
    }

    public void setMailInboundHost(String mailInboundHost) {
        this.mailInboundHost = mailInboundHost;
    }

    public int getMailInboundPort() {
        return mailInboundPort;
    }

    public void setMailInboundPort(int mailInboundPort) {
        this.mailInboundPort = mailInboundPort;
    }

    public boolean isMailInboundSSLEnable() {
        return mailInboundSSLEnable;
    }

    public void setMailInboundSSLEnable(boolean mailInboundSSLEnable) {
        this.mailInboundSSLEnable = mailInboundSSLEnable;
    }

    public String getMailInboundFolder() {
        return mailInboundFolder;
    }

    public void setMailInboundFolder(String mailInboundFolder) {
        this.mailInboundFolder = mailInboundFolder;
    }

    public String getUploadTitle() {
        return uploadTitle;
    }

    public void setUploadTitle(String uploadTitle) {
        this.uploadTitle = uploadTitle;
    }

    public String getWebServiceId() {
        return webServiceId;
    }

    public void setWebServiceId(String webServiceId) {
        this.webServiceId = webServiceId;
    }

    public Map<String, String> getWebServiceDescription() {
        return webServiceDescription;
    }

    public void setWebServiceDescription(Map<String, String> webServiceDescription) {
        this.webServiceDescription = webServiceDescription;
    }

    public Map<String, String> getTypeLabel() {
        return typeLabel;
    }

    public List<String> getType() {
        return type;
    }

    public void setTypeLabel(Map<String, String> typeLabel) {
        this.typeLabel = typeLabel;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public Set<ValidatorChannel> getChannels() {
        return channels;
    }

    public void setChannels(Set<ValidatorChannel> channels) {
        this.channels = channels;
    }

    public boolean isShowAbout() {
        return showAbout;
    }

    public void setShowAbout(boolean showAbout) {
        this.showAbout = showAbout;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public static class Label {

        private String inputSectionTitle;
        private String resultSectionTitle;
        private String fileInputLabel;
        private String fileInputPlaceholder;
        private String typeLabel;
        private String uploadButton;
        private String resultSubSectionOverviewTitle;
        private String resultDateLabel;
        private String resultFileNameLabel;
        private String resultResultLabel;
        private String resultErrorsLabel;
        private String resultWarningsLabel;
        private String resultMessagesLabel;
        private String viewAnnotatedInputButton;
        private String downloadXMLReportButton;
        private String downloadPDFReportButton;
        private String resultSubSectionDetailsTitle;
        private String resultTestLabel;
        private String popupTitle;
        private String popupCloseButton;

        public String getInputSectionTitle() {
            return inputSectionTitle;
        }

        public void setInputSectionTitle(String inputSectionTitle) {
            this.inputSectionTitle = inputSectionTitle;
        }

        public String getResultSectionTitle() {
            return resultSectionTitle;
        }

        public void setResultSectionTitle(String resultSectionTitle) {
            this.resultSectionTitle = resultSectionTitle;
        }

        public String getFileInputLabel() {
            return fileInputLabel;
        }

        public void setFileInputLabel(String fileInputLabel) {
            this.fileInputLabel = fileInputLabel;
        }

        public String getFileInputPlaceholder() {
            return fileInputPlaceholder;
        }

        public void setFileInputPlaceholder(String fileInputPlaceholder) {
            this.fileInputPlaceholder = fileInputPlaceholder;
        }

        public String getTypeLabel() {
            return typeLabel;
        }

        public void setTypeLabel(String typeLabel) {
            this.typeLabel = typeLabel;
        }

        public String getUploadButton() {
            return uploadButton;
        }

        public void setUploadButton(String uploadButton) {
            this.uploadButton = uploadButton;
        }

        public String getResultSubSectionOverviewTitle() {
            return resultSubSectionOverviewTitle;
        }

        public void setResultSubSectionOverviewTitle(String resultSubSectionOverviewTitle) {
            this.resultSubSectionOverviewTitle = resultSubSectionOverviewTitle;
        }

        public String getResultDateLabel() {
            return resultDateLabel;
        }

        public void setResultDateLabel(String resultDateLabel) {
            this.resultDateLabel = resultDateLabel;
        }

        public String getResultFileNameLabel() {
            return resultFileNameLabel;
        }

        public void setResultFileNameLabel(String resultFileNameLabel) {
            this.resultFileNameLabel = resultFileNameLabel;
        }

        public String getResultResultLabel() {
            return resultResultLabel;
        }

        public void setResultResultLabel(String resultResultLabel) {
            this.resultResultLabel = resultResultLabel;
        }

        public String getResultErrorsLabel() {
            return resultErrorsLabel;
        }

        public void setResultErrorsLabel(String resultErrorsLabel) {
            this.resultErrorsLabel = resultErrorsLabel;
        }

        public String getResultWarningsLabel() {
            return resultWarningsLabel;
        }

        public void setResultWarningsLabel(String resultWarningsLabel) {
            this.resultWarningsLabel = resultWarningsLabel;
        }

        public String getResultMessagesLabel() {
            return resultMessagesLabel;
        }

        public void setResultMessagesLabel(String resultMessagesLabel) {
            this.resultMessagesLabel = resultMessagesLabel;
        }

        public String getViewAnnotatedInputButton() {
            return viewAnnotatedInputButton;
        }

        public void setViewAnnotatedInputButton(String viewAnnotatedInputButton) {
            this.viewAnnotatedInputButton = viewAnnotatedInputButton;
        }

        public String getDownloadXMLReportButton() {
            return downloadXMLReportButton;
        }

        public void setDownloadXMLReportButton(String downloadXMLReportButton) {
            this.downloadXMLReportButton = downloadXMLReportButton;
        }

        public String getDownloadPDFReportButton() {
            return downloadPDFReportButton;
        }

        public void setDownloadPDFReportButton(String downloadPDFReportButton) {
            this.downloadPDFReportButton = downloadPDFReportButton;
        }

        public String getResultSubSectionDetailsTitle() {
            return resultSubSectionDetailsTitle;
        }

        public void setResultSubSectionDetailsTitle(String resultSubSectionDetailsTitle) {
            this.resultSubSectionDetailsTitle = resultSubSectionDetailsTitle;
        }

        public String getResultTestLabel() {
            return resultTestLabel;
        }

        public void setResultTestLabel(String resultTestLabel) {
            this.resultTestLabel = resultTestLabel;
        }

        public String getPopupTitle() {
            return popupTitle;
        }

        public void setPopupTitle(String popupTitle) {
            this.popupTitle = popupTitle;
        }

        public String getPopupCloseButton() {
            return popupCloseButton;
        }

        public void setPopupCloseButton(String popupCloseButton) {
            this.popupCloseButton = popupCloseButton;
        }

    }
}
