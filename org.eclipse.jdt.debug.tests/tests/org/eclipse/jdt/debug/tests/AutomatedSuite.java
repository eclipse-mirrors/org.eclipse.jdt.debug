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
package org.eclipse.jdt.debug.tests;

import java.util.Enumeration;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.eclipse.jdt.debug.tests.core.ArchiveSourceLookupTests;
import org.eclipse.jdt.debug.tests.core.BootpathTests;
import org.eclipse.jdt.debug.tests.core.BreakpointListenerTests;
import org.eclipse.jdt.debug.tests.core.ClasspathContainerTests;
import org.eclipse.jdt.debug.tests.core.ClasspathProviderTests;
import org.eclipse.jdt.debug.tests.core.ClasspathVariableTests;
import org.eclipse.jdt.debug.tests.core.CommandArgumentTests;
import org.eclipse.jdt.debug.tests.core.ConditionalBreakpointsTests;
import org.eclipse.jdt.debug.tests.core.DebugEventTests;
import org.eclipse.jdt.debug.tests.core.DeferredBreakpointTests;
import org.eclipse.jdt.debug.tests.core.DirectorySourceLookupTests;
import org.eclipse.jdt.debug.tests.core.EventSetTests;
import org.eclipse.jdt.debug.tests.core.ExceptionBreakpointTests;
import org.eclipse.jdt.debug.tests.core.HcrTests;
import org.eclipse.jdt.debug.tests.core.HitCountBreakpointsTests;
import org.eclipse.jdt.debug.tests.core.InstanceFilterTests;
import org.eclipse.jdt.debug.tests.core.InstanceVariableTests;
import org.eclipse.jdt.debug.tests.core.JavaBreakpointListenerTests;
import org.eclipse.jdt.debug.tests.core.LaunchConfigurationTests;
import org.eclipse.jdt.debug.tests.core.LaunchModeTests;
import org.eclipse.jdt.debug.tests.core.LaunchVariableTests;
import org.eclipse.jdt.debug.tests.core.LocalVariableTests;
import org.eclipse.jdt.debug.tests.core.MethodBreakpointTests;
import org.eclipse.jdt.debug.tests.core.MiscBreakpointsTests;
import org.eclipse.jdt.debug.tests.core.PatternBreakpointTests;
import org.eclipse.jdt.debug.tests.core.ProcessTests;
import org.eclipse.jdt.debug.tests.core.RefreshTabTests;
import org.eclipse.jdt.debug.tests.core.RemoteAttachTests;
import org.eclipse.jdt.debug.tests.core.RuntimeClasspathEntryTests;
import org.eclipse.jdt.debug.tests.core.SourceLocationTests;
import org.eclipse.jdt.debug.tests.core.SourceLookupTests;
import org.eclipse.jdt.debug.tests.core.StaticVariableTests;
import org.eclipse.jdt.debug.tests.core.StepFilterTests;
import org.eclipse.jdt.debug.tests.core.StepIntoSelectionTests;
import org.eclipse.jdt.debug.tests.core.StringSubstitutionTests;
import org.eclipse.jdt.debug.tests.core.SuspendVMBreakpointsTests;
import org.eclipse.jdt.debug.tests.core.TargetPatternBreakpointTests;
import org.eclipse.jdt.debug.tests.core.ThreadFilterBreakpointsTests;
import org.eclipse.jdt.debug.tests.core.TypeTests;
import org.eclipse.jdt.debug.tests.core.WatchExpressionTests;
import org.eclipse.jdt.debug.tests.core.WatchpointTests;
import org.eclipse.jdt.debug.tests.core.WorkingDirectoryTests;
import org.eclipse.swt.widgets.Display;

/**
 * Test all areas of the JDT Debugger.
 * 
 * To run this test suite:
 * <ol>
 * <li>Create a new Run-time Workbench launch configuration</li>
 * <li>Append "org.eclipse.jdt.debug.tests.AutomatedSuite" to the Program Arguments</li>
 * <li>Set the Application Name to "org.eclipse.jdt.debug.tests.app"</li>
 * <li>Run the launch configuration. Output from the tests will be written to the debug console</li>
 * </ol>
 */
public class AutomatedSuite extends TestSuite {
	
	/**
	 * Flag that indicates test are in progress
	 */
	protected boolean fTesting = true;

	/**
	 * Returns the suite.  This is required to
	 * use the JUnit Launcher.
	 */
	public static Test suite() {
		return new AutomatedSuite();
	}

	/**
	 * Construct the test suite.
	 */
	public AutomatedSuite() {
		addTest(new TestSuite(ProjectCreationDecorator.class));
		
		addTest(new TestSuite(BreakpointListenerTests.class));
	}
	
	/**
	 * Runs the tests and collects their result in a TestResult.
	 * The debug tests cannot be run in the UI thread or the event
	 * waiter blocks the UI when a resource changes.
	 */
	public void run(final TestResult result) {
		final Display display = Display.getCurrent();
		Thread thread = null;
		try {
			Runnable r = new Runnable() {
				public void run() {
					for (Enumeration e= tests(); e.hasMoreElements(); ) {
				  		if (result.shouldStop() )
				  			break;
						Test test= (Test)e.nextElement();
						runTest(test, result);
					}					
					fTesting = false;
					display.wake();
				}
			};
			thread = new Thread(r);
			thread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		while (fTesting) {
			try {
				if (!display.readAndDispatch())
					display.sleep();
			} catch (Throwable e) {
				e.printStackTrace();
			}			
		}		
	}

}

