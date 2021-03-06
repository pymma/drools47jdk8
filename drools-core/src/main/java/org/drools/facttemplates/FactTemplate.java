/*
 * Copyright 2002-2005 Peter Lin & RuleML.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://ruleml-dev.sourceforge.net/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.drools.facttemplates;

import java.io.Serializable;

import org.drools.rule.Package;

/**
 * @author Peter Lin
 *
 * Template defines the methods to access an object, which is the
 * equivalent of un-ordered facts. It defines all the necessary
 * methods for Deftemplate.
 */
public interface FactTemplate
    extends
    Serializable {

    Package getPackage();

    /**
     * The name of the template may be the fully qualified
     * class name, or an alias.
     * @return
     */
    String getName();

    /**
     * templates may have 1 or more slots. A slot is a named
     * pattern with a specific type of value.
     * @return
     */
    int getNumberOfFields();

    /**
     * Return an array of all the slots.
     * @return
     */
    FieldTemplate[] getAllFieldTemplates();

    /**
     * Return the slot with the String name
     * @return
     */
    FieldTemplate getFieldTemplate(String name);

    /**
     * Get the Slot at the given pattern id
     * @param index
     * @return
     */
    FieldTemplate getFieldTemplate(int index);

    /**
     * Get the pattern index with the given name
     * @param name
     * @return
     */
    int getFieldTemplateIndex(String name);

    Fact createFact(long id);
}
