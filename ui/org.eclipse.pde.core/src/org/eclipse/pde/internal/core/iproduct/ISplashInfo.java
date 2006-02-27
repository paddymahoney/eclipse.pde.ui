/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;


public interface ISplashInfo extends IProductObject {
	
	public static final String P_LOCATION = "location"; //$NON-NLS-1$
	public static final String P_PROGRESS_GEOMETRY= "startupProgressRect"; //$NON-NLS-1$
	public static final String P_MESSAGE_GEOMETRY = "startupMessageRect"; //$NON-NLS-1$
	public static final String P_FOREGROUND_COLOR = "startupForegroundColor"; //$NON-NLS-1$
	public static final String P_PROPERTY = "property"; //$NON-NLS-1$
	public static final String P_PROPERTY_NAME = "name"; //$NON-NLS-1$
	public static final String P_PROPERTY_VALUE = "value"; //$NON-NLS-1$
	
	void setLocation(String location);
	
	String getLocation();
	
	void addProgressBar(boolean add, boolean blockNotification);
	
	/**
	 * 
	 * @param geo array of length 4 where geo[0] = x
	 * 									  geo[1] = y
	 * 									  geo[1] = width
	 * 									  geo[1] = height
	 */
	void setProgressGeometry(int[] geo);
	
	int[] getProgressGeometry();
	
	void addProgressMessage(boolean add, boolean blockNotification);
	
	/**
	 * 
	 * @param geo array of length 4 where geo[0] = x
	 * 									  geo[1] = y
	 * 									  geo[1] = width
	 * 									  geo[1] = height
	 */
	void setMessageGeometry(int[] geo);
	
	int[] getMessageGeometry();
	
	void setForegroundColor(String hexColor) throws IllegalArgumentException;
	
	String getForegroundColor();
}
