package org.drools.reteoo;

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

import java.beans.IntrospectionException;

import org.drools.Cheese;
import org.drools.DroolsTestCase;
import org.drools.FactException;
import org.drools.RuleBaseConfiguration;
import org.drools.RuleBaseFactory;
import org.drools.base.ClassFieldExtractor;
import org.drools.base.ClassFieldExtractorCache;
import org.drools.base.FieldFactory;
import org.drools.base.ValueType;
import org.drools.base.evaluators.Operator;
import org.drools.common.DefaultFactHandle;
import org.drools.common.PropagationContextImpl;
import org.drools.reteoo.AlphaNode.AlphaMemory;
import org.drools.reteoo.builder.BuildContext;
import org.drools.rule.LiteralConstraint;
import org.drools.rule.Rule;
import org.drools.spi.Evaluator;
import org.drools.spi.FieldExtractor;
import org.drools.spi.FieldValue;
import org.drools.spi.PropagationContext;
import org.drools.util.FactHashTable;

public class AlphaNodeTest extends DroolsTestCase {

    ClassFieldExtractorCache     cache  = ClassFieldExtractorCache.getInstance();

    public void testMemory() {
        RuleBaseConfiguration config = new RuleBaseConfiguration();
        config.setAlphaMemory( false );
        ReteooRuleBase ruleBase = (ReteooRuleBase) RuleBaseFactory.newRuleBase( config );
        BuildContext buildContext = new BuildContext( ruleBase,
                                                      ((ReteooRuleBase) ruleBase).getReteooBuilder().getIdGenerator() );
        ReteooWorkingMemory workingMemory = (ReteooWorkingMemory) ruleBase.newStatefulSession();

        final ClassFieldExtractor extractor = cache.getExtractor( Cheese.class,
                                                                  "type",
                                                                  getClass().getClassLoader() );

        final FieldValue field = FieldFactory.getFieldValue( "cheddar" );

        final Evaluator evaluator = ValueType.OBJECT_TYPE.getEvaluator( Operator.EQUAL );

        final LiteralConstraint constraint = new LiteralConstraint( extractor,
                                                                    evaluator,
                                                                    field );

        final AlphaNode alphaNode = new AlphaNode( buildContext.getNextId(),
                                                   constraint,
                                                   null,
                                                   buildContext );

        final AlphaMemory memory = (AlphaMemory) workingMemory.getNodeMemory( alphaNode );

        assertNotNull( memory );
    }

    public void testLiteralConstraintAssertObjectWithMemory() throws Exception {
        RuleBaseConfiguration config = new RuleBaseConfiguration();
        config.setAlphaMemory( true );
        ReteooRuleBase ruleBase = (ReteooRuleBase) RuleBaseFactory.newRuleBase( config );
        BuildContext buildContext = new BuildContext( ruleBase,
                                                      ((ReteooRuleBase) ruleBase).getReteooBuilder().getIdGenerator() );
        ReteooWorkingMemory workingMemory = (ReteooWorkingMemory) ruleBase.newStatefulSession();

        final Rule rule = new Rule( "test-rule" );
        final PropagationContext context = new PropagationContextImpl( 0,
                                                                       PropagationContext.ASSERTION,
                                                                       null,
                                                                       null );

        final MockObjectSource source = new MockObjectSource( buildContext.getNextId() );

        final ClassFieldExtractor extractor = cache.getExtractor( Cheese.class,
                                                                  "type",
                                                                  getClass().getClassLoader() );

        final FieldValue field = FieldFactory.getFieldValue( "cheddar" );

        final Evaluator evaluator = ValueType.OBJECT_TYPE.getEvaluator( Operator.EQUAL );

        final LiteralConstraint constraint = new LiteralConstraint( extractor,
                                                                    evaluator,
                                                                    field );

        // With Memory
        buildContext.setAlphaNodeMemoryAllowed( true );
        final AlphaNode alphaNode = new AlphaNode( buildContext.getNextId(),
                                                   constraint,
                                                   source,
                                                   buildContext ); // has memory

        final MockObjectSink sink = new MockObjectSink();
        alphaNode.addObjectSink( sink );

        final Cheese cheddar = new Cheese( "cheddar",
                                           5 );
        final DefaultFactHandle f0 = (DefaultFactHandle) workingMemory.insert( cheddar );

        // check sink is empty
        assertLength( 0,
                      sink.getAsserted() );

        // check alpha memory is empty 
        final AlphaMemory memory = (AlphaMemory) workingMemory.getNodeMemory( alphaNode );

        assertEquals( 0,
                      memory.facts.size() );

        // object should assert as it passes text
        alphaNode.assertObject( f0,
                                context,
                                workingMemory );

        assertEquals( 1,
                      sink.getAsserted().size() );
        assertEquals( 1,
                      memory.facts.size() );
        Object[] list = (Object[]) sink.getAsserted().get( 0 );
        assertSame( cheddar,
                    workingMemory.getObject( (DefaultFactHandle) list[0] ) );
        assertTrue( "Should contain 'cheddar handle'",
                    memory.facts.contains( f0 ) );

        final Cheese stilton = new Cheese( "stilton",
                                           6 );
        final DefaultFactHandle f1 = new DefaultFactHandle( 1,
                                                            stilton );

        // object should NOT assert as it does not pass test
        alphaNode.assertObject( f1,
                                context,
                                workingMemory );

        assertLength( 1,
                      sink.getAsserted() );
        assertEquals( 1,
                      memory.facts.size() );
        list = (Object[]) sink.getAsserted().get( 0 );
        assertSame( cheddar,
                    workingMemory.getObject( (DefaultFactHandle) list[0] ) );
        assertTrue( "Should contain 'cheddar handle'",
                    memory.facts.contains( f0 ) );
    }

    public void testIsMemoryAllowedOverride() throws Exception {
        RuleBaseConfiguration config = new RuleBaseConfiguration();
        config.setAlphaMemory( true );
        ReteooRuleBase ruleBase = (ReteooRuleBase) RuleBaseFactory.newRuleBase( config );
        BuildContext buildContext = new BuildContext( ruleBase,
                                                      ((ReteooRuleBase) ruleBase).getReteooBuilder().getIdGenerator() );
        ReteooWorkingMemory workingMemory = (ReteooWorkingMemory) ruleBase.newStatefulSession();

        final Rule rule = new Rule( "test-rule" );
        final PropagationContext context = new PropagationContextImpl( 0,
                                                                       PropagationContext.ASSERTION,
                                                                       null,
                                                                       null );

        final MockObjectSource source = new MockObjectSource( buildContext.getNextId() );

        final ClassFieldExtractor extractor = cache.getExtractor( Cheese.class,
                                                                  "type",
                                                                  getClass().getClassLoader() );

        final FieldValue field = FieldFactory.getFieldValue( "cheddar" );

        final Evaluator evaluator = ValueType.OBJECT_TYPE.getEvaluator( Operator.EQUAL );

        final LiteralConstraint constraint = new LiteralConstraint( extractor,
                                                                    evaluator,
                                                                    field );

        // With Memory
        buildContext.setAlphaNodeMemoryAllowed( false );
        final AlphaNode alphaNode = new AlphaNode( buildContext.getNextId(),
                                                   constraint,
                                                   source,
                                                   buildContext ); // has memory

        final MockObjectSink sink = new MockObjectSink();
        alphaNode.addObjectSink( sink );

        final Cheese cheddar = new Cheese( "cheddar",
                                           5 );
        final DefaultFactHandle f0 = (DefaultFactHandle) workingMemory.insert( cheddar );

        // check sink is empty
        assertLength( 0,
                      sink.getAsserted() );

        // check alpha memory is empty 
        final AlphaMemory memory = (AlphaMemory) workingMemory.getNodeMemory( alphaNode );

        assertNull( memory.facts );

        // object should assert as it passes text
        alphaNode.assertObject( f0,
                                context,
                                workingMemory );

        assertEquals( 1,
                      sink.getAsserted().size() );
        // memory should be one, as even though isAlphaMemory is on for the configuration, the build never allows memory
        assertNull( memory.facts );
    }

    public void testLiteralConstraintAssertObjectWithoutMemory() throws Exception {
        RuleBaseConfiguration config = new RuleBaseConfiguration();
        config.setAlphaMemory( false );
        ReteooRuleBase ruleBase = (ReteooRuleBase) RuleBaseFactory.newRuleBase( config );
        BuildContext buildContext = new BuildContext( ruleBase,
                                                      ((ReteooRuleBase) ruleBase).getReteooBuilder().getIdGenerator() );
        ReteooWorkingMemory workingMemory = (ReteooWorkingMemory) ruleBase.newStatefulSession();

        final Rule rule = new Rule( "test-rule" );
        final PropagationContext context = new PropagationContextImpl( 0,
                                                                       PropagationContext.ASSERTION,
                                                                       null,
                                                                       null );

        final MockObjectSource source = new MockObjectSource( buildContext.getNextId() );

        final ClassFieldExtractor extractor = cache.getExtractor( Cheese.class,
                                                                  "type",
                                                                  getClass().getClassLoader() );

        final FieldValue field = FieldFactory.getFieldValue( "cheddar" );

        final Evaluator evaluator = ValueType.OBJECT_TYPE.getEvaluator( Operator.EQUAL );

        final LiteralConstraint constraint = new LiteralConstraint( extractor,
                                                                    evaluator,
                                                                    field );

        // With Memory
        final AlphaNode alphaNode = new AlphaNode( buildContext.getNextId(),
                                                   constraint,
                                                   source,
                                                   buildContext ); // no memory

        final MockObjectSink sink = new MockObjectSink();
        alphaNode.addObjectSink( sink );

        final Cheese cheddar = new Cheese( "cheddar",
                                           5 );
        final DefaultFactHandle f0 = (DefaultFactHandle) workingMemory.insert( cheddar );

        // check sink is empty
        assertLength( 0,
                      sink.getAsserted() );

        // check alpha memory is empty 
        final AlphaMemory memory = (AlphaMemory) workingMemory.getNodeMemory( alphaNode );

        assertNull( memory.facts );

        // object should assert as it passes text
        alphaNode.assertObject( f0,
                                context,
                                workingMemory );

        assertEquals( 1,
                      sink.getAsserted().size() );
        assertNull( memory.facts );
        Object[] list = (Object[]) sink.getAsserted().get( 0 );
        assertSame( cheddar,
                    workingMemory.getObject( (DefaultFactHandle) list[0] ) );

        final Cheese stilton = new Cheese( "stilton",
                                           6 );
        final DefaultFactHandle f1 = new DefaultFactHandle( 1,
                                                            stilton );

        // object should NOT assert as it does not pass test
        alphaNode.assertObject( f1,
                                context,
                                workingMemory );

        assertLength( 1,
                      sink.getAsserted() );
        assertNull( memory.facts );
        list = (Object[]) sink.getAsserted().get( 0 );
        assertSame( cheddar,
                    workingMemory.getObject( (DefaultFactHandle) list[0] ) );
    }

    public void testLiteralConstraintAssertSequentialMode() throws Exception {
        RuleBaseConfiguration config = new RuleBaseConfiguration();
        config.setSequential( true );
        ReteooRuleBase ruleBase = (ReteooRuleBase) RuleBaseFactory.newRuleBase( config );
        BuildContext buildContext = new BuildContext( ruleBase,
                                                      ((ReteooRuleBase) ruleBase).getReteooBuilder().getIdGenerator() );
        ReteooWorkingMemory workingMemory = new ReteooWorkingMemory( buildContext.getNextId(),
                                                                     ruleBase );

        final Rule rule = new Rule( "test-rule" );
        final PropagationContext context = new PropagationContextImpl( 0,
                                                                       PropagationContext.ASSERTION,
                                                                       null,
                                                                       null );

        final MockObjectSource source = new MockObjectSource( buildContext.getNextId() );

        final ClassFieldExtractor extractor = cache.getExtractor( Cheese.class,
                                                                  "type",
                                                                  getClass().getClassLoader() );

        final FieldValue field = FieldFactory.getFieldValue( "cheddar" );

        final Evaluator evaluator = ValueType.OBJECT_TYPE.getEvaluator( Operator.EQUAL );

        final LiteralConstraint constraint = new LiteralConstraint( extractor,
                                                                    evaluator,
                                                                    field );

        // With Memory
        final AlphaNode alphaNode = new AlphaNode( buildContext.getNextId(),
                                                   constraint,
                                                   source,
                                                   buildContext ); // has memory

        final MockObjectSink sink = new MockObjectSink();
        alphaNode.addObjectSink( sink );

        final Cheese cheddar = new Cheese( "cheddar",
                                           5 );
        final DefaultFactHandle f0 = (DefaultFactHandle) workingMemory.insert( cheddar );

        // check sink is empty
        assertLength( 0,
                      sink.getAsserted() );

        // check alpha memory is empty 
        final AlphaMemory memory = (AlphaMemory) workingMemory.getNodeMemory( alphaNode );

        assertNull( memory.facts );

        // object should assert as it passes text
        alphaNode.assertObject( f0,
                                context,
                                workingMemory );

        assertEquals( 1,
                      sink.getAsserted().size() );
        assertNull( memory.facts );
        Object[] list = (Object[]) sink.getAsserted().get( 0 );
        assertSame( cheddar,
                    workingMemory.getObject( (DefaultFactHandle) list[0] ) );

        final Cheese stilton = new Cheese( "stilton",
                                           6 );
        final DefaultFactHandle f1 = new DefaultFactHandle( 1,
                                                            stilton );

        // object should NOT assert as it does not pass test
        alphaNode.assertObject( f1,
                                context,
                                workingMemory );

        assertLength( 1,
                      sink.getAsserted() );
        assertNull( memory.facts );
        list = (Object[]) sink.getAsserted().get( 0 );
        assertSame( cheddar,
                    workingMemory.getObject( (DefaultFactHandle) list[0] ) );
    }

    /*
     * dont need to test with and without memory on this, as it was already done
     * on the previous two tests. This just test AlphaNode With a different
     * Constraint type.
     */
    public void testReturnValueConstraintAssertObject() throws Exception {
        RuleBaseConfiguration config = new RuleBaseConfiguration();
        config.setAlphaMemory( false );
        ReteooRuleBase ruleBase = (ReteooRuleBase) RuleBaseFactory.newRuleBase( config );
        BuildContext buildContext = new BuildContext( ruleBase,
                                                      ((ReteooRuleBase) ruleBase).getReteooBuilder().getIdGenerator() );
        ReteooWorkingMemory workingMemory = (ReteooWorkingMemory) ruleBase.newStatefulSession();

        final Rule rule = new Rule( "test-rule" );
        final PropagationContext context = new PropagationContextImpl( 0,
                                                                       PropagationContext.ASSERTION,
                                                                       null,
                                                                       null );

        final MockObjectSource source = new MockObjectSource( buildContext.getNextId() );

        final FieldExtractor extractor = cache.getExtractor( Cheese.class,
                                                             "type",
                                                             getClass().getClassLoader() );

        final FieldValue field = FieldFactory.getFieldValue( "cheddar" );

        final Evaluator evaluator = ValueType.OBJECT_TYPE.getEvaluator( Operator.EQUAL );

        final LiteralConstraint constraint = new LiteralConstraint( extractor,
                                                                    evaluator,
                                                                    field );

        final AlphaNode alphaNode = new AlphaNode( buildContext.getNextId(),
                                                   constraint,
                                                   source,
                                                   buildContext );
        final MockObjectSink sink = new MockObjectSink();
        alphaNode.addObjectSink( sink );

        final Cheese cheddar = new Cheese( "cheddar",
                                           5 );

        final DefaultFactHandle f0 = (DefaultFactHandle) workingMemory.insert( cheddar );

        assertLength( 0,
                      sink.getAsserted() );

        // object should assert as it passes text
        alphaNode.assertObject( f0,
                                context,
                                workingMemory );

        assertLength( 1,
                      sink.getAsserted() );
        final Object[] list = (Object[]) sink.getAsserted().get( 0 );
        assertSame( cheddar,
                    workingMemory.getObject( (DefaultFactHandle) list[0] ) );

        final Cheese stilton = new Cheese( "stilton",
                                           6 );
        f0.setObject( stilton );

        sink.getAsserted().clear();

        // object should not assert as it does not pass text
        alphaNode.assertObject( f0,
                                context,
                                workingMemory );

        assertLength( 0,
                      sink.getAsserted() );
    }

    public void testRetractObjectWithMemory() throws Exception {
        RuleBaseConfiguration config = new RuleBaseConfiguration();
        config.setAlphaMemory( true );
        ReteooRuleBase ruleBase = (ReteooRuleBase) RuleBaseFactory.newRuleBase( config );
        BuildContext buildContext = new BuildContext( ruleBase,
                                                      ((ReteooRuleBase) ruleBase).getReteooBuilder().getIdGenerator() );
        ReteooWorkingMemory workingMemory = (ReteooWorkingMemory) ruleBase.newStatefulSession();

        final Rule rule = new Rule( "test-rule" );
        final PropagationContext context = new PropagationContextImpl( 0,
                                                                       PropagationContext.ASSERTION,
                                                                       null,
                                                                       null );

        final MockObjectSource source = new MockObjectSource( buildContext.getNextId() );

        final FieldExtractor extractor = cache.getExtractor( Cheese.class,
                                                             "type",
                                                             getClass().getClassLoader() );

        final FieldValue field = FieldFactory.getFieldValue( "cheddar" );

        final Evaluator evaluator = ValueType.OBJECT_TYPE.getEvaluator( Operator.EQUAL );

        final LiteralConstraint constraint = new LiteralConstraint( extractor,
                                                                    evaluator,
                                                                    field );

        buildContext.setAlphaNodeMemoryAllowed( true );
        final AlphaNode alphaNode = new AlphaNode( buildContext.getNextId(),
                                                   constraint,
                                                   source,
                                                   buildContext ); // has memory
        final MockObjectSink sink = new MockObjectSink();
        alphaNode.addObjectSink( sink );

        final Cheese cheddar = new Cheese( "cheddar",
                                           5 );

        final DefaultFactHandle f0 = new DefaultFactHandle( 0,
                                                            cheddar );

        // check alpha memory is empty
        final AlphaMemory memory = (AlphaMemory) workingMemory.getNodeMemory( alphaNode );
        assertEquals( 0,
                      memory.facts.size() );

        // object should assert as it passes text
        alphaNode.assertObject( f0,
                                context,
                                workingMemory );

        assertEquals( 1,
                      memory.facts.size() );

        final DefaultFactHandle f1 = new DefaultFactHandle( 1,
                                                            "cheese" );

        // object should NOT retract as it doesn't exist
        alphaNode.retractObject( f1,
                                 context,
                                 workingMemory );

        assertLength( 0,
                      sink.getRetracted() );
        assertEquals( 1,
                      memory.facts.size() );
        assertTrue( "Should contain 'cheddar handle'",
                    memory.facts.contains( f0 ) );

        // object should retract as it does exist
        alphaNode.retractObject( f0,
                                 context,
                                 workingMemory );

        assertLength( 1,
                      sink.getRetracted() );
        assertEquals( 0,
                      memory.facts.size() );
        final Object[] list = (Object[]) sink.getRetracted().get( 0 );
        assertSame( f0,
                    list[0] );

    }

    public void testRetractObjectWithoutMemory() throws Exception {
        RuleBaseConfiguration config = new RuleBaseConfiguration();
        config.setAlphaMemory( false );
        ReteooRuleBase ruleBase = (ReteooRuleBase) RuleBaseFactory.newRuleBase( config );
        BuildContext buildContext = new BuildContext( ruleBase,
                                                      ((ReteooRuleBase) ruleBase).getReteooBuilder().getIdGenerator() );
        ReteooWorkingMemory workingMemory = (ReteooWorkingMemory) ruleBase.newStatefulSession();

        final Rule rule = new Rule( "test-rule" );
        final PropagationContext context = new PropagationContextImpl( 0,
                                                                       PropagationContext.ASSERTION,
                                                                       null,
                                                                       null );

        final MockObjectSource source = new MockObjectSource( buildContext.getNextId() );

        final FieldExtractor extractor = cache.getExtractor( Cheese.class,
                                                             "type",
                                                             getClass().getClassLoader() );

        final FieldValue field = FieldFactory.getFieldValue( "cheddar" );

        final Evaluator evaluator = ValueType.OBJECT_TYPE.getEvaluator( Operator.EQUAL );

        final LiteralConstraint constraint = new LiteralConstraint( extractor,
                                                                    evaluator,
                                                                    field );

        final AlphaNode alphaNode = new AlphaNode( buildContext.getNextId(),
                                                   constraint,
                                                   source,
                                                   buildContext ); // no memory
        final MockObjectSink sink = new MockObjectSink();
        alphaNode.addObjectSink( sink );

        final Cheese cheddar = new Cheese( "cheddar",
                                           5 );

        final DefaultFactHandle f0 = new DefaultFactHandle( 0,
                                                            cheddar );

        // check alpha memory is empty
        final AlphaMemory memory = (AlphaMemory) workingMemory.getNodeMemory( alphaNode );
        assertNull( memory.facts );

        // object should assert as it passes text
        alphaNode.assertObject( f0,
                                context,
                                workingMemory );

        assertNull( memory.facts );

        final DefaultFactHandle f1 = new DefaultFactHandle( 1,
                                                            new Cheese( "brie",
                                                                        10 ) );

        // object should NOT retract as it doesn't exist
        alphaNode.retractObject( f1,
                                 context,
                                 workingMemory );

        // without memory, it will always propagate a retract
        assertLength( 0,
                      sink.getRetracted() );
        assertNull( memory.facts );

        // object should retract as it does exist
        alphaNode.retractObject( f0,
                                 context,
                                 workingMemory );

        assertLength( 1,
                      sink.getRetracted() );
        assertNull( memory.facts );
        final Object[] list = (Object[]) sink.getRetracted().get( 0 );
        assertSame( f0,
                    list[0] );

    }

    public void testUpdateSinkWithMemory() throws FactException,
                                          IntrospectionException {
        // An AlphaNode with memory should not try and repropagate from its source
        // Also it should only update the latest tuple sinky
        RuleBaseConfiguration config = new RuleBaseConfiguration();
        config.setAlphaMemory( true );
        ReteooRuleBase ruleBase = (ReteooRuleBase) RuleBaseFactory.newRuleBase( config );
        BuildContext buildContext = new BuildContext( ruleBase,
                                                      ((ReteooRuleBase) ruleBase).getReteooBuilder().getIdGenerator() );
        ReteooWorkingMemory workingMemory = (ReteooWorkingMemory) ruleBase.newStatefulSession();

        final Rule rule = new Rule( "test-rule" );
        final PropagationContext context = new PropagationContextImpl( 0,
                                                                       PropagationContext.ASSERTION,
                                                                       null,
                                                                       null );

        final MockObjectSource source = new MockObjectSource( buildContext.getNextId() );

        final FieldExtractor extractor = cache.getExtractor( Cheese.class,
                                                             "type",
                                                             getClass().getClassLoader() );

        final FieldValue field = FieldFactory.getFieldValue( "cheddar" );

        final Evaluator evaluator = ValueType.OBJECT_TYPE.getEvaluator( Operator.EQUAL );

        final LiteralConstraint constraint = new LiteralConstraint( extractor,
                                                                    evaluator,
                                                                    field );

        buildContext.setAlphaNodeMemoryAllowed( true );
        final AlphaNode alphaNode = new AlphaNode( buildContext.getNextId(),
                                                   constraint,
                                                   source,
                                                   buildContext ); // has memory

        alphaNode.attach();

        final MockObjectSink sink1 = new MockObjectSink();
        alphaNode.addObjectSink( sink1 );

        // Assert a single fact which should be in the AlphaNode memory and also
        // propagated to the
        // the tuple sink
        final Cheese cheese = new Cheese( "cheddar",
                                          0 );
        final DefaultFactHandle handle1 = new DefaultFactHandle( 1,
                                                                 cheese );

        alphaNode.assertObject( handle1,
                                context,
                                workingMemory );

        // Create a fact that should not be propagated, since the alpha node restriction will filter it out
        final Cheese stilton = new Cheese( "stilton",
                                           10 );
        final DefaultFactHandle handle2 = new DefaultFactHandle( 2,
                                                                 stilton );
        // adding handle to the mock source
        source.addFact( handle2 );

        alphaNode.assertObject( handle2,
                                context,
                                workingMemory );
        assertLength( 1,
                      sink1.getAsserted() );

        // Attach a new tuple sink
        final MockObjectSink sink2 = new MockObjectSink();

        // Tell the alphanode to update the new node. Make sure the first sink1
        // is not updated
        // likewise the source should not do anything
        alphaNode.updateSink( sink2,
                              context,
                              workingMemory );

        assertLength( 1,
                      sink1.getAsserted() );
        assertLength( 1,
                      sink2.getAsserted() );
        assertEquals( 0,
                      source.getUdated() );
    }

    public void testUpdateSinkWithoutMemory() throws FactException,
                                             IntrospectionException {
        // An AlphaNode without memory should try and repropagate from its source
        RuleBaseConfiguration config = new RuleBaseConfiguration();
        config.setAlphaMemory( false );
        ReteooRuleBase ruleBase = (ReteooRuleBase) RuleBaseFactory.newRuleBase( config );
        BuildContext buildContext = new BuildContext( ruleBase,
                                                      ((ReteooRuleBase) ruleBase).getReteooBuilder().getIdGenerator() );
        ReteooWorkingMemory workingMemory = (ReteooWorkingMemory) ruleBase.newStatefulSession();

        final Rule rule = new Rule( "test-rule" );
        final PropagationContext context = new PropagationContextImpl( 0,
                                                                       PropagationContext.ASSERTION,
                                                                       null,
                                                                       null );

        final MockObjectSource source = new MockObjectSource( buildContext.getNextId() );

        final FieldExtractor extractor = cache.getExtractor( Cheese.class,
                                                             "type",
                                                             getClass().getClassLoader() );

        final FieldValue field = FieldFactory.getFieldValue( "cheddar" );

        final Evaluator evaluator = ValueType.OBJECT_TYPE.getEvaluator( Operator.EQUAL );

        final LiteralConstraint constraint = new LiteralConstraint( extractor,
                                                                    evaluator,
                                                                    field );

        final AlphaNode alphaNode = new AlphaNode( buildContext.getNextId(),
                                                   constraint,
                                                   source,
                                                   buildContext ); // no memory

        alphaNode.attach();

        final MockObjectSink sink1 = new MockObjectSink();
        alphaNode.addObjectSink( sink1 );

        // Assert a single fact which should be in the AlphaNode memory and also
        // propagated to the
        // the tuple sink
        final Cheese cheese = new Cheese( "cheddar",
                                          0 );
        final DefaultFactHandle handle1 = new DefaultFactHandle( 1,
                                                                 cheese );
        // adding handle to the mock source
        source.addFact( handle1 );

        alphaNode.assertObject( handle1,
                                context,
                                workingMemory );

        // Create a fact that should not be propagated, since the alpha node restriction will filter it out
        final Cheese stilton = new Cheese( "stilton",
                                           10 );
        final DefaultFactHandle handle2 = new DefaultFactHandle( 2,
                                                                 stilton );
        // adding handle to the mock source
        source.addFact( handle2 );

        alphaNode.assertObject( handle2,
                                context,
                                workingMemory );

        assertLength( 1,
                      sink1.getAsserted() );

        // Attach a new tuple sink
        final MockObjectSink sink2 = new MockObjectSink();

        // Tell the alphanode to update the new node. Make sure the first sink1
        // is not updated
        // likewise the source should not do anything
        alphaNode.updateSink( sink2,
                              context,
                              workingMemory );

        assertLength( 1,
                      sink1.getAsserted() );
        assertLength( 1,
                      sink2.getAsserted() );
        assertEquals( 1,
                      source.getUdated() );
    }

}