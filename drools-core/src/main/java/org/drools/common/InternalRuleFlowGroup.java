package org.drools.common;

import org.drools.ruleflow.instance.RuleFlowProcessInstance;
import org.drools.spi.Activation;
import org.drools.spi.RuleFlowGroup;

public interface InternalRuleFlowGroup
    extends
    RuleFlowGroup {

	void setWorkingMemory(InternalWorkingMemory workingMemory);
	
	InternalWorkingMemory getWorkingMemory();
	
    void addActivation(Activation activation);

    void removeActivation(final Activation activation);
    
    void clear();

    /**
     * Activates or deactivates this <code>RuleFlowGroup</code>.
     * When activating, all activations of this <code>RuleFlowGroup</code> are added
     * to the agenda.
     * As long as the <code>RuleFlowGroup</code> remains active,
     * its activations are automatically added to the agenda. 
     * When deactivating, all activations of this <code>RuleFlowGroup</code> are removed
     * to the agenda.
     * As long as the <code>RuleFlowGroup</code> remains deactive,
     * its activations are not added to the agenda. 
     */
    void setActive(boolean active);

    boolean isActive();

    void triggerCompleted();
    
    RuleFlowProcessInstance getProcessInstance();
    
}
