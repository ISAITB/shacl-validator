package eu.europa.ec.itb.shacl.ws;

import java.util.List;

public class InputMultipleData {
	protected List<InputData> input;

	public List<InputData> getInput() { return this.input; }
	
	public InputData getInput(int value) { return this.input.get(value); }
}
