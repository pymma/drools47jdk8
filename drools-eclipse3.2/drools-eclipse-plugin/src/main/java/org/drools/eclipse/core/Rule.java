package org.drools.eclipse.core;

import java.util.HashMap;
import java.util.Map;

/**
 * This represents a rule. 
 * 
 * @author <a href="mailto:kris_verlaenen@hotmail.com">kris verlaenen </a>
 */
public class Rule extends DroolsElement {

    private final String ruleName;
    private Map attributes = new HashMap();

    Rule(Package parent, String ruleName) {
    	super(parent);
        this.ruleName = ruleName;
    }

	public Package getParentPackage() {
		return (Package) getParent();
	}
	
	public String getRuleName() {
		return ruleName;
	}
	
	public RuleAttribute getAttribute(String attributeName) {
		return (RuleAttribute) attributes.get(attributeName);
	}
	
	public int getType() {
		return RULE;
	}
	
	public DroolsElement[] getChildren() {
		return NO_ELEMENTS;
	}

	public String toString() {
		return ruleName;
	}

	// These are helper methods for creating the model and should not
	// be used directly.  Use DroolsModelBuilder instead.

    void addAttribute(RuleAttribute attribute) {
		attributes.put(attribute.getAttributeName(), attribute);
	}

}
