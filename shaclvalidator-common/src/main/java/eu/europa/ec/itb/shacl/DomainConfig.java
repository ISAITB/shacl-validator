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
    private String webServiceId = "SHACLValidationService";
    private Map<String, String> webServiceDescription;
    //UI
    private List<String> webContentSyntax;
    private String reportTitle = "Validation report";
    private String uploadTitle = "Validator";
    private Label label = new Label();
    private boolean showAbout;

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

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }


    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
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

    public boolean isShowAbout() {
        return showAbout;
    }

    public void setShowAbout(boolean showAbout) {
        this.showAbout = showAbout;
    }
    
    
    public List<String> getWebContentSyntax() {
		return webContentSyntax;
	}

	public void setWebContentSyntax(List<String> webContentSyntax) {
		this.webContentSyntax = webContentSyntax;
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

    public static class Label {

        private String inputSectionTitle;
        private String resultSectionTitle;
        private String fileInputLabel;
        private String fileInputPlaceholder;
        private String typeLabel;
        private String contentSyntaxLabel;
        private String externalShapesLabel;
        private String uploadButton;
        private String resultSubSectionOverviewTitle;
        private String resultDateLabel;
        private String resultFileNameLabel;
        private String resultResultLabel;
        private String resultErrorsLabel;
        private String resultWarningsLabel;
        private String resultMessagesLabel;
        private String viewAnnotatedInputButton;
        private String downloadRDFReportButton;
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

        public String getContentSyntaxLabel() {
            return contentSyntaxLabel;
        }

        public void setContentSyntaxLabel(String contentSyntaxLabel) {
            this.contentSyntaxLabel = contentSyntaxLabel;
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

        public String getDownloadRDFReportButton() {
            return downloadRDFReportButton;
        }

        public void setDownloadRDFReportButton(String downloadRDFReportButton) {
            this.downloadRDFReportButton = downloadRDFReportButton;
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

		public String getExternalShapesLabel() {
			return externalShapesLabel;
		}

		public void setExternalShapesLabel(String externalShapesLabel) {
			this.externalShapesLabel = externalShapesLabel;
		}

    }
}
