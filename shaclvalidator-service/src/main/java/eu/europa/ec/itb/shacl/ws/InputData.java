package eu.europa.ec.itb.shacl.ws;

import java.util.List;

public class InputData {
    public static final String embedding_URL     	= "URL" ;
    public static final String embedding_BASE64		= "BASE64" ;
    
	protected String contentToValidate;
	protected String contentSyntax;
	protected String embeddingMethod;
	protected String validationType;
	protected String reportSyntax;
	protected List<RuleSet> externalRules;

	public String getContentToValidate() { return this.contentToValidate; }
	
	public String getEmbeddingMethod() { return this.embeddingMethod; }
	
	public String getValidationType() { return this.validationType; }
	
	public String getReportSyntax() { return this.reportSyntax; }
	
	public String getContentSyntax() { return this.contentSyntax; }
	
	public List<RuleSet> getExternalRules(){ return this.externalRules; }
	
	public RuleSet getExternalRules(int value) { return this.externalRules.get(value); }
	
	public class RuleSet{
		String ruleSet;
		String embeddingMethod;
		
		public String getRuleSet() { return this.ruleSet; }
		
		public String getEmbeddingMethod() { return this.embeddingMethod; }
	}
}
