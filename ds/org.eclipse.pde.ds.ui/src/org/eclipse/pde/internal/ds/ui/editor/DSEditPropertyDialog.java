/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 244997
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SWTUtil;
import org.eclipse.pde.internal.ds.ui.parts.ComboPart;
import org.eclipse.pde.internal.ds.ui.parts.FormEntry;
import org.eclipse.pde.internal.ds.ui.wizards.DSNewClassCreationWizard;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class DSEditPropertyDialog extends FormDialog {

	private IDSProperty fProperty;
	private DSPropertiesSection fPropertiesSection;
	private FormEntry fNameEntry;
	private ComboPart fTypeCombo;
	private FormEntry fValueEntry;
	private FormEntry fBodyEntry;
	private Label fTypeLabel;
	private boolean fAddDialog; // boolean used to erase added element whether user
							// clicks on cancel button.

	protected DSEditPropertyDialog(Shell parentShell, IDSProperty property,
			DSPropertiesSection propertiesSection, boolean addDialog) {
		super(parentShell);
		fProperty = property;
		fPropertiesSection = propertiesSection;
		fAddDialog = addDialog;

	}

	protected void createFormContent(IManagedForm mform) {
		mform.getForm().setText(Messages.DSEditPropertyDialog_dialog_title);

		Composite container = mform.getForm().getBody();
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		FormToolkit toolkit = mform.getToolkit();

		Composite mainContainer = toolkit.createComposite(container);
		mainContainer.setLayout(FormLayoutFactory
				.createSectionClientGridLayout(false, 2));
		mainContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Attribute: name
		fNameEntry = new FormEntry(mainContainer, toolkit,
				Messages.DSPropertyDetails_nameEntry, SWT.MULTI);

		// Attribute: value
		fValueEntry = new FormEntry(mainContainer, toolkit,
				Messages.DSPropertyDetails_valueEntry, SWT.NONE);

		// Attribute: type
		fTypeLabel = toolkit.createLabel(mainContainer,
				Messages.DSPropertyDetails_typeEntry, SWT.WRAP);
		fTypeCombo = new ComboPart();
		fTypeCombo.createControl(mainContainer, toolkit, SWT.READ_ONLY);

		String[] itemsCard = new String[] {
				IDSConstants.VALUE_PROPERTY_TYPE_BOOLEAN,
				IDSConstants.VALUE_PROPERTY_TYPE_BYTE,
				IDSConstants.VALUE_PROPERTY_TYPE_CHAR,
				IDSConstants.VALUE_PROPERTY_TYPE_DOUBLE,
				IDSConstants.VALUE_PROPERTY_TYPE_FLOAT,
				IDSConstants.VALUE_PROPERTY_TYPE_INTEGER,
				IDSConstants.VALUE_PROPERTY_TYPE_LONG,
				IDSConstants.VALUE_PROPERTY_TYPE_SHORT,
				IDSConstants.VALUE_PROPERTY_TYPE_STRING };
		fTypeCombo.setItems(itemsCard);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalIndent = 3;
		fTypeCombo.getControl().setLayoutData(data);

		// description: Content (Element)
		fBodyEntry = new FormEntry(mainContainer, toolkit,
				Messages.DSPropertyDetails_bodyLabel, SWT.MULTI | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 60;
		data.horizontalIndent = 4;
		fBodyEntry.getText().setLayoutData(data);

		updateFields();

		setEntryListeners();

		toolkit.paintBordersFor(mainContainer);

	}

	private Section addSection(FormToolkit toolkit, Composite mainContainer) {
		Section section = toolkit.createSection(mainContainer,
				Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		section.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		section.setText(Messages.DSEditPropertyDialog_dialog_title);
		section.setDescription(Messages.DSEditPropertyDialog_dialogMessage);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		return section;
	}

	public boolean isHelpAvailable() {
		return false;
	}

	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case 0:
			handleOKPressed();
			break;
		}
		super.buttonPressed(buttonId);
	}


	private void handleOKPressed() {

		if (!fNameEntry.getValue().equals("")) { //$NON-NLS-1$
			if (!fNameEntry.getValue().equals(fProperty.getPropertyName())) {
				fProperty.setPropertyName(fNameEntry.getValue());
			}
		}

		if (!fValueEntry.getValue().equals("")) { //$NON-NLS-1$
			if (!fValueEntry.getValue().equals(fProperty.getPropertyValue())) {
				fProperty.setPropertyValue(fValueEntry.getValue());
			}
		}

		String typeSelection = fTypeCombo.getSelection();
		if (typeSelection != null) {
			if (!typeSelection.equals("")) { //$NON-NLS-1$
				if (!typeSelection.equals(fProperty.getPropertyType())) {
					fProperty.setPropertyType(typeSelection);
				}
			}
		}

		if (!fBodyEntry.getValue().equals("")) { //$NON-NLS-1$
			String propertyElemBody = fProperty.getPropertyElemBody();
			if (propertyElemBody != null) {
				propertyElemBody = "";
			}
			if (!fBodyEntry.getValue().equals(propertyElemBody)) {
				fProperty.setPropertyElemBody(fBodyEntry.getValue());
			}
		}
		
		if (fAddDialog) {
			fProperty.getModel().getDSComponent().addPropertyElement(fProperty);
		}
	}

	public void updateFields() {

		// Ensure data object is defined
		if (fProperty == null) {
			return;
		}
		// Attribute: name
		if (fProperty.getPropertyName() != null) {
			fNameEntry.setValue(fProperty.getPropertyName(), true);
		} else {
			fNameEntry.setValue("", true); //$NON-NLS-1$
		}
		fNameEntry.setEditable(true);

		// Attribute: value
		if (fProperty.getPropertyValue() != null) {
			fValueEntry.setValue(fProperty.getPropertyValue(), true);
		} else {
			fValueEntry.setValue("", true); //$NON-NLS-1$
		}
		fValueEntry.setEditable(true);

		// Attribute: type
		if (fProperty.getPropertyType() != null)
			fTypeCombo.setText(fProperty.getPropertyType());

		// Attribute: body
		if (fProperty.getPropertyElemBody() != null) {
			fBodyEntry.setValue(fProperty.getPropertyElemBody(), true);
		} else {
			fBodyEntry.setValue("", true); //$NON-NLS-1$
		}
		fBodyEntry.setEditable(true);
	}

	public void setEntryListeners() {
		// Attribute: Name
		fNameEntry.setFormEntryListener(new FormEntryAdapter(
				this.fPropertiesSection) {
			public void textValueChanged(FormEntry entry) {
				// no op due to OK Button
			}

			public void textDirty(FormEntry entry) {
				// no op due to OK Button
			}

			public void linkActivated(HyperlinkEvent e) {
				String value = fNameEntry.getValue();
				value = handleLinkActivated(value, false);
				if (value != null)
					fNameEntry.setValue(value);
			}

			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(fNameEntry);
			}

		});

		// Attribute: Value
		fValueEntry.setFormEntryListener(new FormEntryAdapter(
				this.fPropertiesSection) {
			public void textValueChanged(FormEntry entry) {
				// no op due to OK Button
			}

			public void textDirty(FormEntry entry) {
				// no op due to OK Button
			}

			public void linkActivated(HyperlinkEvent e) {
				String value = fValueEntry.getValue();
				value = handleLinkActivated(value, false);
				if (value != null)
					fValueEntry.setValue(value);
			}

			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(fValueEntry);
			}

		});

		// Attribute: Body
		fBodyEntry.setFormEntryListener(new FormEntryAdapter(
				this.fPropertiesSection) {
			public void textValueChanged(FormEntry entry) {
				// no op due to OK Button
			}

			public void textDirty(FormEntry entry) {
				// no op due to OK Button
			}

		});

	}

	private String handleLinkActivated(String value, boolean isInter) {
		IProject project = getProject();
		try {
			if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement element = javaProject.findType(value.replace('$',
						'.'));
				if (element != null)
					JavaUI.openInEditor(element);
				else {
					DSNewClassCreationWizard wizard = new DSNewClassCreationWizard(
							project, isInter, value);
					WizardDialog dialog = new WizardDialog(Activator
							.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					if (dialog.open() == Window.OK) {
						return wizard.getQualifiedName();
					}
				}
			}
		} catch (PartInitException e1) {
		} catch (CoreException e1) {
		}
		return null;
	}

	private IProject getProject() {
		PDEFormEditor editor = (PDEFormEditor) this.fPropertiesSection
				.getPage().getEditor();
		return editor.getCommonProject();
	}

	private void doOpenSelectionDialog(FormEntry entry) {
		final IProject project = getProject();
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				Activator.getActiveWorkbenchShell(),
				new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setInput(project.getWorkspace());
		IResource resource = getFile(entry);
		if (resource != null)
			dialog.setInitialSelection(resource);
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				if (element instanceof IProject)
					return ((IProject) element).equals(project);
				return true;
			}
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(Messages.DSEditPropertyDialog_dialog_title);
		dialog.setMessage(Messages.DSEditPropertyDialog_dialogMessage);
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection != null
						&& selection.length > 0
						&& (selection[0] instanceof IFile || selection[0] instanceof IContainer))
					return new Status(IStatus.OK, Activator.PLUGIN_ID,
							IStatus.OK, "", null); //$NON-NLS-1$

				return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						IStatus.ERROR, "", null); //$NON-NLS-1$
			}
		});
		if (dialog.open() == Window.OK) {
			IResource res = (IResource) dialog.getFirstResult();
			IPath path = res.getProjectRelativePath();
			if (res instanceof IContainer)
				path = path.addTrailingSeparator();
			String value = path.toString();
			entry.setValue(value);
		}
	}

	private IResource getFile(FormEntry entry) {
		String value = entry.getValue();
		if (value.length() == 0)
			return null;
		IProject project = getProject();
		IPath path = project.getFullPath().append(value);
		return project.getWorkspace().getRoot().findMember(path);
	}

}
