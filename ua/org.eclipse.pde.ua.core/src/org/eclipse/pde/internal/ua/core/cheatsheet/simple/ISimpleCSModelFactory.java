/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.cheatsheet.simple;

import org.eclipse.pde.internal.core.text.IDocumentNodeFactory;

public interface ISimpleCSModelFactory extends IDocumentNodeFactory {

	public ISimpleCS createSimpleCS();

	public ISimpleCSAction createSimpleCSAction(ISimpleCSObject parent);

	public ISimpleCSCommand createSimpleCSCommand(ISimpleCSObject parent);

	public ISimpleCSConditionalSubItem createSimpleCSConditionalSubItem(
			ISimpleCSObject parent);

	public ISimpleCSIntro createSimpleCSIntro(ISimpleCSObject parent);

	public ISimpleCSItem createSimpleCSItem(ISimpleCSObject parent);

	public ISimpleCSOnCompletion createSimpleCSOnCompletion(
			ISimpleCSObject parent);

	public ISimpleCSPerformWhen createSimpleCSPerformWhen(ISimpleCSObject parent);

	public ISimpleCSRepeatedSubItem createSimpleCSRepeatedSubItem(
			ISimpleCSObject parent);

	public ISimpleCSSubItem createSimpleCSSubItem(ISimpleCSObject parent);

	public ISimpleCSDescription createSimpleCSDescription(ISimpleCSObject parent);
}
