package org.drools.integrationtests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.drools.Address;
import org.drools.Cheese;
import org.drools.Person;
import org.drools.Primitives;
import org.drools.RuleBase;
import org.drools.RuleBaseConfiguration;
import org.drools.RuleBaseFactory;
import org.drools.StatefulSession;
import org.drools.WorkingMemory;
import org.drools.common.InternalFactHandle;
import org.drools.compiler.PackageBuilder;
import org.drools.rule.Package;
import org.drools.rule.Rule;
import org.mvel.ExpressionCompiler;
import org.mvel.ParserContext;

public class MarshallingTest extends TestCase {
    public void testSerializable() throws Exception {

        final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "test_Serializable.drl" ) );

        final PackageBuilder builder = new PackageBuilder();
        builder.addPackageFromDrl( reader );
        final Package pkg = serialisePackage( builder.getPackage() );

        assertEquals( 0,
                      builder.getErrors().getErrors().length );

        RuleBase ruleBase = getRuleBase();// RuleBaseFactory.newRuleBase();

        ruleBase.addPackage( pkg );

        Map map = new HashMap();
        map.put( "x",
                 ruleBase );
        final byte[] ast = serializeOut( map );
        map = (Map) serializeIn( ast );
        ruleBase = (RuleBase) map.get( "x" );
        final Rule[] rules = ruleBase.getPackages()[0].getRules();
        assertEquals( 4,
                      rules.length );

        assertEquals( "match Person 1",
                      rules[0].getName() );
        assertEquals( "match Person 2",
                      rules[1].getName() );
        assertEquals( "match Person 3",
                      rules[2].getName() );
        assertEquals( "match Integer",
                      rules[3].getName() );

        WorkingMemory workingMemory = ruleBase.newStatefulSession();

        workingMemory.setGlobal( "list",
                                 new ArrayList() );

        final Person bob = new Person( "bob" );
        workingMemory.insert( bob );

        final byte[] wm = serializeOut( workingMemory );

        workingMemory = ruleBase.newStatefulSession( new ByteArrayInputStream( wm ) );

        assertEquals( 1,
                      IteratorToList.convert( workingMemory.iterateObjects() ).size() );
        assertEquals( bob,
                      IteratorToList.convert( workingMemory.iterateObjects() ).get( 0 ) );

        assertEquals( 2,
                      workingMemory.getAgenda().agendaSize() );

        workingMemory.fireAllRules();

        final List list = (List) workingMemory.getGlobal( "list" );

        assertEquals( 3,
                      list.size() );
        // because of agenda-groups
        assertEquals( new Integer( 4 ),
                      list.get( 0 ) );

        assertEquals( 2,
                      IteratorToList.convert( workingMemory.iterateObjects() ).size() );
        assertTrue( IteratorToList.convert( workingMemory.iterateObjects() ).contains( bob ) );
        assertTrue( IteratorToList.convert( workingMemory.iterateObjects() ).contains( new Person( "help" ) ) );
    }

    public void testMVELSerialization() {
        String expression = "x";
        
        ExpressionCompiler compiler = new ExpressionCompiler( expression );
        ParserContext ctx = new ParserContext();
        ctx.addImport( "x", int.class );
        
        Serializable result = compiler.compile( ctx );
        
        try {
            byte[] out = serializeOut( result );
            Serializable in = (Serializable) serializeIn( out );
            assertNotNull( in );
        } catch ( Exception e ) {
            e.printStackTrace();
            fail( "Should not raise exceptio" );
        }
    }

    public void testSerializableCollectAccumulate() throws Exception {

        final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "test_SerializableCollectAccumulate.drl" ) );

        final PackageBuilder builder = new PackageBuilder();
        builder.addPackageFromDrl( reader );
        final Package pkg = serialisePackage( builder.getPackage() );

        assertEquals( builder.getErrors().toString(),
                      0,
                      builder.getErrors().getErrors().length );

        RuleBase ruleBase = getRuleBase();// RuleBaseFactory.newRuleBase();

        ruleBase.addPackage( pkg );

        Map map = new HashMap();
        map.put( "x",
                 ruleBase );
        final byte[] ast = serializeOut( map );
        map = (Map) serializeIn( ast );
        ruleBase = (RuleBase) map.get( "x" );
        final Rule[] rules = ruleBase.getPackages()[0].getRules();
        assertEquals( 2,
                      rules.length );

        assertEquals( "collect",
                      rules[0].getName() );
        assertEquals( "accumulate",
                      rules[1].getName() );

        WorkingMemory workingMemory = ruleBase.newStatefulSession();

        workingMemory.setGlobal( "list",
                                 new ArrayList() );

        final Person bob = new Person( "bob", "stilton" );
        final Cheese c1 = new Cheese( "stilton", 10 );
        final Cheese c2 = new Cheese( "brie", 8 );
        final Cheese c3 = new Cheese( "stilton", 5 );
        workingMemory.insert( bob );
        workingMemory.insert( c1 );
        workingMemory.insert( c2 );
        workingMemory.insert( c3 );

        final byte[] wm = serializeOut( workingMemory );

        workingMemory = ruleBase.newStatefulSession( new ByteArrayInputStream( wm ) );

        assertEquals( 4,
                      IteratorToList.convert( workingMemory.iterateObjects() ).size() );

        assertEquals( 2,
                      workingMemory.getAgenda().agendaSize() );

        workingMemory.fireAllRules();

        final List list = (List) workingMemory.getGlobal( "list" );

        assertEquals( 2,
                      list.size() );
        // because of agenda-groups
        assertEquals( 2,
                      ((List) list.get( 0 )).size() );
        assertEquals( 15,
                      ((Number) list.get( 1 )).intValue() );

    }

    public void testSerializeWorkingMemoryAndRuleBase1() throws Exception {
        // has the first newStatefulSession before the ruleBase is serialised
        final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "test_Serializable.drl" ) );

        final PackageBuilder builder = new PackageBuilder();
        builder.addPackageFromDrl( reader );
        final Package pkg = serialisePackage( builder.getPackage() );

        assertEquals( 0,
                      builder.getErrors().getErrors().length );

        RuleBase ruleBase = getRuleBase();// RuleBaseFactory.newRuleBase();

        WorkingMemory workingMemory = ruleBase.newStatefulSession();

        Map map = new HashMap();
        map.put( "x",
                 ruleBase );
        final byte[] ast = serializeOut( map );
        map = (Map) serializeIn( ast );
        ruleBase = (RuleBase) map.get( "x" );

        final byte[] wm = serializeOut( workingMemory );

        workingMemory = ruleBase.newStatefulSession( new ByteArrayInputStream( wm ) );

        ruleBase.addPackage( pkg );

        workingMemory.setGlobal( "list",
                                 new ArrayList() );

        final Person bob = new Person( "bob" );
        workingMemory.insert( bob );

        final Rule[] rules = ruleBase.getPackages()[0].getRules();

        assertEquals( 4,
                      rules.length );

        assertEquals( "match Person 1",
                      rules[0].getName() );
        assertEquals( "match Person 2",
                      rules[1].getName() );
        assertEquals( "match Person 3",
                      rules[2].getName() );
        assertEquals( "match Integer",
                      rules[3].getName() );

        assertEquals( 1,
                      IteratorToList.convert( workingMemory.iterateObjects() ).size() );
        assertEquals( bob,
                      IteratorToList.convert( workingMemory.iterateObjects() ).get( 0 ) );

        assertEquals( 2,
                      workingMemory.getAgenda().agendaSize() );

        workingMemory.fireAllRules();

        final List list = (List) workingMemory.getGlobal( "list" );

        assertEquals( 3,
                      list.size() );
        // because of agenda-groups
        assertEquals( new Integer( 4 ),
                      list.get( 0 ) );

        assertEquals( 2,
                      IteratorToList.convert( workingMemory.iterateObjects() ).size() );
        assertTrue( IteratorToList.convert( workingMemory.iterateObjects() ).contains( bob ) );
        assertTrue( IteratorToList.convert( workingMemory.iterateObjects() ).contains( new Person( "help" ) ) );

    }

    public void testSerializeWorkingMemoryAndRuleBase2() throws Exception {
        // has the first newStatefulSession after the ruleBase is serialised
        final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "test_Serializable.drl" ) );

        final PackageBuilder builder = new PackageBuilder();
        builder.addPackageFromDrl( reader );
        final Package pkg = serialisePackage( builder.getPackage() );

        assertEquals( 0,
                      builder.getErrors().getErrors().length );

        RuleBase ruleBase = getRuleBase();// RuleBaseFactory.newRuleBase();   

        // serialise a hashmap with the RuleBase as a key
        Map map = new HashMap();
        map.put( "x",
                 ruleBase );
        final byte[] ast = serializeOut( map );
        map = (Map) serializeIn( ast );
        ruleBase = (RuleBase) map.get( "x" );

        WorkingMemory workingMemory = ruleBase.newStatefulSession();

        // serialise the working memory before population
        final byte[] wm = serializeOut( workingMemory );
        workingMemory = ruleBase.newStatefulSession( new ByteArrayInputStream( wm ) );

        ruleBase.addPackage( pkg );

        workingMemory.setGlobal( "list",
                                 new ArrayList() );

        final Person bob = new Person( "bob" );
        workingMemory.insert( bob );

        final Rule[] rules = ruleBase.getPackages()[0].getRules();

        assertEquals( 4,
                      rules.length );

        assertEquals( "match Person 1",
                      rules[0].getName() );
        assertEquals( "match Person 2",
                      rules[1].getName() );
        assertEquals( "match Person 3",
                      rules[2].getName() );
        assertEquals( "match Integer",
                      rules[3].getName() );

        assertEquals( 1,
                      IteratorToList.convert( workingMemory.iterateObjects() ).size() );
        assertEquals( bob,
                      IteratorToList.convert( workingMemory.iterateObjects() ).get( 0 ) );

        assertEquals( 2,
                      workingMemory.getAgenda().agendaSize() );

        workingMemory.fireAllRules();

        final List list = (List) workingMemory.getGlobal( "list" );

        assertEquals( 3,
                      list.size() );
        // because of agenda-groups
        assertEquals( new Integer( 4 ),
                      list.get( 0 ) );

        assertEquals( 2,
                      IteratorToList.convert( workingMemory.iterateObjects() ).size() );
        assertTrue( IteratorToList.convert( workingMemory.iterateObjects() ).contains( bob ) );
        assertTrue( IteratorToList.convert( workingMemory.iterateObjects() ).contains( new Person( "help" ) ) );
    }

    public void FIXME_testSerializeWorkingMemoryAndRuleBase3() throws Exception {
        // has the first newStatefulSession after the ruleBase is serialised
        final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "test_Serializable.drl" ) );

        final PackageBuilder builder = new PackageBuilder();
        builder.addPackageFromDrl( reader );
        final Package pkg = serialisePackage( builder.getPackage() );

        assertEquals( 0,
                      builder.getErrors().getErrors().length );

        RuleBase ruleBase = getRuleBase();
        WorkingMemory workingMemory = ruleBase.newStatefulSession();

        ruleBase.addPackage( pkg );

        workingMemory.setGlobal( "list",
                                 new ArrayList() );

        final Person bob = new Person( "bob" );
        workingMemory.insert( bob );

        // serialise a hashmap with the RuleBase as a key, after WM population
        Map map = new HashMap();
        map.put( "x",
                 ruleBase );
        final byte[] ast = serializeOut( map );
        map = (Map) serializeIn( ast );
        ruleBase = (RuleBase) map.get( "x" );

        // now try serialising with a fully populated wm from a serialised rulebase
        final byte[] wm = serializeOut( workingMemory );
        workingMemory = ruleBase.newStatefulSession( new ByteArrayInputStream( wm ) );

        final Rule[] rules = ruleBase.getPackages()[0].getRules();

        assertEquals( 4,
                      rules.length );

        assertEquals( "match Person 1",
                      rules[0].getName() );
        assertEquals( "match Person 2",
                      rules[1].getName() );
        assertEquals( "match Person 3",
                      rules[2].getName() );
        assertEquals( "match Integer",
                      rules[3].getName() );

        assertEquals( 1,
                      IteratorToList.convert( workingMemory.iterateObjects() ).size() );
        assertEquals( bob,
                      IteratorToList.convert( workingMemory.iterateObjects() ).get( 0 ) );

        assertEquals( 2,
                      workingMemory.getAgenda().agendaSize() );

        workingMemory.fireAllRules();

        final List list = (List) workingMemory.getGlobal( "list" );

        assertEquals( 3,
                      list.size() );
        // because of agenda-groups
        assertEquals( new Integer( 4 ),
                      list.get( 0 ) );

        assertEquals( 2,
                      IteratorToList.convert( workingMemory.iterateObjects() ).size() );
        assertTrue( IteratorToList.convert( workingMemory.iterateObjects() ).contains( bob ) );
        assertTrue( IteratorToList.convert( workingMemory.iterateObjects() ).contains( new Person( "help" ) ) );
    }

    public void testSerializeAdd() throws Exception {

        //Create a rulebase, a session, and test it
        RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        PackageBuilder builder = new PackageBuilder();
        builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_Dynamic1.drl" ) ) );
        Package pkg = serialisePackage( builder.getPackage() );
        ruleBase.addPackage( pkg );

        StatefulSession session = ruleBase.newStatefulSession();
        List list = new ArrayList();
        session.setGlobal( "list",
                           list );

        InternalFactHandle stilton = (InternalFactHandle) session.insert( new Cheese( "stilton",
                                                                                      10 ) );
        InternalFactHandle brie = (InternalFactHandle) session.insert( new Cheese( "brie",
                                                                                   10 ) );
        session.fireAllRules();

        assertEquals( list.size(),
                      1 );
        assertEquals( "stilton",
                      list.get( 0 ) );

        byte[] serializedSession = serializeOut( session );
        session.dispose();

        byte[] serializedRulebase = serializeOut( ruleBase );

        // now recreate the rulebase, deserialize the session and test it
        ruleBase = (RuleBase) serializeIn( serializedRulebase );

        session = ruleBase.newStatefulSession( new ByteArrayInputStream( serializedSession ) );
        list = (List) session.getGlobal( "list" );

        assertNotNull( list );
        assertEquals( list.size(),
                      1 );
        assertEquals( "stilton",
                      list.get( 0 ) );

        builder = new PackageBuilder();
        builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_Dynamic3.drl" ) ) );
        pkg = serialisePackage( builder.getPackage() );
        ruleBase.addPackage( pkg );

        InternalFactHandle stilton2 = (InternalFactHandle) session.insert( new Cheese( "stilton",
                                                                                       10 ) );
        InternalFactHandle brie2 = (InternalFactHandle) session.insert( new Cheese( "brie",
                                                                                    10 ) );
        InternalFactHandle bob = (InternalFactHandle) session.insert( new Person( "bob",
                                                                                  30 ) );
        session.fireAllRules();

        assertEquals( list.size(),
                      3 );
        assertEquals( bob.getObject(),
                      list.get( 1 ) );
        assertEquals( "stilton",
                      list.get( 2 ) );

        session.dispose();

    }

    public void testSerializationOfIndexedWM() throws Exception {
        final Reader reader = new InputStreamReader( getClass().getResourceAsStream( "test_Serializable2.drl" ) );

        final PackageBuilder builder = new PackageBuilder();
        builder.addPackageFromDrl( reader );
        final Package pkg = builder.getPackage();

        assertEquals( builder.getErrors().toString(),
                      0,
                      builder.getErrors().getErrors().length );

        RuleBase ruleBase = getRuleBase();// RuleBaseFactory.newRuleBase();

        ruleBase.addPackage( pkg );

        Map map = new HashMap();
        map.put( "x",
                 ruleBase );
        final byte[] ast = serializeOut( map );
        map = (Map) serializeIn( ast );
        ruleBase = (RuleBase) map.get( "x" );
        final Rule[] rules = ruleBase.getPackages()[0].getRules();
        assertEquals( 3,
                      rules.length );

        WorkingMemory workingMemory = ruleBase.newStatefulSession();

        workingMemory.setGlobal( "list",
                                 new ArrayList() );

        final Primitives p = new Primitives();
        p.setBytePrimitive( (byte) 1 );
        p.setShortPrimitive( (short) 2 );
        p.setIntPrimitive( (int) 3 );
        workingMemory.insert( p );

        final byte[] wm = serializeOut( workingMemory );

        workingMemory = ruleBase.newStatefulSession( new ByteArrayInputStream( wm ) );

        assertEquals( 1,
                      IteratorToList.convert( workingMemory.iterateObjects() ).size() );
        assertEquals( p,
                      IteratorToList.convert( workingMemory.iterateObjects() ).get( 0 ) );

        assertEquals( 3,
                      workingMemory.getAgenda().agendaSize() );

        workingMemory.fireAllRules();

        final List list = (List) workingMemory.getGlobal( "list" );

        assertEquals( 3,
                      list.size() );
        // because of agenda-groups
        assertEquals( "1",
                      list.get( 0 ) );
        assertEquals( "2",
                      list.get( 1 ) );
        assertEquals( "3",
                      list.get( 2 ) );

    }

    /*
     *  Here I am inserting data points which are not used by any rule (e.g Person).
     *  Later adding rule (e.g. Rule: 'match Person') for those data points.
     *  
     *  Result: Pkg/Rule addition is failing with ClassCastException
     */
    public void testSerializeAdd2() throws Exception {

        //Create a rulebase, a session, and test it
        RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        PackageBuilder builder = new PackageBuilder();
        builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_Dynamic1_0.drl" ) ) );
        Package pkg = serialisePackage( builder.getPackage() );
        ruleBase.addPackage( pkg );

        List results = new ArrayList();
        StatefulSession session = ruleBase.newStatefulSession();
        session.setGlobal( "results",
                           results );

        InternalFactHandle stilton1 = (InternalFactHandle) session.insert( new Cheese( "stilton",
                                                                                       10 ) );
        session.insert( new Cheese( "brie",
                                    10 ) );
        InternalFactHandle bob = (InternalFactHandle) session.insert( new Person( "bob",
                                                                                  10 ) );

        // fire rules
        session.fireAllRules();
        // check the results are correct
        assertEquals( 1,
                      results.size() );
        assertEquals( stilton1.getObject(),
                      results.get( 0 ) );

        // serialize session and rulebase out
        byte[] serializedSession = serializeOut( session );
        byte[] serializedRulebase = serializeOut( ruleBase );
        session.dispose();

        // now deserialize the rulebase, deserialize the session and test it
        ruleBase = (RuleBase) serializeIn( serializedRulebase );
        session = ruleBase.newStatefulSession( new ByteArrayInputStream( serializedSession ) );

        // dynamically add a new package
        builder = new PackageBuilder();
        builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_Dynamic3_0.drl" ) ) );
        pkg = serialisePackage( builder.getPackage() );
        ruleBase.addPackage( pkg );

        InternalFactHandle stilton2 = (InternalFactHandle) session.insert( new Cheese( "stilton",
                                                                                       20 ) );
        session.insert( new Cheese( "brie",
                                    20 ) );
        InternalFactHandle mark = (InternalFactHandle) session.insert( new Person( "mark",
                                                                                   20 ) );
        session.fireAllRules();

        results = (List) session.getGlobal( "results" );
        assertEquals( 4,
                      results.size() );
        assertEquals( bob.getObject(),
                      results.get( 1 ) );
        assertEquals( stilton2.getObject(),
                      results.get( 2 ) );
        assertEquals( mark.getObject(),
                      results.get( 3 ) );

        serializedSession = null;
        serializedRulebase = null;

        serializedSession = serializeOut( session );
        serializedRulebase = serializeOut( ruleBase );

        // dispose session
        session.dispose();

    }

    /*
     *  Here I am inserting data points(e.g. Cheese) with  'stilton' / 'brie' as type value.
     *  Initially I had only 1 rule ('like stilton') for data points(e.g. Cheese) having type as 'stilton'.
     *
     *  Later added new rule ('like brie')  for data points(e.g. Cheese) having type as 'brie'. 
     *  
     *  Result: new rule is not getting fired for new data points having type as 'brie'.
     *          Only for old data points having type as 'brie' the new rule got fired.
     */
    public void testSerializeAdd_newRuleNotFiredForNewData() throws Exception {
        //Create a rulebase, a session, and test it
        RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        PackageBuilder builder = new PackageBuilder();
        builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_Dynamic1_0.drl" ) ) );
        Package pkg = serialisePackage( builder.getPackage() );
        ruleBase.addPackage( pkg );

        List results = new ArrayList();
        StatefulSession session = ruleBase.newStatefulSession();
        session.setGlobal( "results",
                           results );

        InternalFactHandle stilton1 = (InternalFactHandle) session.insert( new Cheese( "stilton",
                                                                                       10 ) );
        InternalFactHandle brie1 = (InternalFactHandle) session.insert( new Cheese( "brie",
                                                                                    10 ) );
        session.fireAllRules();

        byte[] serializedSession = serializeOut( session );
        byte[] serializedRulebase = serializeOut( ruleBase );

        session.dispose();

        assertEquals( 1,
                      results.size() );
        assertEquals( stilton1.getObject(),
                      results.get( 0 ) );

        // now recreate the rulebase, deserialize the session and test it
        ruleBase = (RuleBase) serializeIn( serializedRulebase );
        session = ruleBase.newStatefulSession( new ByteArrayInputStream( serializedSession ) );
        results = (List) session.getGlobal( "results" );

        builder = new PackageBuilder();
        builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_Dynamic1_1.drl" ) ) );
        pkg = serialisePackage( builder.getPackage() );
        ruleBase.addPackage( pkg );
        
        InternalFactHandle stilton2 = (InternalFactHandle) session.insert( new Cheese( "stilton",
                                    20 ) );
        InternalFactHandle brie2 = (InternalFactHandle) session.insert( new Cheese( "brie",
                                    20 ) );
        InternalFactHandle brie3 = (InternalFactHandle) session.insert( new Cheese( "brie",
                                    30 ) );
        session.fireAllRules();
        assertEquals( 5,
                      results.size() );
        assertEquals( brie1.getObject(),
                      results.get( 1 ) );
        assertEquals( stilton2.getObject(),
                      results.get( 2 ) );
        assertEquals( brie2.getObject(),
                      results.get( 4 ) );
        assertEquals( brie3.getObject(),
                      results.get( 3 ) );

        serializedSession = null;
        serializedRulebase = null;

        serializedSession = serializeOut( session );
        serializedRulebase = serializeOut( ruleBase );

        session.dispose();
    }

    /*
     *  Works Fine if both the scenarios mentioned above are skipped. 
     */
    public void testSerializeAdd3() throws Exception {
        //Create a rulebase, a session, and test it
        RuleBase ruleBase = RuleBaseFactory.newRuleBase( );
        PackageBuilder builder = new PackageBuilder();
        builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_Dynamic1_0.drl" ) ) );
        Package pkg = serialisePackage( builder.getPackage() );
        ruleBase.addPackage( pkg );
        
        List results = new ArrayList();
        StatefulSession session = ruleBase.newStatefulSession();
        session.setGlobal( "results",
                           results );

        InternalFactHandle stilton1 = (InternalFactHandle) session.insert( new Cheese( "stilton",
                                                                                       10 ) );
        InternalFactHandle brie1 = (InternalFactHandle) session.insert( new Cheese( "brie",
                                                                                    10 ) );
        session.fireAllRules();

        byte[] serializedSession = serializeOut( session );
        byte[] serializedRulebase = serializeOut( ruleBase );

        session.dispose();

        assertEquals( 1,
                      results.size() );
        assertEquals( stilton1.getObject(),
                      results.get( 0 ) );

        // now recreate the rulebase, deserialize the session and test it
        ruleBase = (RuleBase) serializeIn( serializedRulebase );
        session = ruleBase.newStatefulSession( new ByteArrayInputStream( serializedSession ) );
        results = (List) session.getGlobal( "results" );
        
        builder = new PackageBuilder();
        builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_Dynamic3_0.drl" ) ) );
        pkg = serialisePackage( builder.getPackage() );
        
        ruleBase.addPackage( pkg );
        
        InternalFactHandle stilton2 = (InternalFactHandle) session.insert( new Cheese( "stilton", 20 ) );
        InternalFactHandle brie2 = (InternalFactHandle) session.insert( new Cheese( "brie", 20 ) );
        InternalFactHandle bob1 = (InternalFactHandle) session.insert( new Person( "bob", 20 ) );
        InternalFactHandle bob2 = (InternalFactHandle) session.insert( new Person( "bob", 30 ) );
        session.fireAllRules();
 
        assertEquals( 4,
                      results.size() );
        assertEquals( stilton2.getObject(),
                      results.get( 1 ) );
        assertEquals( bob2.getObject(),
                      results.get( 2 ) );
        assertEquals( bob1.getObject(),
                      results.get( 3 ) );

        serializedSession = null;
        serializedRulebase = null;
        
        serializedSession = serializeOut( session );
        serializedRulebase = serializeOut( ruleBase );
        
        session.dispose();
        
        // now recreate the rulebase, deserialize the session and test it
        ruleBase = (RuleBase) serializeIn( serializedRulebase );
        session = ruleBase.newStatefulSession( new ByteArrayInputStream( serializedSession ) );
        results = (List) session.getGlobal( "results" );
        
        builder = new PackageBuilder();
        builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_Dynamic1_2.drl" ) ) );
        pkg = serialisePackage( builder.getPackage() );
        ruleBase.addPackage( pkg );
        
        InternalFactHandle stilton3 = (InternalFactHandle) session.insert( new Cheese( "stilton", 40 ) );
        InternalFactHandle brie3 = (InternalFactHandle) session.insert( new Cheese( "brie", 40 ) );
        InternalFactHandle bob3 = (InternalFactHandle) session.insert( new Person( "bob", 40 ) );
        InternalFactHandle bob4 = (InternalFactHandle) session.insert( new Person( "bob", 40 ) );
        InternalFactHandle addr1 = (InternalFactHandle) session.insert( new Address( "bangalore" ) );
        InternalFactHandle addr2 = (InternalFactHandle) session.insert( new Address( "India" ) );
        
        session.fireAllRules();
 
        assertEquals( 9,
                      results.size() );
        assertEquals( stilton3.getObject(),
                      results.get( 4 ) );
        assertEquals( bob4.getObject(),
                      results.get( 5 ) );
        assertEquals( bob3.getObject(),
                      results.get( 6 ) );
        assertEquals( addr2.getObject(),
                      results.get( 7 ) );
        assertEquals( addr1.getObject(),
                      results.get( 8 ) );

        serializedSession = null;
        serializedRulebase = null;
        
        serializedSession = serializeOut( session );
        serializedRulebase = serializeOut( ruleBase );
        
        session.dispose();
        
        // now recreate the rulebase, deserialize the session and test it
        ruleBase = (RuleBase) serializeIn( serializedRulebase );
        session = ruleBase.newStatefulSession( new ByteArrayInputStream( serializedSession ) );
        results = (List) session.getGlobal( "results" );
        
        InternalFactHandle stilton4 = (InternalFactHandle) session.insert( new Cheese( "stilton", 50 ) );
        InternalFactHandle brie4 = (InternalFactHandle) session.insert( new Cheese( "brie", 50 ) );
        InternalFactHandle bob5 = (InternalFactHandle) session.insert( new Person( "bob", 50 ) );
        InternalFactHandle bob6 = (InternalFactHandle) session.insert( new Person( "bob", 50 ) );
        InternalFactHandle addr3 = (InternalFactHandle) session.insert( new Address( "Tripura" ) );
        InternalFactHandle addr4 = (InternalFactHandle) session.insert( new Address( "Agartala" ) );
        
        session.fireAllRules();
 
        assertEquals( 14,
                      results.size() );
        assertEquals( stilton4.getObject(),
                      results.get( 9 ) );
        assertEquals( bob6.getObject(),
                      results.get( 10 ) );
        assertEquals( bob5.getObject(),
                      results.get( 11 ) );
        assertEquals( addr4.getObject(),
                      results.get( 12 ) );
        assertEquals( addr3.getObject(),
                      results.get( 13 ) );

        serializedSession = null;
        serializedRulebase = null;
        
        serializedSession = serializeOut( session );
        serializedRulebase = serializeOut( ruleBase );
        
        session.dispose();
       
    }
    
    /*
     * I have tried both the scenarios
     * 1. Remove a rule from a pkg.
     * 2. Remove a pkg
     * 
     * But both cases after inserting associated data points (i.e data points which are used to fire/activate the removed rule)
     * session.fireAllRules() is throwing NoClassDefFoundError
     * 
     */
    public void testSerializeAddRemove_NoClassDefFoundError() throws Exception {

        //Create a rulebase, a session, and test it
        RuleBase ruleBase = RuleBaseFactory.newRuleBase( );
        PackageBuilder builder = new PackageBuilder();
        builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_Dynamic1_0.drl" ) ) );
        Package pkg = serialisePackage( builder.getPackage() );
        ruleBase.addPackage( pkg );
        
        List results = new ArrayList();
        StatefulSession session = ruleBase.newStatefulSession();
        session.setGlobal( "results",
                           results );

        InternalFactHandle stilton1 = (InternalFactHandle) session.insert( new Cheese( "stilton",
                                                                                       10 ) );
        InternalFactHandle brie1 = (InternalFactHandle) session.insert( new Cheese( "brie",
                                                                                    10 ) );
        session.fireAllRules();

        byte[] serializedSession = serializeOut( session );
        byte[] serializedRulebase = serializeOut( ruleBase );

        session.dispose();

        assertEquals( 1,
                      results.size() );
        assertEquals( stilton1.getObject(),
                      results.get( 0 ) );

        // now recreate the rulebase, deserialize the session and test it
        ruleBase = (RuleBase) serializeIn( serializedRulebase );
        session = ruleBase.newStatefulSession( new ByteArrayInputStream( serializedSession ) );
        results = (List) session.getGlobal( "results" );
        
        builder = new PackageBuilder();
        builder.addPackageFromDrl( new InputStreamReader( getClass().getResourceAsStream( "test_Dynamic3_0.drl" ) ) );
        pkg = serialisePackage( builder.getPackage() );
        
        ruleBase.addPackage( pkg );
        
        InternalFactHandle stilton2 = (InternalFactHandle) session.insert( new Cheese( "stilton", 20 ) );
        InternalFactHandle brie2 = (InternalFactHandle) session.insert( new Cheese( "brie", 20 ) );
        InternalFactHandle bob1 = (InternalFactHandle) session.insert( new Person( "bob", 20 ) );
        InternalFactHandle bob2 = (InternalFactHandle) session.insert( new Person( "bob", 30 ) );
        session.fireAllRules();
 
        assertEquals( 4,
                      results.size() );
        assertEquals( stilton2.getObject(),
                      results.get( 1 ) );
        assertEquals( bob2.getObject(),
                      results.get( 2 ) );
        assertEquals( bob1.getObject(),
                      results.get( 3 ) );

        serializedSession = null;
        serializedRulebase = null;
        
        serializedSession = serializeOut( session );
        serializedRulebase = serializeOut( ruleBase );
        
        session.dispose();
        
        // now recreate the rulebase, deserialize the session and test it
        ruleBase = (RuleBase) serializeIn( serializedRulebase );
        session = ruleBase.newStatefulSession( new ByteArrayInputStream( serializedSession ) );
        results = (List) session.getGlobal( "results" );
       
        // CASE 1: remove rule
        ruleBase.removeRule("org.drools.test", "like stilton");
        
        InternalFactHandle stilton3 = (InternalFactHandle) session.insert( new Cheese( "stilton", 20 ) );
        InternalFactHandle brie3 = (InternalFactHandle) session.insert( new Cheese( "brie", 20 ) );
        InternalFactHandle bob3 = (InternalFactHandle) session.insert( new Person( "bob", 20 ) );
        InternalFactHandle bob4 = (InternalFactHandle) session.insert( new Person( "bob", 30 ) );
        session.fireAllRules();
 
        assertEquals( 6,
                      results.size() );
        assertEquals( bob4.getObject(),
                      results.get( 4 ) );
        assertEquals( bob3.getObject(),
                      results.get( 5 ) );


        // now recreate the rulebase, deserialize the session and test it
        ruleBase = (RuleBase) serializeIn( serializedRulebase );
        session = ruleBase.newStatefulSession( new ByteArrayInputStream( serializedSession ) );
        results = (List) session.getGlobal( "results" );
       
        // CASE 2: remove pkg
        ruleBase.removePackage("org.drools.test");
        
        InternalFactHandle stilton4 = (InternalFactHandle) session.insert( new Cheese( "stilton", 20 ) );
        InternalFactHandle brie4 = (InternalFactHandle) session.insert( new Cheese( "brie", 20 ) );
        InternalFactHandle bob5 = (InternalFactHandle) session.insert( new Person( "bob", 20 ) );
        InternalFactHandle bob6 = (InternalFactHandle) session.insert( new Person( "bob", 30 ) );
        session.fireAllRules();
 
        assertEquals( 6,
                      results.size() );
        assertEquals( bob6.getObject(),
                      results.get( 4 ) );
        assertEquals( bob5.getObject(),
                      results.get( 5 ) );
        
        serializedSession = null;
        serializedRulebase = null;
        
        // Now serialize rulebase and session again
        serializedRulebase = serializeOut( ruleBase );
        serializedSession = serializeOut( session );
        
        session.dispose();

        // Deserialize the rulebase and the session 
        ruleBase = (RuleBase) serializeIn( serializedRulebase );
        session = ruleBase.newStatefulSession( new ByteArrayInputStream( serializedSession ) );    //  throws java.lang.ClassNotFoundException Exception
        results = (List) session.getGlobal( "results" );
             
        InternalFactHandle stilton5 = (InternalFactHandle) session.insert( new Cheese( "stilton", 30 ) );
        InternalFactHandle brie5 = (InternalFactHandle) session.insert( new Cheese( "brie", 30 ) );
        InternalFactHandle bob7 = (InternalFactHandle) session.insert( new Person( "bob", 30 ) );
        InternalFactHandle bob8 = (InternalFactHandle) session.insert( new Person( "bob", 40 ) );
        session.fireAllRules();
 
        assertEquals( 8,
                      results.size() );
        assertEquals( bob8.getObject(),
                      results.get( 6 ) );
        assertEquals( bob7.getObject(),
                      results.get( 7 ) );
       
        serializedSession = null;
        serializedRulebase = null;
       
        serializedSession = serializeOut( session );
        serializedRulebase = serializeOut( ruleBase );
       
        session.dispose();        
       
    }
    
    protected RuleBase getRuleBase() throws Exception {

        return RuleBaseFactory.newRuleBase( RuleBase.RETEOO,
                                            null );
    }

    protected RuleBase getRuleBase(final RuleBaseConfiguration config) throws Exception {

        return RuleBaseFactory.newRuleBase( RuleBase.RETEOO,
                                            config );
    }

    protected Package serialisePackage(Package pkg) {
        try {
            byte[] bytes = serializeOut( pkg );
            return (Package) serializeIn( bytes );
        } catch ( Exception e ) {
            throw new RuntimeException( "trouble serialising package.",
                                        e );
        }
    }

    protected Object serializeIn(final byte[] bytes) throws IOException,
                                                    ClassNotFoundException {
        final ObjectInput in = new ObjectInputStream( new ByteArrayInputStream( bytes ) );
        final Object obj = in.readObject();
        in.close();
        return obj;
    }

    protected byte[] serializeOut(final Object obj) throws IOException {
        // Serialize to a byte array
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutput out = new ObjectOutputStream( bos );
        out.writeObject( obj );
        out.close();

        // Get the bytes of the serialized object
        final byte[] bytes = bos.toByteArray();
        return bytes;
    }
}
