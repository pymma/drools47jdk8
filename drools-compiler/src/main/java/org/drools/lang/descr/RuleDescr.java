package org.drools.lang.descr;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.drools.compiler.Dialect;

public class RuleDescr extends BaseDescr {

    private static final long serialVersionUID = 400L;
    private String            name;
    private String            documentation;

    private AndDescr          lhs;
    private Object            consequence;
    private int               consequenceLine;
    private int               consequencePattern;
    private int               offset;
    private List              attributes       = Collections.EMPTY_LIST;
    private String            salience;

    private String            className;

	//MVEL: Compiler change
    private Dialect           dialect;

    public RuleDescr(final String name) {
        this( name,
              "" );
    }


    public RuleDescr(final String ruleName,
                     final String documentation) {
        this.name = ruleName;
        this.documentation = documentation;
    }

    public String getName() {
        return this.name;
    }

    public String getSalience() {
        return salience;
    }

    public void setSalience(String salience) {
        this.salience = salience;
    }

    public String getClassName() {
        return this.className;
    }

    public void setClassName(final String className) {
        this.className = className;
    }

    public String getDocumentation() {
        return this.documentation;
    }

    public List getAttributes() {
        return this.attributes;
    }

    public void addAttribute(final AttributeDescr attribute) {
        if( attribute != null ) {
            if ( this.attributes == Collections.EMPTY_LIST ) {
                this.attributes = new ArrayList();
            }
            this.attributes.add( attribute );
        }
    }

    public void setAttributes(final List attributes) {
        this.attributes = new ArrayList( attributes );
    }

    public AndDescr getLhs() {
        return this.lhs;
    }

    public void setLhs(final AndDescr lhs) {
        this.lhs = lhs;
    }

    public Object getConsequence() {
        return this.consequence;
    }

    public void setConsequence(final Object consequence) {
        this.consequence = consequence;
    }

    public void setConsequenceLocation(final int line,
                                       final int pattern) {
        this.consequenceLine = line;
        this.consequencePattern = pattern;
    }

    public void setConsequenceOffset(final int offset) {
        this.offset = offset;
    }

    public int getConsequenceOffset() {
        return this.offset;
    }

    public int getConsequenceLine() {
        return this.consequenceLine;
    }

    public int getConsequencePattern() {
        return this.consequencePattern;
    }

    public void setDialect(Dialect dialect) {
        this.dialect = dialect;
    }

    public Dialect getDialect() {
        return this.dialect;
    }
}