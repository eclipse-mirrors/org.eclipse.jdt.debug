package org.eclipse.jdt.internal.debug.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.internal.core.Breakpoint;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.*;

import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.MethodEntryRequest;

public abstract class JavaBreakpoint extends Breakpoint implements IJavaBreakpoint {

	// Thread and marker label resource String keys
	protected static final String THREAD_PREFIX= "jdi_thread.";
	protected static final String MARKER_PREFIX= "jdi_marker.";
	protected static final String LABEL= "label.";	
	protected static final String THREAD_LABEL= THREAD_PREFIX + LABEL;
	protected static final String MARKER_LABEL= MARKER_PREFIX + LABEL;
	/**
	 * JavaBreakpoint attributes
	 */	
	protected static final String[] fgExpiredEnabledAttributes= new String[]{IJavaDebugConstants.EXPIRED, IDebugConstants.ENABLED};
	
	public JavaBreakpoint() {
	}	
	
	public JavaBreakpoint(IMarker marker) {
		super(marker);
		setMarker(marker);
	}

	public void setMarker(IMarker marker) {
		super.setMarker(marker);
		configureAtStartup();
	}

	/**
	 * @see Breakpoint#addToTarget(IDebugTarget)
	 */
	public void addToTarget(IDebugTarget target) {
		if (target instanceof JDIDebugTarget) {
			addToTarget((JDIDebugTarget) target);
		}
	}	

	/**
	 * @see Breakpoint#changeForTarget
	 */
	public void changeForTarget(IDebugTarget target) {
		if (target instanceof JDIDebugTarget) {
			changeForTarget((JDIDebugTarget) target);
		}
	}
	
	/**
	 * @see Breakpoint#removeFromTarget(IDebugTarget)
	 */
	public void removeFromTarget(IDebugTarget target) {
		if (target instanceof JDIDebugTarget) {
			removeFromTarget((JDIDebugTarget) target);
		}
	}

	/**
	 * This breakpoint has been removed.
	 * Remove the request from the given target.
	 */
	public abstract void removeFromTarget(JDIDebugTarget target);

	/**
	 * Update the enabled state of the given request, which is associated
	 * with this breakpoint. Set the enabled state of the request
	 * to the enabled state of this breakpoint.
	 */
	protected void updateEnabledState(EventRequest request)  {
		updateEnabledState(request, this.isEnabled());
	}

	/**
	 * Set the enabled state of the given request as specified by the enabled
	 * parameter
	 */
	protected void updateEnabledState(EventRequest request, boolean enabled) {
		if (request.isEnabled() != enabled) {
			// change the enabled state
			try {
				// if the request has expired, and is not a method entry request, do not disable.
				// BreakpointRequests that have expired cannot be deleted. However method entry 
				// requests that are expired can be deleted (since we simulate the hit count)
				if (request instanceof MethodEntryRequest || !isExpired(request)) {
					request.setEnabled(enabled);
				}
			} catch (VMDisconnectedException e) {
			} catch (RuntimeException e) {
				logError(e);
			}
		}
	}
	
	/**
	 * Return whether this breakpoint is enabled
	 */
	public boolean isEnabled() {
		try {
			return super.isEnabled();
		} catch (CoreException ce) {
			logError(ce);
			return false;
		}
	}
	
	/**
	 * Returns whether this breakpoint has expired.
	 */
	public boolean isExpired() {
		return getBooleanAttribute(IJavaDebugConstants.EXPIRED);
	}	
	
	/**
	 * Returns whether the given request is expired
	 */
	protected boolean isExpired(EventRequest request) {
		Boolean requestExpired= (Boolean) request.getProperty(IJavaDebugConstants.EXPIRED);
		if (requestExpired == null) {
				return false;
		}
		return requestExpired.booleanValue();
	}
	
	/**
	 * Enable this breakpoint
	 */
	public void enable() {
		try {
			super.enable();
		} catch (CoreException ce) {
			logError(ce);
		}
	}
	
	/**
	 * Disable this breakpoint
	 */
	public void disable() {
		try {
			super.disable();
		} catch (CoreException ce) {
			logError(ce);
		}
	}
	
	/**
	 * @see IJavaBreakpoint#isInstalled()
	 */
	public boolean isInstalled() {
		return getAttribute(IJavaDebugConstants.INSTALL_COUNT, 0) > 0;
	}	
	
	/**
	 * Increments the install count on this breakpoint
	 */
	public void incrementInstallCount() throws CoreException {
		int count = getInstallCount();
		setAttribute(IJavaDebugConstants.INSTALL_COUNT, count + 1);
	}	
	
	/**
	 * Returns the <code>INSTALL_COUNT</code> attribute of this breakpoint
	 * or 0 if the attribute is not set.
	 */
	public int getInstallCount() {
		return getAttribute(IJavaDebugConstants.INSTALL_COUNT, 0);
	}	

	/**
	 * @see IJavaBreakpoint
	 */
	public void decrementInstallCount() throws CoreException {
		int count= getInstallCount();
		if (count > 0) {
			setAttribute(IJavaDebugConstants.INSTALL_COUNT, count - 1);	
		}
		if (count == 1) {
			if (isExpired()) {
				// if breakpoint was auto-disabled, re-enable it
				setAttributes(fgExpiredEnabledAttributes,
						new Object[]{Boolean.FALSE, Boolean.TRUE});
			}
		}
	}

	/**
	 * Sets the <code>TYPE_HANDLE</code> attribute of the given breakpoint, associated
	 * with the given IType.
	 */
	public void setType(IType type) throws CoreException {
		String handle = type.getHandleIdentifier();
		setTypeHandleIdentifier(handle);
	}
	
	/**
	 * Sets the <code>TYPE_HANDLE</code> attribute of the given breakpoint.
	 */
	public void setTypeHandleIdentifier(String identifier) throws CoreException {
		setAttribute(IJavaDebugConstants.TYPE_HANDLE, identifier);
	}
	
	/**
	 * @see IJavaBreakpoint#getInstalledType()
	 */
	public IType getInstalledType() {
		try {
			String handle = getTypeHandleIdentifier();
			if (handle != null) {
				return (IType)JavaCore.create(handle);
			}
		} catch (CoreException e) {
			logError(e);
		}
		return null;
	}	
	
	/**
	 * Returns the <code>TYPE_HANDLE</code> attribute of the given breakpoint.
	 */
	public String getTypeHandleIdentifier() throws CoreException {
		return (String)getAttribute(IJavaDebugConstants.TYPE_HANDLE);
	}	
	
	/**
	 * Returns the top-level type name associated with the type 
	 * the given breakpoint is associated with, or <code>null</code>.
	 */
	public String getTopLevelTypeName() {
		IType type = getInstalledType();
		if (type != null) {
			while (type.getDeclaringType() != null) {
				type = type.getDeclaringType();
			}
			return type.getFullyQualifiedName();
		}
		return null;
	}
	
	/**
	 * @see IJavaBreakpoint
	 */
	public String getThreadText(String threadName, boolean qualified, boolean systemThread) {
		String typeName= getBreakpointTypeName(qualified);
		return getFormattedThreadText(threadName, typeName, systemThread);			
	}
	
	/**
	 * @see IJavaBreakpoint#getThreadText
	 */
	protected abstract String getFormattedThreadText(String threadName, String typeName, boolean systemThread);
	
	public String getBreakpointTypeName(boolean qualified) {
		String typeName= "";
		typeName= getInstalledType().getFullyQualifiedName();
		if (!qualified) {
			int index= typeName.lastIndexOf('.');
			if (index != -1) {
				typeName= typeName.substring(index + 1);
			}
		}
		return typeName;
	}		
	
	/**
	 * Returns the identifier for this JDI debug model plug-in
	 *
	 * @return plugin identifier
	 */
	public static String getPluginIdentifier() {
		return JDIDebugPlugin.getDefault().getDescriptor().getUniqueIdentifier();
	}	

	/**
	 * Plug in the single argument to the resource String for the key to get a formatted resource String
	 */
	public static String getFormattedString(String key, String arg) {
		return getFormattedString(key, new String[] {arg});
	}

	/**
	 * Plug in the arguments to the resource String for the key to get a formatted resource String
	 */
	public static String getFormattedString(String key, String[] args) {
		return DebugJavaUtils.getFormattedString(key, args);
	}
	
	/**
	 * Convenience method to log internal errors
	 */
	protected void logError(Exception e) {
		DebugJavaUtils.logError(e);
	}	
	
	protected void run(IWorkspaceRunnable wr) throws DebugException {
		try {
			ResourcesPlugin.getWorkspace().run(wr, null);
		} catch (CoreException e) {
			throw new DebugException(e.getStatus());
		}			
	}
	
	/**
	 * Resets the install count attribute on this breakpoint's marker
	 * to "0".  Resets the expired attribute on all breakpoint markers to <code>false</code>.
	 * Resets the enabled attribute on the breakpoint marker to <code>true</code>.
	 * If a workbench crashes, the attributes could have been persisted
	 * in an incorrect state.
	 */
	private void configureAtStartup() {
		List attributes= new ArrayList(3);
		List values= new ArrayList(3);
		if (isInstalled()) {
			attributes.add(IJavaDebugConstants.INSTALL_COUNT);
			values.add(new Integer(0));
		}
		if (isExpired()) {
			// if breakpoint was auto-disabled, re-enable it
			attributes.add(IJavaDebugConstants.EXPIRED);
			values.add(Boolean.FALSE);
			attributes.add(IDebugConstants.ENABLED);
			values.add(Boolean.TRUE);
		}
		if (!attributes.isEmpty()) {
			String[] strAttributes= new String[attributes.size()];
			try {
				setAttributes((String[])attributes.toArray(strAttributes), values.toArray());
			} catch (CoreException ce) {
				logError(ce);
			}
		}
	}	

	/**
	 * Add this breakpoint to the breakpoint manager
	 */
	protected void addToBreakpointManager() throws DebugException {
		getBreakpointManager().addBreakpoint(this);
	}

	/**
	 * Returns the breakpoint manager
	 */
	protected IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}

}

