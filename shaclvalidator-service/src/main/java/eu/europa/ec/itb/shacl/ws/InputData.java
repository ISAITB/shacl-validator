package eu.europa.ec.itb.shacl.ws;

import java.util.List;

public class InputData {
    public static final String embeddingMethod_URL     = "URL" ;
    
	protected List<InputContent> input;

	public List<InputContent> getInput() { return this.input; }
	
	public InputContent getInput(int value) { return this.input.get(value); }
	
	public class InputContent {
		protected String contentToValidate;
		protected String embeddingMethod;
		protected String validationType;
		protected String reportSyntax;
		protected String contentSyntax;
		protected List<ExternalRules> externalRules;

		public String getContentToValidate() { return this.contentToValidate; }
		
		public String getEmbeddingMethod() { return this.embeddingMethod; }
		
		public String getValidationType() { return this.validationType; }
		
		public String getReportSyntax() { return this.reportSyntax; }
		
		public String getContentSyntax() { return this.contentSyntax; }
		
		public List<ExternalRules> getExternalRules(){ return this.externalRules; }
		
		public ExternalRules getExternalRules(int value) { return this.externalRules.get(value); }
		
		public class ExternalRules{
			String shape;
			String embeddingMethod;
			
			public String getShape() { return this.shape; }
			
			public String getEmbeddingMethod() { return this.embeddingMethod; }
		}
	}
}
