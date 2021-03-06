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

public class ReturnValueRestrictionDescr extends RestrictionDescr {
    /**
     * 
     */
    private static final long serialVersionUID = 400L;
    private String            evaluator;
    private Object            content;
    private String[]          declarations;

    private String            classMethodName;

    public ReturnValueRestrictionDescr(final String evaluator) {
        this.evaluator = evaluator;
    }

    public ReturnValueRestrictionDescr(final String evaluator,
                                       final Object content) {
        this.evaluator = evaluator;
        this.content = content;
    }

    public String getClassMethodName() {
        return this.classMethodName;
    }

    public void setClassMethodName(final String classMethodName) {
        this.classMethodName = classMethodName;
    }

    public String getEvaluator() {
        return this.evaluator;
    }

    public Object getContent() {
        return this.content;
    }

    public void setContent(final String text) {
        this.content = text;
    }

    public void setDeclarations(final String[] declarations) {
        this.declarations = declarations;
    }

    public String[] getDeclarations() {
        return this.declarations;
    }

    public String toString() {
        return "[ReturnValue: " + this.evaluator + " " + this.content + "]";
    }
}