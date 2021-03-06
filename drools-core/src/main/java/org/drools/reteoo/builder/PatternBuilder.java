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

package org.drools.reteoo.builder;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.drools.RuntimeDroolsException;
import org.drools.base.ClassObjectType;
import org.drools.base.DroolsQuery;
import org.drools.common.InstanceNotEqualsConstraint;
import org.drools.common.InternalWorkingMemory;
import org.drools.reteoo.AlphaNode;
import org.drools.reteoo.ObjectSource;
import org.drools.reteoo.ObjectTypeNode;
import org.drools.rule.Declaration;
import org.drools.rule.InvalidPatternException;
import org.drools.rule.MutableTypeConstraint;
import org.drools.rule.Pattern;
import org.drools.rule.PatternSource;
import org.drools.rule.RuleConditionElement;
import org.drools.spi.AlphaNodeFieldConstraint;
import org.drools.spi.Constraint;
import org.drools.spi.ObjectType;
import org.drools.spi.Constraint.ConstraintType;

/**
 * A builder for patterns
 * 
 * @author etirelli
 */
public class PatternBuilder
    implements
    ReteooComponentBuilder {

    /**
     * @inheritDoc
     */
    public void build(final BuildContext context,
                      final BuildUtils utils,
                      final RuleConditionElement rce) {

        final Pattern pattern = (Pattern) rce;

        this.attachPattern( context,
                            utils,
                            pattern );

    }

    private void attachPattern(final BuildContext context,
                               final BuildUtils utils,
                               final Pattern pattern) throws InvalidPatternException {

        // Set pattern offset to the appropriate value
        pattern.setOffset( context.getCurrentPatternOffset() );

        final List alphaConstraints = new LinkedList();
        final List betaConstraints = new LinkedList();

        this.createConstraints( context,
                                utils,
                                pattern,
                                alphaConstraints,
                                betaConstraints );

        // Create BetaConstraints object
        context.setBetaconstraints( betaConstraints );

        if ( pattern.getSource() == null ) {
            // pattern is selected from working memory, so 
            // Attach alpha nodes
            attachAlphaNodes( context,
                              utils,
                              pattern,
                              alphaConstraints );

        } else {
            context.setAlphaConstraints( alphaConstraints );
            final int currentOffset = context.getCurrentPatternOffset();

            PatternSource source = pattern.getSource();

            ReteooComponentBuilder builder = utils.getBuilderFor( source );

            builder.build( context,
                           utils,
                           source );
            // restoring offset
            context.setCurrentPatternOffset( currentOffset );
        }
        
        // last thing to do is increment the offset, since if the pattern has a source,
        // offset must be overriden
        context.incrementCurrentPatternOffset();
    }

    private void createConstraints(BuildContext context,
                                   BuildUtils utils,
                                   Pattern pattern,
                                   List alphaConstraints,
                                   List betaConstraints) {

        final List constraints = pattern.getConstraints();

        // check if cross products for identity patterns should be disabled
        checkRemoveIdentities( context,
                               pattern,
                               betaConstraints );

        for ( final Iterator it = constraints.iterator(); it.hasNext(); ) {
            final Object object = it.next();
            // Check if its a declaration
            if ( object instanceof Declaration ) {
                // nothing to be done
                continue;
            }

            final Constraint constraint = (Constraint) object;
            if ( constraint.getType().equals( Constraint.ConstraintType.ALPHA ) ) {
                alphaConstraints.add( constraint );
            } else if ( constraint.getType().equals( Constraint.ConstraintType.BETA ) ) {
                utils.checkUnboundDeclarations( context,
                                                constraint.getRequiredDeclarations() );
                betaConstraints.add( constraint );
            } else {
                throw new RuntimeDroolsException( "Unknown constraint type: "+constraint.getType()+". This is a bug. Please contact development team.");
            }
        }
    }

    public static ObjectTypeNode attachObjectTypeNode(BuildContext context,
                                                      ObjectType objectType) {
        synchronized ( context.getRuleBase().getPackagesMap() ) {                           
            ObjectTypeNode otn = new ObjectTypeNode( context.getNextId(),
                                                     objectType,
                                                     context );
    
            InternalWorkingMemory[] wms = context.getWorkingMemories();
            if ( wms.length > 0 ) {
                otn.attach( wms );
            } else {
                otn.attach();
            }
    
            return otn;
        } 
    }

    public void attachAlphaNodes(final BuildContext context,
                                 final BuildUtils utils,
                                 final Pattern pattern,
                                 List alphaConstraints) throws InvalidPatternException {

        // Drools Query ObjectTypeNode never has memory, but other ObjectTypeNode/AlphaNoesNodes may (if not in sequential), 
        //so need to preserve, so we can resotre after this node is added. LeftMemory  and Terminal remain the same once set.
        
        boolean objectMemory = context.isObjectTypeNodeMemoryEnabled(); 
        boolean alphaMemory = context.isAlphaMemoryAllowed();
                
        if ( pattern.getObjectType() instanceof ClassObjectType ) {
            // Is this the query node, if so we don't want any memory
            if ( DroolsQuery.class == ((ClassObjectType) pattern.getObjectType()).getClassType() ) {
                context.setTupleMemoryEnabled( false );
                context.setObjectTypeNodeMemoryEnabled( false );
                context.setTerminalNodeMemoryEnabled( false );
                context.setAlphaNodeMemoryAllowed( false );
            }
        }

        context.setObjectSource( (ObjectSource) utils.attachNode( context,
                                                                  new ObjectTypeNode( context.getNextId(),
                                                                                      pattern.getObjectType(),
                                                                                      context ) ) );

        for ( final Iterator it = alphaConstraints.iterator(); it.hasNext(); ) {
            final AlphaNodeFieldConstraint constraint = (AlphaNodeFieldConstraint) it.next();

            context.setObjectSource( (ObjectSource) utils.attachNode( context,
                                                                      new AlphaNode( context.getNextId(),
                                                                                     (AlphaNodeFieldConstraint) constraint,
                                                                                     context.getObjectSource(),
                                                                                     context ) ) );
        }
        
        // now restore back to original values
        context.setObjectTypeNodeMemoryEnabled( objectMemory );
        context.setAlphaNodeMemoryAllowed( alphaMemory );

    }

    /**
     * @param context
     * @param pattern
     * @param betaConstraints
     */
    private void checkRemoveIdentities(final BuildContext context,
                                       final Pattern pattern,
                                       final List betaConstraints) {
        if ( context.getRuleBase().getConfiguration().isRemoveIdentities() && pattern.getObjectType().getClass() == ClassObjectType.class ) {
            List patterns = null;
            // Check if this object type exists before
            // If it does we need stop instance equals cross product
            final Class thisClass = ((ClassObjectType) pattern.getObjectType()).getClassType();
            for ( final Iterator it = context.getObjectType().iterator(); it.hasNext(); ) {
                final Pattern previousPattern = (Pattern) it.next();
                final Class previousClass = ((ClassObjectType) previousPattern.getObjectType()).getClassType();
                if ( thisClass.isAssignableFrom( previousClass ) ) {
                    betaConstraints.add( new InstanceNotEqualsConstraint( previousPattern ) );
                }
            }

            // Must be added after the checking, otherwise it matches against itself
            context.getObjectType().add( pattern );
        }
    }

    /**
     * @inheritDoc
     */
    public boolean requiresLeftActivation(final BuildUtils utils,
                                          final RuleConditionElement rce) {
        return ((Pattern) rce).getSource() != null;
    }
}
