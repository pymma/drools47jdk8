package org.drools.ruleflow.instance.impl;

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

import org.drools.ruleflow.core.RuleSetNode;
import org.drools.ruleflow.instance.RuleFlowNodeInstance;

/**
 * Runtime counterpart of a ruleset node.
 * 
 * @author <a href="mailto:kris_verlaenen@hotmail.com">Kris Verlaenen</a>
 */
public class RuleFlowSequenceNodeInstanceImpl extends RuleFlowNodeInstanceImpl {

    protected RuleSetNode getRuleSetNode() {
        return (RuleSetNode) getNode();
    }

    public void trigger(final RuleFlowNodeInstance from) {
        getProcessInstance().getAgenda().activateRuleFlowGroup( getRuleSetNode().getRuleFlowGroup() );
    }

    public void triggerCompleted() {
        getProcessInstance().getNodeInstance( getRuleSetNode().getTo().getTo() ).trigger( this );
        getProcessInstance().removeNodeInstance(this);
    }
    
    public void cancel() {
    	getProcessInstance().getAgenda().deactivateRuleFlowGroup( getRuleSetNode().getRuleFlowGroup() );
    	super.cancel();
    }

}