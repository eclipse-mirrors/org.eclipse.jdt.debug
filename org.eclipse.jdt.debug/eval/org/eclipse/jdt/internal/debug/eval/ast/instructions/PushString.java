package org.eclipse.jdt.internal.debug.eval.ast.instructions;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
/**
 * Pushes a String literal onto the stack.
 */
public class PushString extends SimpleInstruction {
	
	private String fValue;
	
	public PushString(String value) {
		fValue = value;
	}
	
	public void execute() {
		pushNewValue(fValue);
	}
	
	public String toString() {
		return InstructionsEvaluationMessages.getString("PushString.push__1") + fValue; //$NON-NLS-1$
	}

}

