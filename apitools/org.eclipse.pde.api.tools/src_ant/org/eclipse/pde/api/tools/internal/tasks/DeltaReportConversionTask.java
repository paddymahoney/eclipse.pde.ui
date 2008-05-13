/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.jdt.core.Signature;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.icu.text.MessageFormat;

public class DeltaReportConversionTask extends Task {
	static class Entry {
		/*<delta
		 *  compatible="true"
		 *  componentId="org.eclipse.equinox.p2.ui_0.1.0"
		 *  element_type="CLASS_ELEMENT_TYPE"
		 *  flags="25"
		 *  key="schedule(Lorg/eclipse/equinox/internal/provisional/p2/ui/operations/ProvisioningOperation;Lorg/eclipse/swt/widgets/Shell;I)Lorg/eclipse/core/runtime/jobs/Job;"
		 *  kind="ADDED"
		 *  modifiers="9"
		 *  restrictions="0"
		 *  type_name="org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner"/>
		 */
		String elementType;
		int flags;
		String key;
		String kind;
		String typeName;
		String[] arguments;

		public Entry(
				String elementType, int flags, String key, String kind,
				String typeName,
				String[] arguments) {
			this.elementType = elementType;
			this.flags = flags;
			this.key = key.replace('/', '.');
			this.kind = kind;
			if (typeName != null) {
				this.typeName = typeName.replace('/', '.');
			}
			this.arguments = arguments;
		}
		
		public String getDisplayString() {
			StringBuffer buffer = new StringBuffer();
			if(this.typeName != null && this.typeName.length() != 0) {
				buffer.append(this.typeName);
				switch(this.flags) {
					case IDelta.METHOD :
					case IDelta.METHOD_WITH_DEFAULT_VALUE :
					case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
						int indexOf = key.indexOf('(');
						if (indexOf == -1) {
							return null;
						}
						int index = indexOf;
						String selector = key.substring(0, index);
						String descriptor = key.substring(index, key.length());
						buffer.append('#');
						buffer.append(Signature.toString(descriptor, selector, null, false, true));
						break;
					case IDelta.CONSTRUCTOR :
						indexOf = key.indexOf('(');
						if (indexOf == -1) {
							return null;
						}
						index = indexOf;
						selector = key.substring(0, index);
						descriptor = key.substring(index, key.length());
						buffer.append('#');
						buffer.append(Signature.toString(descriptor, selector, null, false, false));
						break;
					case IDelta.FIELD :
						buffer.append('#');
						buffer.append(this.key);
						break;
					case IDelta.TYPE_MEMBER :
						buffer.append('.');
						buffer.append(this.key);
						break;
				}
			} else {
				switch(this.flags) {
					case IDelta.MAJOR_VERSION :
						buffer.append(MessageFormat.format("The major version has been changed (from {1} to {2})", this.arguments)); //$NON-NLS-1$
						break;
					case IDelta.MINOR_VERSION :
						buffer.append(MessageFormat.format("The minor version has been changed (from {1} to {2})", this.arguments)); //$NON-NLS-1$
						break;
				}
			}
			return String.valueOf(buffer);
		}
	}
	static final class ConverterDefaultHandler extends DefaultHandler {
		private Map map;
		private boolean debug;
		private String[] arguments;
		private String elementType;
		private int flags;
		private String kind;
		private String typename;
		private String key;
		private String componentID;
		private List argumentsList;

		public ConverterDefaultHandler(boolean debug) {
			this.map = new HashMap();
			this.debug = debug;
		}
		public void startElement(String uri, String localName,
				String name, Attributes attributes) throws SAXException {
			if (debug) {
				System.out.println("name : " + name); //$NON-NLS-1$
				/*<delta
				 *  compatible="true"
				 *  componentId="org.eclipse.equinox.p2.ui_0.1.0"
				 *  element_type="CLASS_ELEMENT_TYPE"
				 *  flags="25"
				 *  key="schedule(Lorg/eclipse/equinox/internal/provisional/p2/ui/operations/ProvisioningOperation;Lorg/eclipse/swt/widgets/Shell;I)Lorg/eclipse/core/runtime/jobs/Job;"
				 *  kind="ADDED"
				 *  modifiers="9"
				 *  restrictions="0"
				 *  type_name="org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner"/>
				 */
				printAttribute(attributes, IApiXmlConstants.ATTR_NAME_COMPATIBLE);
				printAttribute(attributes, IApiXmlConstants.ATTR_NAME_COMPONENT_ID);
				printAttribute(attributes, IApiXmlConstants.ATTR_NAME_ELEMENT_TYPE);
				printAttribute(attributes, IApiXmlConstants.ATTR_NAME_FLAGS);
				printAttribute(attributes, IApiXmlConstants.ATTR_NAME_KEY);
				printAttribute(attributes, IApiXmlConstants.ATTR_NAME_MODIFIERS);
				printAttribute(attributes, IApiXmlConstants.ATTR_NAME_RESTRICTIONS);
				printAttribute(attributes, IApiXmlConstants.ATTR_NAME_TYPE_NAME);
			}
			if (IApiXmlConstants.DELTA_ELEMENT_NAME.equals(name)) {
				componentID = attributes.getValue(IApiXmlConstants.ATTR_NAME_COMPONENT_ID);
				
				elementType = attributes.getValue(IApiXmlConstants.ATTR_NAME_ELEMENT_TYPE);
				flags = Integer.parseInt(attributes.getValue(IApiXmlConstants.ATTR_NAME_FLAGS));
				kind = attributes.getValue(IApiXmlConstants.ATTR_NAME_KIND);
				typename = attributes.getValue(IApiXmlConstants.ATTR_NAME_TYPE_NAME);
				key = attributes.getValue(IApiXmlConstants.ATTR_NAME_KEY);
			} else if (IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENTS.equals(name)) {
				if (this.argumentsList == null) {
					this.argumentsList = new ArrayList();
				} else {
					this.argumentsList.clear();
				}
			} else if (IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENT.equals(name)) {
				this.argumentsList.add(attributes.getValue(IApiXmlConstants.ATTR_VALUE));
			}
		}

		public void endElement(String uri, String localName, String name)
			throws SAXException {
			if (IApiXmlConstants.DELTA_ELEMENT_NAME.equals(name)) {
				Entry entry = new Entry(
						elementType,
						flags,
						key,
						kind,
						typename,
						this.arguments);
				Object object = this.map.get(this.componentID);
				if (object != null) {
					((List) object).add(entry);
				} else {
					ArrayList value = new ArrayList();
					value.add(entry);
					this.map.put(componentID, value);
				}
			} else if (IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENTS.equals(name)) {
				if (this.argumentsList != null && this.argumentsList.size() != 0) {
					this.arguments = new String[this.argumentsList.size()];
					this.argumentsList.toArray(this.arguments);
				}
			}
		}
		public Map getEntries() {
			return this.map;
		}

		private void printAttribute(Attributes attributes, String name) {
			System.out.println("\t" + name + " = " + String.valueOf(attributes.getValue(name))); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	boolean debug;

	private String xmlFileLocation;
	private String htmlFileLocation;

	public void setXmlFile(String xmlFileLocation) {
		this.xmlFileLocation = xmlFileLocation;
	}
	public void setHtmlFile(String htmlFileLocation) {
		this.htmlFileLocation = htmlFileLocation;
	}
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue); 
	}
	public void execute() throws BuildException {
		if (this.debug) {
			System.out.println("xmlFileLocation : " + this.xmlFileLocation); //$NON-NLS-1$
			System.out.println("htmlFileLocation : " + this.htmlFileLocation); //$NON-NLS-1$
		}
		if (this.xmlFileLocation == null) {
			throw new BuildException("Missing one argument"); //$NON-NLS-1$
		}
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = null;
		try {
			parser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		if (parser == null) {
			throw new BuildException("Could not create a sax parser"); //$NON-NLS-1$
		}

		File file = new File(this.xmlFileLocation);
		if (this.htmlFileLocation == null) {
			this.htmlFileLocation = extractNameFromXMLName();
			if (this.debug) {
				System.out.println("output name :" + this.htmlFileLocation); //$NON-NLS-1$
			}
		}
		try {
			ConverterDefaultHandler defaultHandler = new ConverterDefaultHandler(this.debug);
			parser.parse(file, defaultHandler);
			StringBuffer buffer = new StringBuffer();
			dumpEntries(defaultHandler, buffer);
			writeOutput(buffer);
		} catch (SAXException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		}
	}

	private void writeOutput(StringBuffer buffer) throws IOException {
		FileWriter writer = null;
		BufferedWriter bufferedWriter = null;
		try {
			writer = new FileWriter(this.htmlFileLocation);
			bufferedWriter = new BufferedWriter(writer);
			bufferedWriter.write(String.valueOf(buffer));
		} finally {
			if (bufferedWriter != null) {
				bufferedWriter.close();
			}
		}
	}
	private void dumpEntries(ConverterDefaultHandler defaultHandler, StringBuffer buffer) {
		dumpHeader(buffer);
		Map entries = defaultHandler.getEntries();
		Set entrySet = entries.entrySet();
		List allEntries = new ArrayList();
		for (Iterator iterator = entrySet.iterator(); iterator.hasNext(); ) {
			allEntries.add(iterator.next());
		}
		Collections.sort(allEntries, new Comparator() {
			public int compare(Object o1, Object o2) {
				Map.Entry entry1 = (Map.Entry) o1;
				Map.Entry entry2 = (Map.Entry) o2;
				return ((String) entry1.getKey()).compareTo(entry2.getKey());
			}
		});
		for (Iterator iterator = allEntries.iterator(); iterator.hasNext(); ) {
			Map.Entry mapEntry = (Map.Entry) iterator.next();
			String key = (String) mapEntry.getKey();
			Object value = mapEntry.getValue();
			dumpEntryForComponent(buffer, key);
			if (value instanceof List) {
				List values = (List) value;
				Collections.sort(values, new Comparator() {
					public int compare(Object o1, Object o2) {
						Entry entry1 = (Entry) o1;
						Entry entry2 = (Entry) o2;
						String typeName1 = entry1.typeName;
						String typeName2 = entry2.typeName;
						if (typeName1 == null) {
							if (typeName2 == null) {
								return entry1.key.compareTo(entry2.key);
							}
							return -1;
						} else if (typeName2 == null) {
							return 1;
						}
						if (!typeName1.equals(typeName2)) {
							return typeName1.compareTo(typeName2);
						}
						return entry1.key.compareTo(entry2.key);
					}
				});
				if (debug) {
					System.out.println("Entries for " + key); //$NON-NLS-1$
				}
				for (Iterator iterator2 = ((List)value).iterator(); iterator2.hasNext(); ) {
					Entry entry = (Entry) iterator2.next();
					if (debug) {
						if (entry.typeName != null) {
							System.out.print(entry.typeName);
						}
						System.out.println(entry.key);
					}
					dumpEntry(buffer, entry);
				}
			}
			dumpEndEntryForComponent(buffer, key);
		}
		dumpFooter(buffer);
	}
	private void dumpEntry(StringBuffer buffer, Entry entry) {
		buffer.append(NLS.bind(Messages.entry, entry.getDisplayString()));
	}
	private void dumpEntryForComponent(StringBuffer buffer, String componentID) {
		buffer.append(NLS.bind(Messages.componentEntry, componentID));
	}
	private void dumpFooter(StringBuffer buffer) {
		buffer.append(Messages.footer);
	}
	private void dumpHeader(StringBuffer buffer) {
		buffer.append(Messages.header);
	}
	private void dumpEndEntryForComponent(StringBuffer buffer, String componentID) {
		buffer.append(NLS.bind(Messages.endComponentEntry, componentID));
	}
	private String extractNameFromXMLName() {
		int index = this.xmlFileLocation.lastIndexOf('.');
		StringBuffer buffer = new StringBuffer();
		buffer.append(this.xmlFileLocation.substring(0, index)).append(".html"); //$NON-NLS-1$
		return String.valueOf(buffer);
	}
}
