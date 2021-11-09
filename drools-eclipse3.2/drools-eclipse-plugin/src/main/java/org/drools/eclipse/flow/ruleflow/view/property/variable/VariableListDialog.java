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

import org.drools.eclipse.flow.common.view.property.EditListDialog;
import org.drools.ruleflow.core.impl.VariableImpl;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for editing a list of variables.
 * 
 * @author <a href="mailto:kris_verlaenen@hotmail.com">Kris Verlaenen</a>
 */
public class VariableListDialog extends EditListDialog {
    
    protected VariableListDialog(Shell parentShell) {
        super(parentShell, "Variable List Editor", VariableDialog.class);
    }

    protected Object createItem() {
        return new VariableImpl();
    }
}
