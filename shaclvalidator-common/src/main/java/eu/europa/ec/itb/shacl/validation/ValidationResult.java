package eu.europa.ec.itb.shacl.validation;

public class ValidationResult {
	private String focusNode;
	private String resultMessage;
	private String resultPath;
	private String resultSeverity;
	private String sourceConstraint;
	private String sourceConstraintComponent;
	private String sourceShape;
	private String value;
	
	public ValidationResult() {
		 
	}
	
	public String getFocusNode() {
		return focusNode;
	}
	public void setFocusNode(String focusNode) {
		this.focusNode = focusNode;
	}
	public String getResultMessage() {
		return resultMessage;
	}
	public void setResultMessage(String resultMessage) {
		this.resultMessage = resultMessage;
	}
	public String getResultPath() {
		return resultPath;
	}
	public void setResultPath(String resultPath) {
		this.resultPath = resultPath;
	}
	public String getResultSeverity() {
		return resultSeverity;
	}
	public void setResultSeverity(String resultSeverity) {
		this.resultSeverity = resultSeverity;
	}
	public String getSourceConstraint() {
		return sourceConstraint;
	}
	public void setSourceConstraint(String sourceConstraint) {
		this.sourceConstraint = sourceConstraint;
	}
	public String getSourceConstraintComponent() {
		return sourceConstraintComponent;
	}
	public void setSourceConstraintComponent(String sourceConstraintComponent) {
		this.sourceConstraintComponent = sourceConstraintComponent;
	}
	public String getSourceShape() {
		return sourceShape;
	}
	public void setSourceShape(String sourceShape) {
		this.sourceShape = sourceShape;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
}
