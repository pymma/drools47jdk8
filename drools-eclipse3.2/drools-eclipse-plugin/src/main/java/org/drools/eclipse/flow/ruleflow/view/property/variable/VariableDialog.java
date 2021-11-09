package org.drools.eclipse.flow.ruleflow.view.property.variable;
/*
 * Copyright 2005 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.drools.eclipse.DroolsEclipsePlugin;
import org.drools.eclipse.flow.common.datatype.DefaultDataTypeRegistry;
import org.drools.eclipse.flow.common.view.datatype.editor.DataTypeEditor;
import org.drools.eclipse.flow.common.view.datatype.editor.impl.DataTypeCombo;
import org.drools.eclipse.flow.common.view.datatype.editor.impl.DataTypeEditorComposite;
import org.drools.eclipse.flow.common.view.datatype.editor.impl.EditorComposite;
import org.drools.eclipse.flow.common.view.property.EditBeanDialog;
import org.drools.ruleflow.common.datatype.DataType;
import org.drools.ruleflow.core.Variable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog for editing variables.
 * 
 * @author <a href="mailto:kris_verlaenen@hotmail.com">Kris Verlaenen</a>
 */
public class VariableDialog extends EditBeanDialog {
    
    private Text nameText;
    private DataTypeCombo dataTypeCombo;
    private DataTypeEditorComposite dataTypeEditorComposite;
    private EditorComposite editorComposite;

    public VariableDialog(Shell parentShell) {
        super(parentShell, "Edit Variable");
    }
    
    protected Control createDialogArea(Composite parent) {
        final Composite composite = (Composite) super.createDialogArea(parent);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        composite.setLayout(gridLayout);
        
        Label nameLabel = new Label(composite, SWT.NONE);
        nameLabel.setText("Name: ");
        nameText = new Text(composite, SWT.NONE);
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        nameText.setLayoutData(gridData);
        String name = ((Variable) getValue()).getName();
        nameText.setText(name == null ? "" : name);

        Label typeLabel = new Label(composite, SWT.NONE);
        typeLabel.setText("Type: ");
        
        dataTypeCombo = new DataTypeCombo(composite,
    		SWT.NONE, DefaultDataTypeRegistry.getInstance());
        DataType dataType = ((Variable) getValue()).getType();
        dataTypeCombo.setDataType(dataType);
        dataTypeCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				DataType dataType = dataTypeCombo.getDataType();
				dataTypeEditorComposite.setDataType(dataType);
				editorComposite.setDataType(dataType);
				composite.layout();
			}
        });
        
    	new Label(composite, SWT.NONE);
        
        dataTypeEditorComposite = new DataTypeEditorComposite(composite,
            SWT.NONE, DefaultDataTypeRegistry.getInstance());
        gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = GridData.FILL;
        dataTypeEditorComposite.setLayoutData(gridData);
        dataTypeEditorComposite.setDataType(dataType);
        dataTypeEditorComposite.addListener(new DataTypeEditor.DataTypeListener() {
            public void dataTypeChanged(DataType dataType) {
                editorComposite.setDataType(dataType);
                composite.layout();
            }
        });
        
        Label valueLabel = new Label(composite, SWT.NONE);
        valueLabel.setText("Value: ");
        gridData = new GridData();
        gridData.verticalAlignment = SWT.TOP;
        valueLabel.setLayoutData(gridData);
        editorComposite = new EditorComposite(composite,
            SWT.NONE, DefaultDataTypeRegistry.getInstance());
        gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        editorComposite.setLayoutData(gridData);
        editorComposite.setDataType(dataType);
        editorComposite.setValue(((Variable) getValue()).getValue());
        
        Composite bottom = new Composite(composite, SWT.NONE);
        gridData = new GridData();
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalSpan = 2;
        bottom.setLayoutData(gridData);
        
        return composite;
    }
    
    protected Object updateValue(Object value) {
        Variable variable = (Variable) getValue();
        variable.setName(nameText.getText());
        try {
            variable.setType(dataTypeEditorComposite.getDataType());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            throw e;
        }
        try {
            variable.setValue(editorComposite.getValue());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            throw e;
        }
        return variable;
    }
    
    private void showError(String error) {
        ErrorDialog.openError(getShell(), "Error", error, new Status(
            IStatus.ERROR, DroolsEclipsePlugin.getDefault().getBundle().getSymbolicName(),
            IStatus.ERROR, error, null));
    }
}