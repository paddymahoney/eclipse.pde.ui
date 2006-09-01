/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSModel;
import org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSWorkspaceModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.context.UTF8InputContext;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;

/**
 * SimpleCSInputContext
 *
 */
public class SimpleCSInputContext extends UTF8InputContext {

	public static final String CONTEXT_ID = "simplecs-context"; //$NON-NLS-1$	
	
	/**
	 * @param editor
	 * @param input
	 * @param primary
	 */
	public SimpleCSInputContext(PDEFormEditor editor, IEditorInput input,
			boolean primary) {
		super(editor, input, primary);
		create();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContext#addTextEditOperation(java.util.ArrayList, org.eclipse.pde.core.IModelChangedEvent)
	 */
	protected void addTextEditOperation(ArrayList ops, IModelChangedEvent event) {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContext#createModel(org.eclipse.ui.IEditorInput)
	 */
	protected IBaseModel createModel(IEditorInput input) throws CoreException {
		ISimpleCSModel model = null;
		if (input instanceof IStorageEditorInput) {
			try {
				if (input instanceof IFileEditorInput) {
					IFile file = ((IFileEditorInput) input).getFile();
					model = new SimpleCSWorkspaceModel(file, true);
					model.load();
				} else if (input instanceof IStorageEditorInput) {
					InputStream is = ((IStorageEditorInput) input).getStorage()
							.getContents();
					model = new SimpleCSModel();
					model.load(is, false);
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
				return null;
			}
		}
		return model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContext#getId()
	 */
	public String getId() {
		return CONTEXT_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContext#getPartitionName()
	 */
	protected String getPartitionName() {
		return "___simplecs_partition"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.InputContext#flushModel(org.eclipse.jface.text.IDocument)
	 */
	protected void flushModel(IDocument doc) {
		if (!(getModel() instanceof IEditable))
			return;
		IEditable editableModel = (IEditable) getModel();
		if (editableModel.isDirty() == false)
			return;
		try {
			StringWriter swriter = new StringWriter();
			PrintWriter writer = new PrintWriter(swriter);
			editableModel.save(writer);
			writer.flush();
			swriter.close();
			doc.set(swriter.toString());
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
	}	
}
