/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.ui.actions;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.ui.IJavaDebugUIConstants;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.IClasspathViewer;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * Adds a project to the runtime class path.
 */
public class AddProjectAction extends RuntimeClasspathAction {



	public AddProjectAction(IClasspathViewer viewer) {
		super(ActionMessages.AddProjectAction_Add_Project_1, viewer);
	}

	/**
	 * Prompts for a project to add.
	 *
	 * @see IAction#run()
	 */
	@Override
	public void run() {
		List<IJavaProject> projects = getPossibleAdditions();
		ProjectSelectionDialog dialog= new ProjectSelectionDialog(getShell(),projects);
		dialog.setTitle(ActionMessages.AddProjectAction_Project_Selection_2);
		MultiStatus status = new MultiStatus(JDIDebugUIPlugin.getUniqueIdentifier(), IJavaDebugUIConstants.INTERNAL_ERROR, "One or more exceptions occurred while adding projects.", null);  //$NON-NLS-1$

		if (dialog.open() == Window.OK) {
			Object[] selections = dialog.getResult();

			List<IJavaProject> additions = new ArrayList<>(selections.length);
			try {
				for (Object selection : selections) {
					IJavaProject jp = (IJavaProject) selection;
					if (dialog.isAddRequiredProjects()) {
						collectRequiredProjects(jp, additions);
					} else {
						additions.add(jp);
					}
				}
			} catch (JavaModelException e) {
				status.add(e.getStatus());
			}

			List<IRuntimeClasspathEntry> runtimeEntries = new ArrayList<>(additions.size());
			Iterator<IJavaProject> iter = additions.iterator();
			IRuntimeClasspathEntry[] addedEntries = getViewer().getEntries();
			while (iter.hasNext()) {
				IJavaProject jp = iter.next();
				if (isProjectAdded(jp, addedEntries)) {
					continue;
				}
				runtimeEntries.add(JavaRuntime.newProjectRuntimeClasspathEntry(jp));
				if (dialog.isAddExportedEntries()) {
					try {
						collectExportedEntries(jp, runtimeEntries);
					} catch (CoreException e) {
						status.add(e.getStatus());
					}
				}
			}
			IRuntimeClasspathEntry[] entries = runtimeEntries.toArray(new IRuntimeClasspathEntry[runtimeEntries.size()]);
			getViewer().addEntries(entries);
		}

		if (!status.isOK()) {
			JDIDebugUIPlugin.statusDialog(status);
		}
	}

	private boolean isProjectAdded(IJavaProject project, IRuntimeClasspathEntry[] addedEntries) {
		for (IRuntimeClasspathEntry entry : addedEntries) {
			if (entry.getType() == IRuntimeClasspathEntry.PROJECT) {
				IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(entry.getPath());
				if (res.getLocationURI().equals(project.getResource().getLocationURI())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @see SelectionListenerAction#updateSelection(IStructuredSelection)
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		return getViewer().updateSelection(getActionType(), selection) && !getPossibleAdditions().isEmpty();
	}

	@Override
	protected int getActionType() {
		return ADD;
	}

	/**
	 * Returns the possible projects that can be added
	 * @return the list of projects
	 */
	protected List<IJavaProject> getPossibleAdditions() {
		IJavaProject[] projects;
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		try {
			projects= JavaCore.create(root).getJavaProjects();
		} catch (JavaModelException e) {
			JDIDebugUIPlugin.log(e);
			projects= new IJavaProject[0];
		}
		List<IJavaProject> remaining = new ArrayList<>();
		for (IJavaProject project : projects) {
			remaining.add(project);
		}
		List<IJavaProject> alreadySelected = new ArrayList<>();
		for (IRuntimeClasspathEntry entry : getViewer().getEntries()) {
			if (entry.getType() == IRuntimeClasspathEntry.PROJECT) {
				IResource res = root.findMember(entry.getPath());
				IJavaProject jp = (IJavaProject)JavaCore.create(res);
				alreadySelected.add(jp);
			}
		}
		remaining.removeAll(alreadySelected);
		return remaining;
	}

	/**
	 * Adds all projects required by <code>proj</code> to the list
	 * <code>res</code>
	 *
	 * @param proj the project for which to compute required
	 *  projects
	 * @param res the list to add all required projects too
	 * @throws JavaModelException if there is a problem accessing the Java model
	 */
	protected void collectRequiredProjects(IJavaProject proj, List<IJavaProject> res) throws JavaModelException {
		if (!res.contains(proj)) {
			res.add(proj);

			IJavaModel model= proj.getJavaModel();

			for (IClasspathEntry curr : proj.getRawClasspath()) {
				if (curr.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
					IJavaProject ref= model.getJavaProject(curr.getPath().segment(0));
					if (ref.exists()) {
						collectRequiredProjects(ref, res);
					}
				}
			}
		}
	}

	/**
	 * Adds all exported entries defined by <code>proj</code> to the list
	 * <code>runtimeEntries</code>.
	 *
	 * @param proj the project
	 * @param runtimeEntries the entries
	 * @throws CoreException if an exception occurs
	 */
	protected void collectExportedEntries(IJavaProject proj, List<IRuntimeClasspathEntry> runtimeEntries) throws CoreException {
		for (IClasspathEntry entry : proj.getRawClasspath()) {
			if (entry.isExported()) {
				IRuntimeClasspathEntry rte = null;
				switch (entry.getEntryKind()) {
				case IClasspathEntry.CPE_CONTAINER:
					IClasspathContainer container = JavaCore.getClasspathContainer(entry.getPath(), proj);
					int kind = 0;
					switch (container.getKind()) {
					case IClasspathContainer.K_APPLICATION:
						kind = IRuntimeClasspathEntry.USER_CLASSES;
						break;
					case IClasspathContainer.K_SYSTEM:
						kind = IRuntimeClasspathEntry.BOOTSTRAP_CLASSES;
						break;
					case IClasspathContainer.K_DEFAULT_SYSTEM:
						kind = IRuntimeClasspathEntry.STANDARD_CLASSES;
						break;
					}
					rte = JavaRuntime.newRuntimeContainerClasspathEntry(entry.getPath(), kind, proj);
					break;
				case IClasspathEntry.CPE_LIBRARY:
					rte = JavaRuntime.newArchiveRuntimeClasspathEntry(entry.getPath());
					rte.setSourceAttachmentPath(entry.getSourceAttachmentPath());
					rte.setSourceAttachmentRootPath(entry.getSourceAttachmentRootPath());
					break;
				case IClasspathEntry.CPE_PROJECT:
					String name = entry.getPath().segment(0);
					IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
					if (p.exists()) {
						IJavaProject jp = JavaCore.create(p);
						if (jp.exists()) {
							rte = JavaRuntime.newProjectRuntimeClasspathEntry(jp);
						}
					}
					break;
				case IClasspathEntry.CPE_VARIABLE:
					rte = JavaRuntime.newVariableRuntimeClasspathEntry(entry.getPath());
					break;
				default:
					break;
				}
				if (rte != null) {
					if (!runtimeEntries.contains(rte)) {
						runtimeEntries.add(rte);
					}
				}
			}
		}
	}
}
