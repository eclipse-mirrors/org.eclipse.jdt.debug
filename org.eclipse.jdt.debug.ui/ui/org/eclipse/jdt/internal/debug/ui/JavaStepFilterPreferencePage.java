/*
 * (c) Copyright IBM Corp. 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

public class JavaStepFilterPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	private static final String DEFAULT_NEW_FILTER_TEXT = ""; //$NON-NLS-1$

	// Step filter widgets
	private CheckboxTableViewer fFilterViewer;
	private Table fFilterTable;
	private Button fUseFiltersCheckbox;
	private Button fAddPackageButton;
	private Button fAddTypeButton;
	private Button fRemoveFilterButton;
	private Button fAddFilterButton;
	private Button fFilterSyntheticButton;
	private Button fFilterStaticButton;
	private Button fFilterConstructorButton;
	private Text fEditorText;
	private String fInvalidEditorText= null;
	private TableEditor fTableEditor;
	private TableItem fNewTableItem;
	private Filter fNewStepFilter;
	private Label fTableLabel;
	
	private StepFilterContentProvider fStepFilterContentProvider;
	
	public JavaStepFilterPreferencePage() {
		super();
		setPreferenceStore(JDIDebugUIPlugin.getDefault().getPreferenceStore());
	}

	protected Control createContents(Composite parent) {
		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.JAVA_STEP_FILTER_PREFERENCE_PAGE);
		
		//The main composite
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);	
		
		createStepFilterPreferences(composite);
		
		return composite;	
	}

	public void init(IWorkbench workbench) {
	}
	
	/**
	 * Set the default preferences for this page.
	 */
	public static void initDefaults(IPreferenceStore store) {
		store.setDefault(IJDIPreferencesConstants.PREF_ACTIVE_FILTERS_LIST, "java.lang.ClassLoader"); //$NON-NLS-1$
		store.setDefault(IJDIPreferencesConstants.PREF_INACTIVE_FILTERS_LIST, "com.ibm.*,com.sun.*,java.*,javax.*,org.omg.*,sun.*,sunw.*"); //$NON-NLS-1$
		store.setDefault(IJDIPreferencesConstants.PREF_USE_FILTERS, true);
	}
	
	/**
	 * Creates composite control and sets the default layout data.
	 *
	 * @param parent  the parent of the new composite
	 * @param numColumns  the number of columns for the new composite
	 * @param labelText  the text label of the new composite
	 * @return the newly-created composite
	 */
	private Composite createLabelledComposite(Composite parent, int numColumns, String labelText) {
		Composite comp = new Composite(parent, SWT.NONE);
		
		//GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumns;
		comp.setLayout(layout);
		//GridData
		GridData gd= new GridData();
		gd.verticalAlignment = GridData.FILL;
		gd.horizontalAlignment = GridData.FILL;
		comp.setLayoutData(gd);
		
		//Label
		Label label = new Label(comp, SWT.NONE);
		label.setText(labelText);
		gd = new GridData();
		gd.horizontalSpan = numColumns;
		label.setLayoutData(gd);
		return comp;
	}
	
	/**
	 * Create a group to contain the step filter related widgetry
	 */
	private void createStepFilterPreferences(Composite parent) {
		Composite comp = createLabelledComposite(parent, 1, DebugUIMessages.getString("JavaDebugPreferencePage.Step_filters_6")); //$NON-NLS-1$
		
		// top level container
		Composite container = new Composite(comp, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);
		
		// use filters checkbox
		fUseFiltersCheckbox = new Button(container, SWT.CHECK);
		fUseFiltersCheckbox.setText(DebugUIMessages.getString("JavaDebugPreferencePage.Use_&step_filters_7")); //$NON-NLS-1$
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 2;
		fUseFiltersCheckbox.setLayoutData(gd);	
		fUseFiltersCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				toggleStepFilterWidgetsEnabled(fUseFiltersCheckbox.getSelection());
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});	
		
		//table label
		fTableLabel= new Label(container, SWT.NONE);
		fTableLabel.setText(DebugUIMessages.getString("JavaDebugPreferencePage.Defined_step_fi&lters__8")); //$NON-NLS-1$
		gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 2;
		fTableLabel.setLayoutData(gd);
		
		// filter table
		fFilterTable= new Table(container, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		
		TableLayout tableLayout= new TableLayout();
		ColumnLayoutData[] columnLayoutData= new ColumnLayoutData[1];
		columnLayoutData[0]= new ColumnWeightData(100);		
		tableLayout.addColumnData(columnLayoutData[0]);
		fFilterTable.setLayout(tableLayout);
		new TableColumn(fFilterTable, SWT.NONE);
		fFilterViewer = new CheckboxTableViewer(fFilterTable);
		fTableEditor = new TableEditor(fFilterTable);
		fFilterViewer.setLabelProvider(new FilterLabelProvider());
		fFilterViewer.setSorter(new FilterViewerSorter());
		fStepFilterContentProvider = new StepFilterContentProvider(fFilterViewer);
		fFilterViewer.setContentProvider(fStepFilterContentProvider);
		// input just needs to be non-null
		fFilterViewer.setInput(this);
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		gd.heightHint = 150;
		gd.widthHint = 300;
		fFilterViewer.getTable().setLayoutData(gd);
		fFilterViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Filter filter = (Filter)event.getElement();
				fStepFilterContentProvider.toggleFilter(filter);
			}
		});
		fFilterViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				if (selection.isEmpty()) {
					fRemoveFilterButton.setEnabled(false);
				} else {
					fRemoveFilterButton.setEnabled(true);					
				}
			}
		});		
		
		createStepFilterButtons(container);
		createStepFilterCheckboxes(container);
	}
	
	private void createStepFilterCheckboxes(Composite container) {
		// filter synthetic checkbox
		fFilterSyntheticButton = new Button(container, SWT.CHECK);
		fFilterSyntheticButton.setText(DebugUIMessages.getString("JavaDebugPreferencePage.Filter_s&ynthetic_methods_(requires_VM_support)_17")); //$NON-NLS-1$
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 2;
		fFilterSyntheticButton.setLayoutData(gd);
		
		// filter static checkbox
		fFilterStaticButton = new Button(container, SWT.CHECK);
		fFilterStaticButton.setText(DebugUIMessages.getString("JavaDebugPreferencePage.Filter_static_&initializers_18")); //$NON-NLS-1$
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 2;
		fFilterStaticButton.setLayoutData(gd);
		
		// filter constructor checkbox
		fFilterConstructorButton = new Button(container, SWT.CHECK);
		fFilterConstructorButton.setText(DebugUIMessages.getString("JavaDebugPreferencePage.Filter_co&nstructors_19")); //$NON-NLS-1$
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalSpan = 2;
		fFilterConstructorButton.setLayoutData(gd);
		
		fFilterSyntheticButton.setSelection(getPreferenceStore().getBoolean(IJDIPreferencesConstants.PREF_FILTER_SYNTHETICS));
		fFilterStaticButton.setSelection(getPreferenceStore().getBoolean(IJDIPreferencesConstants.PREF_FILTER_STATIC_INITIALIZERS));
		fFilterConstructorButton.setSelection(getPreferenceStore().getBoolean(IJDIPreferencesConstants.PREF_FILTER_CONSTRUCTORS));
		boolean enabled = getPreferenceStore().getBoolean(IJDIPreferencesConstants.PREF_USE_FILTERS);
		fUseFiltersCheckbox.setSelection(enabled);
		toggleStepFilterWidgetsEnabled(enabled);
	}
	private void createStepFilterButtons(Composite container) {
		// button container
		Composite buttonContainer = new Composite(container, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		GridLayout buttonLayout = new GridLayout();
		buttonLayout.numColumns = 1;
		buttonLayout.marginHeight = 0;
		buttonLayout.marginWidth = 0;
		buttonContainer.setLayout(buttonLayout);
		
		// Add filter button
		fAddFilterButton = new Button(buttonContainer, SWT.PUSH);
		fAddFilterButton.setText(DebugUIMessages.getString("JavaDebugPreferencePage.Add_&Filter_9")); //$NON-NLS-1$
		fAddFilterButton.setToolTipText(DebugUIMessages.getString("JavaDebugPreferencePage.Key_in_the_name_of_a_new_step_filter_10")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fAddFilterButton.setLayoutData(gd);
		fAddFilterButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				editFilter();
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});
		
		// Add type button
		fAddTypeButton = new Button(buttonContainer, SWT.PUSH);
		fAddTypeButton.setText(DebugUIMessages.getString("JavaDebugPreferencePage.Add_&Type..._11")); //$NON-NLS-1$
		fAddTypeButton.setToolTipText(DebugUIMessages.getString("JavaDebugPreferencePage.Choose_a_Java_type_and_add_it_to_step_filters_12")); //$NON-NLS-1$
		gd = getButtonGridData(fAddTypeButton);
		fAddTypeButton.setLayoutData(gd);
		fAddTypeButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				addType();
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});
		
		// Add package button
		fAddPackageButton = new Button(buttonContainer, SWT.PUSH);
		fAddPackageButton.setText(DebugUIMessages.getString("JavaDebugPreferencePage.Add_&Package..._13")); //$NON-NLS-1$
		fAddPackageButton.setToolTipText(DebugUIMessages.getString("JavaDebugPreferencePage.Choose_a_package_and_add_it_to_step_filters_14")); //$NON-NLS-1$
		gd = getButtonGridData(fAddPackageButton);
		fAddPackageButton.setLayoutData(gd);
		fAddPackageButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				addPackage();
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});
		
		// Remove button
		fRemoveFilterButton = new Button(buttonContainer, SWT.PUSH);
		fRemoveFilterButton.setText(DebugUIMessages.getString("JavaDebugPreferencePage.&Remove_15")); //$NON-NLS-1$
		fRemoveFilterButton.setToolTipText(DebugUIMessages.getString("JavaDebugPreferencePage.Remove_all_selected_step_filters_16")); //$NON-NLS-1$
		gd = getButtonGridData(fRemoveFilterButton);
		fRemoveFilterButton.setLayoutData(gd);
		fRemoveFilterButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent se) {
				removeFilters();
			}
			public void widgetDefaultSelected(SelectionEvent se) {
			}
		});
		fRemoveFilterButton.setEnabled(false);
		
	}
	private void toggleStepFilterWidgetsEnabled(boolean enabled) {
		fFilterViewer.getTable().setEnabled(enabled);
		fAddPackageButton.setEnabled(enabled);
		fAddTypeButton.setEnabled(enabled);
		fAddFilterButton.setEnabled(enabled);
		fFilterSyntheticButton.setEnabled(enabled);
		fFilterStaticButton.setEnabled(enabled);
		fFilterConstructorButton.setEnabled(enabled);
		if (!enabled) {
			fRemoveFilterButton.setEnabled(enabled);
		} else if (!fFilterViewer.getSelection().isEmpty()) {
			fRemoveFilterButton.setEnabled(true);
		}
	}
	
	private GridData getButtonGridData(Button button) {
		GridData gd= new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		GC gc = new GC(button);
		gc.setFont(button.getFont());
		FontMetrics fontMetrics= gc.getFontMetrics();
		gc.dispose();
		int widthHint= Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_WIDTH);
		gd.widthHint= Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		
		gd.heightHint= Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_HEIGHT);
		return gd;
	}
	
	/**
	 * Create a new filter in the table (with the default 'new filter' value),
	 * then open up an in-place editor on it.
	 */
	private void editFilter() {
		// if a previous edit is still in progress, finish it
		if (fEditorText != null) {
			validateChangeAndCleanup();
		}
		
		fNewStepFilter = fStepFilterContentProvider.addFilter(DEFAULT_NEW_FILTER_TEXT, true);		
		fNewTableItem = fFilterTable.getItem(0);
		
		// create & configure Text widget for editor
		// Fix for bug 1766.  Border behavior on Windows & Linux for text
		// fields is different.  On Linux, you always get a border, on Windows,
		// you don't.  Specifying a border on Linux results in the characters
		// getting pushed down so that only there very tops are visible.  Thus,
		// we have to specify different style constants for the different platforms.
		int textStyles = SWT.SINGLE | SWT.LEFT;
		if (SWT.getPlatform().equals("win32")) {  //$NON-NLS-1$
			textStyles |= SWT.BORDER;
		}
		fEditorText = new Text(fFilterTable, textStyles);
		GridData gd = new GridData(GridData.FILL_BOTH);
		fEditorText.setLayoutData(gd);
		
		// set the editor
		fTableEditor.horizontalAlignment = SWT.LEFT;
		fTableEditor.grabHorizontal = true;
		fTableEditor.setEditor(fEditorText, fNewTableItem, 0);
		
		// get the editor ready to use
		fEditorText.setText(fNewStepFilter.getName());
		fEditorText.selectAll();
		setEditorListeners(fEditorText);
		fEditorText.setFocus();
	}
	
	private void setEditorListeners(Text text) {
		// CR means commit the changes, ESC means abort and don't commit
		text.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {				
				if (event.character == SWT.CR) {
					if (fInvalidEditorText != null) {
						fEditorText.setText(fInvalidEditorText);
						fInvalidEditorText= null;
					} else {
						validateChangeAndCleanup();
					}
				} else if (event.character == SWT.ESC) {
					removeNewFilter();
					cleanupEditor();
				}
			}
		});
		// Consider loss of focus on the editor to mean the same as CR
		text.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent event) {
				if (fInvalidEditorText != null) {
					fEditorText.setText(fInvalidEditorText);
					fInvalidEditorText= null;
				} else {
					validateChangeAndCleanup();
				}
			}
		});
		// Consume traversal events from the text widget so that CR doesn't 
		// traverse away to dialog's default button.  Without this, hitting
		// CR in the text field closes the entire dialog.
		text.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event event) {
				event.doit = false;
			}
		});
	}
	
	private void validateChangeAndCleanup() {
		String trimmedValue = fEditorText.getText().trim();
		// if the new value is blank, remove the filter
		if (trimmedValue.length() < 1) {
			removeNewFilter();
		}
		// if it's invalid, beep and leave sitting in the editor
		else if (!validateEditorInput(trimmedValue)) {
			fInvalidEditorText= trimmedValue;
			fEditorText.setText(DebugUIMessages.getString("JavaDebugPreferencePage.Invalid_step_filter._Return_to_continue;_escape_to_exit._1")); //$NON-NLS-1$
			getShell().getDisplay().beep();			
			return;
		// otherwise, commit the new value if not a duplicate
		} else {		
			
			Object[] filters= fStepFilterContentProvider.getElements(null);
			for (int i = 0; i < filters.length; i++) {
				Filter filter = (Filter)filters[i];
				if (filter.getName().equals(trimmedValue)) {
					removeNewFilter();
					cleanupEditor();
					return;
				}	
			}
			fNewTableItem.setText(trimmedValue);
			fNewStepFilter.setName(trimmedValue);
			fFilterViewer.refresh();
		}
		cleanupEditor();
	}
	
	/**
	 * Cleanup all widgetry & resources used by the in-place editing
	 */
	private void cleanupEditor() {
		if (fEditorText != null) {
			fEditorText.dispose();
			fEditorText = null;
			fNewStepFilter = null;
			fNewTableItem = null;
			fTableEditor.setEditor(null, null, 0);			
		}
	}
	
	private void removeNewFilter() {
		fStepFilterContentProvider.removeFilters(new Object[] {fNewStepFilter});
	}
	
	/**
	 * A valid step filter is simply one that is a valid Java identifier.
	 * and, as defined in the JDI spec, the regular expressions used for
	 * step filtering must be limited to exact matches or patterns that
	 * begin with '*' or end with '*'. Beyond this, a string cannot be validated
	 * as corresponding to an existing type or package (and this is probably not
	 * even desirable).  
	 */
	private boolean validateEditorInput(String trimmedValue) {
		char firstChar= trimmedValue.charAt(0);
		if (!Character.isJavaIdentifierStart(firstChar)) {
			if (!(firstChar == '*')) {
				return false;
			}
		}
		int length= trimmedValue.length();
		for (int i= 1; i < length; i++) {
			char c= trimmedValue.charAt(i);
			if (!Character.isJavaIdentifierPart(c)) {
				if (c == '.' && i != (length - 1)) {
					continue;
				}
				if (c == '*' && i == (length - 1)) {
					continue;
				}
				return false;
			}
		}
		return true;
	}
	
	private void addType() {
		Shell shell= getShell();
		SelectionDialog dialog= null;
		try {
			dialog= JavaUI.createTypeDialog(shell, new ProgressMonitorDialog(shell),
				SearchEngine.createWorkspaceScope(), IJavaElementSearchConstants.CONSIDER_TYPES, false);
		} catch (JavaModelException jme) {
			String title= DebugUIMessages.getString("JavaDebugPreferencePage.Add_type_to_step_filters_20"); //$NON-NLS-1$
			String message= DebugUIMessages.getString("JavaDebugPreferencePage.Could_not_open_type_selection_dialog_for_step_filters_21"); //$NON-NLS-1$
			ExceptionHandler.handle(jme, title, message);
			return;
		}
	
		dialog.setTitle(DebugUIMessages.getString("JavaDebugPreferencePage.Add_type_to_step_filters_20")); //$NON-NLS-1$
		dialog.setMessage(DebugUIMessages.getString("JavaDebugPreferencePage.Select_a_type_to_filter_when_stepping_23")); //$NON-NLS-1$
		if (dialog.open() == IDialogConstants.CANCEL_ID) {
			return;
		}
		
		Object[] types= dialog.getResult();
		if (types != null && types.length > 0) {
			IType type = (IType)types[0];
			fStepFilterContentProvider.addFilter(type.getFullyQualifiedName(), true);
		}		
	}
	
	private void addPackage() {
		Shell shell= getShell();
		SelectionDialog dialog = null;
		try {
			dialog = createAllPackagesDialog(shell);
		} catch (JavaModelException jme) {
			String title= DebugUIMessages.getString("JavaDebugPreferencePage.Add_package_to_step_filters_24"); //$NON-NLS-1$
			String message= DebugUIMessages.getString("JavaDebugPreferencePage.Could_not_open_package_selection_dialog_for_step_filters_25"); //$NON-NLS-1$
			ExceptionHandler.handle(jme, title, message);
			return;			
		}
	
		dialog.setTitle(DebugUIMessages.getString("JavaDebugPreferencePage.Add_package_to_step_filters_24")); //$NON-NLS-1$
		dialog.setMessage(DebugUIMessages.getString("JavaDebugPreferencePage.Select_a_package_to_filter_when_stepping_27")); //$NON-NLS-1$
		if (dialog.open() == IDialogConstants.CANCEL_ID) {
			return;
		}
		
		Object[] packages= dialog.getResult();
		if (packages != null && packages.length > 0) {
			IJavaElement pkg = (IJavaElement)packages[0];
			String filter = pkg.getElementName();
			if (filter.length() < 1) {
				filter = DebugUIMessages.getString("JavaDebugPreferencePage.(default_package)_28"); //$NON-NLS-1$
			} else {
				filter += ".*"; //$NON-NLS-1$
			}
			fStepFilterContentProvider.addFilter(filter, true);
		}		
	}
	private void removeFilters() {
		IStructuredSelection selection = (IStructuredSelection)fFilterViewer.getSelection();		
		fStepFilterContentProvider.removeFilters(selection.toArray());
	}
	
	public boolean performOk() {
		fStepFilterContentProvider.saveFilters();
		return true;
	}
	
	/**
	 * Sets the default preferences.
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		setDefaultValues();
		super.performDefaults();	
	}
	
	private void setDefaultValues() {
		fStepFilterContentProvider.setDefaults();
	}
	
	/**
	 * Returns the array as a list
	 */
	protected List toList(Object[] array) {
		List list = new ArrayList(array.length);
		for (int i = 0; i < array.length; i++) {
			list.add(array[i]);
		}
		return list;		
	}
	
	/**
	 * Returns a list of active step filters.
	 * 
	 * @return list
	 */
	protected List createActiveStepFiltersList() {
		String[] strings = JavaDebugOptionsManager.parseList(getPreferenceStore().getString(IJDIPreferencesConstants.PREF_ACTIVE_FILTERS_LIST));
		return toList(strings);
	}
	
	/**
	 * Returns a list of active step filters.
	 * 
	 * @return list
	 */
	protected List createInactiveStepFiltersList() {
		String[] strings = JavaDebugOptionsManager.parseList(getPreferenceStore().getString(IJDIPreferencesConstants.PREF_INACTIVE_FILTERS_LIST));
		return toList(strings);
	}	
	
	/**
	 * Ignore package fragments that contain only non-Java resources, and make sure
	 * that each fragment is added to the list only once.
	 */
	private SelectionDialog createAllPackagesDialog(Shell shell) throws JavaModelException{
		IWorkspaceRoot wsroot= ResourcesPlugin.getWorkspace().getRoot();
		IJavaModel model= JavaCore.create(wsroot);
		IJavaProject[] projects= model.getJavaProjects();
		Set packageNameSet= new HashSet(); 
		List packageList = new ArrayList();
		for (int i = 0; i < projects.length; i++) {						
			IPackageFragment[] pkgs= projects[i].getPackageFragments();	
			for (int j = 0; j < pkgs.length; j++) {
				IPackageFragment pkg = pkgs[j];
				if (!pkg.hasChildren() && (pkg.getNonJavaResources().length > 0)) {
					continue;
				}
				if (packageNameSet.add(pkg.getElementName())) {
					packageList.add(pkg);
				}
			}
		}
		int flags= JavaElementLabelProvider.SHOW_DEFAULT;
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(shell, new JavaElementLabelProvider(flags));
		dialog.setIgnoreCase(false);
		dialog.setElements(packageList.toArray()); // XXX inefficient
		return dialog;
	}
	
	/**
	 * Content provider for the table.  Content consists of instances of StepFilter.
	 */	
	protected class StepFilterContentProvider implements IStructuredContentProvider {
		
		private CheckboxTableViewer fViewer;
		private List fFilters;
		
		public StepFilterContentProvider(CheckboxTableViewer viewer) {
			fViewer = viewer;
			List active = createActiveStepFiltersList();
			List inactive = createInactiveStepFiltersList();
			populateFilters(active, inactive);
		}
		
		public void setDefaults() {
			fViewer.remove(fFilters.toArray());			
			List active = createActiveStepFiltersList();
			List inactive = createInactiveStepFiltersList();
			populateFilters(active, inactive);		
							
			fFilterSyntheticButton.setSelection(getPreferenceStore().getBoolean(IJDIPreferencesConstants.PREF_FILTER_SYNTHETICS));
			fFilterStaticButton.setSelection(getPreferenceStore().getBoolean(IJDIPreferencesConstants.PREF_FILTER_STATIC_INITIALIZERS));
			fFilterConstructorButton.setSelection(getPreferenceStore().getBoolean(IJDIPreferencesConstants.PREF_FILTER_CONSTRUCTORS));
			boolean useStepFilters = getPreferenceStore().getBoolean(IJDIPreferencesConstants.PREF_USE_FILTERS);
			fUseFiltersCheckbox.setSelection(useStepFilters);
			toggleStepFilterWidgetsEnabled(useStepFilters);
		}
		
		protected void populateFilters(List activeList, List inactiveList) {
			fFilters = new ArrayList(activeList.size() + inactiveList.size());
			populateList(activeList, true);
			populateList(inactiveList, false);
		}
		
		protected void populateList(List list, boolean checked) {
			Iterator iterator = list.iterator();
			while (iterator.hasNext()) {
				String name = (String)iterator.next();
				addFilter(name, checked);
			}			
		}
		
		public Filter addFilter(String name, boolean checked) {
			Filter filter = new Filter(name, checked);
			if (!fFilters.contains(filter)) {
				fFilters.add(filter);
				fViewer.add(filter);
				fViewer.setChecked(filter, checked);
			}
			return filter;
		}
		
		public void saveFilters() {
			
			getPreferenceStore().setValue(IJDIPreferencesConstants.PREF_USE_FILTERS, fUseFiltersCheckbox.getSelection());
			getPreferenceStore().setValue(IJDIPreferencesConstants.PREF_FILTER_CONSTRUCTORS, fFilterConstructorButton.getSelection());
			getPreferenceStore().setValue(IJDIPreferencesConstants.PREF_FILTER_STATIC_INITIALIZERS, fFilterStaticButton.getSelection());
			getPreferenceStore().setValue(IJDIPreferencesConstants.PREF_FILTER_SYNTHETICS, fFilterSyntheticButton.getSelection());
							
			List active = new ArrayList(fFilters.size());
			List inactive = new ArrayList(fFilters.size());
			Iterator iterator = fFilters.iterator();
			while (iterator.hasNext()) {
				Filter filter = (Filter)iterator.next();
				String name = filter.getName();
				if (filter.isChecked()) {
					active.add(name);
				} else {
					inactive.add(name);
				}
			}
			String pref = JavaDebugOptionsManager.serializeList((String[])active.toArray(new String[active.size()]));
			getPreferenceStore().setValue(IJDIPreferencesConstants.PREF_ACTIVE_FILTERS_LIST, pref);
			pref = JavaDebugOptionsManager.serializeList((String[])inactive.toArray(new String[inactive.size()]));
			getPreferenceStore().setValue(IJDIPreferencesConstants.PREF_INACTIVE_FILTERS_LIST, pref);
		}
		
		public void removeFilters(Object[] filters) {
			for (int i = 0; i < filters.length; i++) {
				Filter filter = (Filter)filters[i];
				fFilters.remove(filter);
			}
			fViewer.remove(filters);
		}
		
		public void toggleFilter(Filter filter) {
			boolean newState = !filter.isChecked();
			filter.setChecked(newState);
			fViewer.setChecked(filter, newState);
		}
		
		/**
		 * @see IStructuredContentProvider#getElements(Object)
		 */
		public Object[] getElements(Object inputElement) {
			return fFilters.toArray();
		}
		
		/**
		 * @see IContentProvider#inputChanged(Viewer, Object, Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		
		/**
		 * @see IContentProvider#dispose()
		 */
		public void dispose() {
		}		
	}
	

}
