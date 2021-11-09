/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.drools.eclipse.editors.scanners;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class RuleEditorMessages {

    
    
	private static final String RESOURCE_BUNDLE= getLocation();
    


    
	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

	private RuleEditorMessages() {
	}

	public static String getString(String key) {
        
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}
	
	public static ResourceBundle getResourceBundle() {
		return fgResourceBundle;
	}

    
    private static String getLocation()  {
        return RuleEditorMessages.class.getPackage().getName() + ".RuleEditorMessages";        
    }    
    
}
