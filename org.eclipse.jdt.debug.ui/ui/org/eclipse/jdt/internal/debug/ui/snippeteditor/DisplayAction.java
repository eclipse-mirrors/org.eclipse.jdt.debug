/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui.snippeteditor;

import org.eclipse.jdt.internal.debug.ui.JavaDebugImages;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Displays the result of a snippet
 *
 */
public class DisplayAction extends SnippetAction implements IUpdate {

	public DisplayAction(JavaSnippetEditor editor) {
		super(editor);
		setImageDescriptor(JavaDebugImages.DESC_TOOL_DISPLAYSNIPPET);

		setText(SnippetMessages.getString("DisplayAction.label")); //$NON-NLS-1$
		setToolTipText(SnippetMessages.getString("DisplayAction.tooltip")); //$NON-NLS-1$
		setDescription(SnippetMessages.getString("DisplayAction.description")); //$NON-NLS-1$
	}
	
	public void run() {
		fEditor.evalSelection(JavaSnippetEditor.RESULT_DISPLAY);
	} 
}
