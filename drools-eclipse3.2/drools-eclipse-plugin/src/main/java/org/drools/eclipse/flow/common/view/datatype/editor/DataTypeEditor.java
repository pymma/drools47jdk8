package org.drools.eclipse.flow.common.view.datatype.editor;
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

import org.drools.ruleflow.common.datatype.DataType;

/**
 * An editor for a datatype.
 * 
 * @author <a href="mailto:kris_verlaenen@hotmail.com">Kris Verlaenen</a>
 */
public interface DataTypeEditor {

    void setDataType(DataType dataType);
    
    DataType getDataType();
    
    void reset();
    
    void addListener(DataTypeListener listener);
    
    void removeListener(DataTypeListener listener);

    public interface DataTypeListener {
        void dataTypeChanged(DataType dataType);
    }
}
