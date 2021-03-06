package org.drools.eclipse.flow.ruleflow.core;
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

import org.drools.eclipse.flow.common.editor.core.DefaultElementWrapper;
import org.drools.ruleflow.core.Node;

/**
 * Wrapper for a node.
 * 
 * @author <a href="mailto:kris_verlaenen@hotmail.com">Kris Verlaenen</a>
 */
public abstract class NodeWrapper extends DefaultElementWrapper {
	
    protected void setNode(Node node) {
        setElement(node);
    }
    
    public Node getNode() {
        return (Node) getElement();
    }
    
    public String getId() {
        long id = getNode().getId();
        return id == -1 ? null : getNode().getId() + "-Wrapper";
    }

    public String getName() {
        return getNode().getName();
    }

    public void internalSetName(String name) {
        getNode().setName(name);    
        notifyListeners(CHANGE_NAME);
    }
    
}
