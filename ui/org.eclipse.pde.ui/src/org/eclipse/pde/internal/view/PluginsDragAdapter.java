package org.eclipse.pde.internal.view;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.core.FileAdapter;
import java.util.*;

public class PluginsDragAdapter extends DragSourceAdapter {
	ISelectionProvider selectionProvider;

	/**
	 * NavigatorDragAction constructor comment.
	 */
	public PluginsDragAdapter(ISelectionProvider provider) {
		selectionProvider = provider;
	}

	/**
	 * Returns the data to be transferred in a drag and drop
	 * operation.
	 */
	public void dragSetData(DragSourceEvent event) {
		DragSource dragSource = (DragSource) event.widget;
		Control control = dragSource.getControl();
		Shell shell = control.getShell();

		//resort to a file transfer
		if (!FileTransfer.getInstance().isSupportedType(event.dataType))
			return;

		FileAdapter[] files = getSelectedFiles();

		// Get the path of each file and set as the drag data
		final int len = files.length;
		String[] fileNames = new String[len];
		for (int i = 0, length = len; i < length; i++) {
			fileNames[i] = files[i].getFile().getAbsolutePath();
		}
		event.data = fileNames;
	}
	/**
	 * All selection must be files or folders.
	 */
	public void dragStart(DragSourceEvent event) {

		// Workaround for 1GEUS9V
		DragSource dragSource = (DragSource) event.widget;
		Control control = dragSource.getControl();
		if (control != control.getDisplay().getFocusControl()) {
			event.doit = false;
			return;
		}
		
		FileAdapter [] files = getSelectedFiles();
		
		if (files.length==0) {
			event.doit = false;
			return;
		}
		event.doit = true;
	}
	private FileAdapter [] getSelectedFiles() {
		IStructuredSelection selection = (IStructuredSelection)selectionProvider.getSelection();
		ArrayList files = new ArrayList();
		for (Iterator iter=selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof FileAdapter)
				files.add(obj);
			else
				return new FileAdapter[0];
		}
		return (FileAdapter[])files.toArray(new FileAdapter[files.size()]);
	}
}