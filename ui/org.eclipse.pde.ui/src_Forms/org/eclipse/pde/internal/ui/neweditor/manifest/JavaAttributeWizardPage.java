/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.neweditor.manifest;
import java.util.ArrayList;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.manifest.JavaAttributeValue;

public class JavaAttributeWizardPage extends NewClassWizardPage {
	private String className;
	private IProject project;
	private ISchemaAttribute attInfo;
	private IPluginModelBase model;
	private InitialClassProperties initialValues;
	private IJavaProject javaProject;
	private IStatus currentStatus;
	
	class InitialClassProperties {
		// populate new wizard page
		IType superClassType;
		String superClassName;
		IType interfaceType;
		String interfaceName;
		String className;
		String packageName;
		IPackageFragmentRoot packageFragmentRoot;
		IPackageFragment packageFragment;
		public InitialClassProperties() {
			this.superClassType = null;
			this.superClassName = "";
			this.interfaceName = null;
			this.interfaceType = null;
			this.className = null;
			this.packageName = null;
			this.packageFragment = null;
			this.packageFragmentRoot = null;
		}
	}
	public JavaAttributeWizardPage(IProject project, IPluginModelBase model,
			ISchemaAttribute attInfo, String className) {
		super();
		this.className = className;
		this.model = model;
		this.project = project;
		this.attInfo = attInfo;
		try {
			if (project.hasNature(JavaCore.NATURE_ID))
				this.javaProject = JavaCore.create(project);
			else
				this.javaProject = null;
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		initialValues = new InitialClassProperties();
		initialValues.className = className;
	}

	public Object getValue() {
		return new JavaAttributeValue(project, model, attInfo, className);
	}
	public void init() {
		initializeExpectedValues();
		initializeWizardPage();
	}
	protected void initializeWizardPage() {
		setPackageFragmentRoot(initialValues.packageFragmentRoot, true);
		setPackageFragment(initialValues.packageFragment, true);
		setEnclosingType(null, true);
		setEnclosingTypeSelection(false, true);
		setTypeName(initialValues.className, true);
		setSuperClass(initialValues.superClassName, true);
		if (initialValues.interfaceName != null) {
			ArrayList interfaces = new ArrayList();
			interfaces.add(initialValues.interfaceName);
			setSuperInterfaces(interfaces, true);
		}
		boolean hasSuperClass = initialValues.superClassName != null
				&& initialValues.superClassName.length() > 0;
		boolean hasInterface = initialValues.interfaceName != null
				&& initialValues.interfaceName.length() > 0;
		setMethodStubSelection(false, hasSuperClass, hasInterface
				|| hasSuperClass, true);
		if (!currentStatus.isOK())
			updateStatus(currentStatus);
	}
	private IType findTypeForName(String typeName) throws JavaModelException {
		if (typeName == null)
			return null;
		IType type = null;
		String fileName = typeName.replace('.', '/') + ".java";
		IJavaElement element = javaProject.findElement(new Path(fileName));
		if (element == null)
			return null;
		if (element instanceof IClassFile) {
			type = ((IClassFile) element).getType();
		} else if (element instanceof ICompilationUnit) {
			IType[] types = ((ICompilationUnit) element).getTypes();
			type = types[0];
		}
		return type;
	}
	private void initializeExpectedValues() {
		if (javaProject == null)
			return;
		try {
			//			source folder name, package name, class name
			currentStatus = JavaConventions.validateJavaTypeName(initialValues.className);
			int loc = className.lastIndexOf('.');
			if (loc != -1) {
				initialValues.packageName = className.substring(0, loc);
				currentStatus = JavaConventions.validateJavaTypeName(className
						.substring(loc + 1));
				initialValues.className = className.substring(loc + 1);
			} 
			if (initialValues.packageFragmentRoot == null) {
				IPackageFragmentRoot srcEntryDft = null;
				IPackageFragmentRoot[] roots = javaProject
						.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
						srcEntryDft = roots[i];
						break;
					}
				}
				if (srcEntryDft != null)
					initialValues.packageFragmentRoot = srcEntryDft;
				else if (initialValues.packageName != null
						&& initialValues.packageName.length() > 0) {
					initialValues.packageFragmentRoot = javaProject
							.getPackageFragmentRoot(project
									.getFolder(initialValues.packageName));
				}
				if (initialValues.packageFragment == null
						&& initialValues.packageFragmentRoot != null
						&& initialValues.packageName != null
						&& initialValues.packageName.length() > 0) {
					IFolder packageFolder = project
							.getFolder(initialValues.packageName);
					initialValues.packageFragment = initialValues.packageFragmentRoot
							.getPackageFragment(packageFolder
									.getProjectRelativePath().toOSString());
				}
			}
			//			superclass and interface
			if (attInfo == null) {
				initialValues.superClassName = "org.eclipse.core.runtime.Plugin";
				IPluginImport[] imports = model.getPluginBase().getImports();
				for (int i = 0; i < imports.length; i++) {
					if (imports[i].getId().equals("org.eclipse.ui")) {
						initialValues.superClassName = "org.eclipse.ui.plugin.AbstractUIPlugin";
						break;
					}
				}
				initialValues.superClassType = findTypeForName(initialValues.superClassName);
				return;
			}
			String schemaBasedOn = null;
			schemaBasedOn = attInfo.getBasedOn();
			if (schemaBasedOn == null || schemaBasedOn.length() == 0) {
				initialValues.superClassName = "java.lang.Object";
				initialValues.superClassType = findTypeForName(initialValues.superClassName);
				return;
			}
			int del = schemaBasedOn.indexOf(':');
			if (del != -1) {
				initialValues.superClassName = schemaBasedOn.substring(0, del);
				initialValues.interfaceName = schemaBasedOn.substring(del + 1);
			} else {
				int schemaLoc = schemaBasedOn.lastIndexOf(".");
				if (schemaLoc != -1 && schemaLoc < schemaBasedOn.length()) {
					String name = schemaBasedOn.substring(schemaLoc + 1,
							schemaBasedOn.length());
					if (name.length() > 0 && name.startsWith("I"))
						initialValues.interfaceName = schemaBasedOn;
					else if (name.length() > 0)
						initialValues.superClassName = schemaBasedOn;
				}
			}
			initialValues.superClassType = findTypeForName(initialValues.superClassName);
			if (initialValues.superClassType != null
					&& initialValues.superClassType.isClass()
					&& initialValues.interfaceName != null)
				initialValues.interfaceType = findTypeForName(initialValues.interfaceName);
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.wizards.NewClassWizardPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		// policy: wizards are not allowed to come up with an error message;
		// in this wizard, some fields may need initial validation and thus,
		// potentially start with an error message.
		if (!currentStatus.isOK())
			updateStatus(currentStatus);
	}
}