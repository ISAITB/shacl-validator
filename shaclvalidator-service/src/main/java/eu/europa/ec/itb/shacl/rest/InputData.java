package eu.europa.ec.itb.shacl.rest;

import java.util.List;

import io.swagger.annotations.ApiModelProperty;

public class InputData {
    public static final String embedding_URL     	= "URL" ;
    public static final String embedding_BASE64		= "BASE64" ;
    
    @ApiModelProperty(required = true, notes = "The RDF content to validate.")
	protected String contentToValidate;
    @ApiModelProperty(required = false, notes = "The mime type of the provided RDF content (e.g. \"application/rdf+xml\", \"application/ld+json\", \"text/turtle\"). If not provided the type is determined from the provided content (if possible).")
	protected String contentSyntax;
    @ApiModelProperty(required = false, notes = "The way in which to interpret the contentToValidate. If not provided, the method will be determined from the contentToValidate value (i.e. check it is a valid URL).")
	protected String embeddingMethod;
    @ApiModelProperty(required = false, notes = "The type of validation to perform (e.g. the profile to apply or the version to validate against). This can be skipped if a single validation type is supported by the validator. Otherwise, if multiple are supported, the service should fail with an error.")
	protected String validationType;
    @ApiModelProperty(required = false, notes = "The mime type for the validation report systax. If none is provided \"application/rdf+xml\" is considered as the default.")
	protected String reportSyntax;
    @ApiModelProperty(required = false, notes = "Any shapes to consider that are externally provided (i.e. provided at the time of the call).")
	protected List<RuleSet> externalRules;

	public String getContentToValidate() { return this.contentToValidate; }
	
	public String getEmbeddingMethod() { return this.embeddingMethod; }
	
	public String getValidationType() { return this.validationType; }
	
	public String getReportSyntax() { return this.reportSyntax; }
	
	public String getContentSyntax() { return this.contentSyntax; }
	
	public List<RuleSet> getExternalRules(){ return this.externalRules; }
	
	public RuleSet getExternalRules(int value) { return this.externalRules.get(value); }
	
	public class RuleSet{
	    @ApiModelProperty(required = true, notes = "The RDF containing the rules to apply (shapes).")
		String ruleSet;
	    @ApiModelProperty(required = false, notes = "The way in which to interpret the value for ruleSet. If not provided, the method will be determined from the ruleSet value (i.e. check it is a valid URL).")
		String embeddingMethod;
		
		public String getRuleSet() { return this.ruleSet; }
		
		public String getEmbeddingMethod() { return this.embeddingMethod; }
	}
}
