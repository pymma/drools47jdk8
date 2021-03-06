/*
 * Copyright 2006 JBoss Inc
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

package org.drools.rule.builder.dialect.mvel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.base.mvel.DroolsMVELFactory;
import org.drools.base.mvel.MVELPredicateExpression;
import org.drools.compiler.Dialect;
import org.drools.compiler.RuleError;
import org.drools.lang.descr.PredicateDescr;
import org.drools.rule.Declaration;
import org.drools.rule.PredicateConstraint;
import org.drools.rule.builder.PredicateBuilder;
import org.drools.rule.builder.RuleBuildContext;

/**
 * @author etirelli
 *
 */
public class MVELPredicateBuilder
    implements
    PredicateBuilder {

    public void build(final RuleBuildContext context,
                      final List[] usedIdentifiers,
                      final Declaration[] previousDeclarations,
                      final Declaration[] localDeclarations,
                      final PredicateConstraint predicate,
                      final PredicateDescr predicateDescr) {
        Map previousMap = new HashMap();
        for ( int i = 0, length = previousDeclarations.length; i < length; i++ ) {
            previousMap.put( previousDeclarations[i].getIdentifier(),
                             previousDeclarations[i] );
        }

        Map localMap = new HashMap();
        for ( int i = 0, length = localDeclarations.length; i < length; i++ ) {
            localMap.put( localDeclarations[i].getIdentifier(),
                          localDeclarations[i] );
        }

        try {
            final DroolsMVELFactory factory = new DroolsMVELFactory( previousMap,
                                                                     localMap,
                                                                     context.getPkg().getGlobals() );

            Dialect.AnalysisResult analysis = context.getDialect().analyzeExpression( context,
                                                                                      predicateDescr,
                                                                                      predicateDescr.getContent() );

            final Serializable expr = ((MVELDialect) context.getDialect()).compile( (String) predicateDescr.getContent(),
                                                                                    analysis,
                                                                                    null,
                                                                                    null,
                                                                                    null,
                                                                                    context );

            predicate.setPredicateExpression( new MVELPredicateExpression( expr,
                                                                           factory,
                                                                           (String) predicateDescr.getContent() ) );
        } catch ( final Exception e ) {
            context.getErrors().add( new RuleError( context.getRule(),
                                                    predicateDescr,
                                                    e,
                                                    "Unable to build expression for 'inline-eval' node '" + predicateDescr.getContent() + "'\n" + e.getMessage() ) );
        }
    }

}
