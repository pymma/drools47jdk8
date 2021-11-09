package org.drools.brms.server.builder;
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



import java.io.InputStream;
import java.util.Map;

import junit.framework.TestCase;

import org.acme.insurance.Driver;
import org.acme.insurance.Policy;
import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.WorkingMemory;
import org.drools.brms.client.common.AssetFormats;
import org.drools.brms.client.modeldriven.SuggestionCompletionEngine;
import org.drools.brms.client.modeldriven.brl.ActionFieldValue;
import org.drools.brms.client.modeldriven.brl.ActionSetField;
import org.drools.brms.client.modeldriven.brl.DSLSentence;
import org.drools.brms.client.modeldriven.brl.FactPattern;
import org.drools.brms.client.modeldriven.brl.RuleModel;
import org.drools.brms.server.selector.AssetSelector;
import org.drools.brms.server.selector.SelectorManager;
import org.drools.brms.server.util.BRXMLPersistence;
import org.drools.brms.server.util.TestEnvironmentSessionHelper;
import org.drools.repository.AssetItem;
import org.drools.repository.PackageItem;
import org.drools.repository.RulesRepository;
import org.drools.rule.Package;
import org.drools.ruleflow.core.RuleFlowProcess;

/**
 * This will unit test package assembly into a binary.
 * @author Michael Neale
 */
public class ContentPackageAssemblerTest extends TestCase {

    /**
     * Test package configuration errors,
     * including header, functions, DSL files.
     */
    public void testPackageConfigWithErrors() throws Exception {
        //test the config, no rule assets yet
        RulesRepository repo = getRepo();
        PackageItem pkg = repo.createPackage( "testBuilderPackageConfig", "x" );
        pkg.updateHeader( "import java.util.List" );
        AssetItem func = pkg.addAsset( "func1", "a function" );
        func.updateFormat( AssetFormats.FUNCTION );
        func.updateContent( "function void doSomething() { \n System.err.println(List.class.toString()); }" );
        func.checkin( "yeah" );

        func = pkg.addAsset( "func2", "q" );
        func.updateFormat( AssetFormats.FUNCTION );
        func.updateContent( "function void foo() { \nSystem.err.println(42); \n}");
        func.checkin( "" );

        AssetItem ass = pkg.addAsset( "dsl", "m");
        ass.updateFormat( AssetFormats.DSL );
        ass.updateContent( "[when]Foo bar=String()" );
        ass.checkin( "" );
        repo.save();


        //now lets light it up
        ContentPackageAssembler assembler = new ContentPackageAssembler(pkg);
        assertFalse(assembler.hasErrors());
        Package bin = assembler.getBinaryPackage();
        assertNotNull(bin);
        assertEquals("testBuilderPackageConfig", bin.getName());
        assertEquals(2, bin.getFunctions().size());

        assertTrue(bin.isValid());
        assertEquals(1, assembler.builder.getDSLMappingFiles().size());


        pkg.updateHeader( "koo koo ca choo" );
        assembler = new ContentPackageAssembler(pkg);
        assertTrue(assembler.hasErrors());
        assertTrue(assembler.isPackageConfigurationInError());

        pkg.updateHeader( "import java.util.Date" );
        assembler = new ContentPackageAssembler(pkg);
        assertTrue(assembler.hasErrors());
        assertTrue(assembler.getErrors().get(0).itemInError instanceof AssetItem);

        assertEquals("func1", assembler.getErrors().get( 0 ).itemInError.getName());
        try {
            assembler.getBinaryPackage();
            fail("should not work as is in error.");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
        }

        //fix it up
        pkg.updateHeader( "import java.util.List" );
        assembler = new ContentPackageAssembler(pkg);
        assertFalse(assembler.hasErrors());

        //now break a DSL and check the error
        ass.updateContent( "rubbish" );
        ass.checkin( "" );
        assembler = new ContentPackageAssembler(pkg);
        assertTrue(assembler.hasErrors());
        assertTrue(assembler.getErrors().get( 0 ).itemInError.getName().equals( ass.getName() ));
        assertNotEmpty(assembler.getErrors().get( 0 ).errorReport);
        assertFalse(assembler.isPackageConfigurationInError());

        //now fix it up
        ass.updateContent( "[when]foo=String()" );
        ass.checkin( "" );
        assembler = new ContentPackageAssembler(pkg);
        assertFalse(assembler.hasErrors());

        //break a func, and check for error
        func.updateContent( "goo" );
        func.checkin( "" );
        assembler = new ContentPackageAssembler(pkg);
        assertTrue(assembler.hasErrors());
        assertFalse(assembler.isPackageConfigurationInError());
        assertTrue(assembler.getErrors().get( 0 ).itemInError.getName().equals( func.getName() ));
        assertNotEmpty(assembler.getErrors().get( 0 ).errorReport);
    }


    public void testPackageWithRuleflow() throws Exception {
        RulesRepository repo = getRepo();

        PackageItem pkg = repo.createPackage( "testPackageWithRuleFlow", "" );
        AssetItem model = pkg.addAsset( "model", "qed" );
        model.updateFormat( AssetFormats.MODEL );

        model.updateBinaryContentAttachment( this.getClass().getResourceAsStream( "/billasurf.jar" ) );
        model.checkin( "" );

        pkg.updateHeader( "import com.billasurf.Board\n global com.billasurf.Person customer" );

        AssetItem rule1 = pkg.addAsset( "rule_1", "" );
        rule1.updateFormat( AssetFormats.DRL );
        rule1.updateContent( "rule 'rule1' \n when Board() \n then customer.setAge(42); \n end");
        rule1.checkin( "" );

        AssetItem ruleFlow = pkg.addAsset( "ruleFlow", "" );
        ruleFlow.updateFormat( AssetFormats.RULE_FLOW_RF );

        ruleFlow.updateBinaryContentAttachment( this.getClass().getResourceAsStream( "/ruleflow.rfm" ) );
        ruleFlow.checkin( "" );

        ContentPackageAssembler asm = new ContentPackageAssembler(pkg);
        assertFalse(asm.hasErrors());
        Map flows = asm.getBinaryPackage().getRuleFlows();
        assertNotNull(flows);

        assertEquals(1, flows.size());
        Object flow = flows.values().iterator().next();
        assertNotNull(flow);
        assertTrue(flow instanceof RuleFlowProcess);

    }

    public void testSimplePackageBuildNoErrors() throws Exception {
        RulesRepository repo = getRepo();

        PackageItem pkg = repo.createPackage( "testSimplePackageBuildNoErrors", "" );
        AssetItem model = pkg.addAsset( "model", "qed" );
        model.updateFormat( AssetFormats.MODEL );

        model.updateBinaryContentAttachment( this.getClass().getResourceAsStream( "/billasurf.jar" ) );
        model.checkin( "" );

        pkg.updateHeader( "import com.billasurf.Board\n global com.billasurf.Person customer" );

        AssetItem rule1 = pkg.addAsset( "rule_1", "" );
        rule1.updateFormat( AssetFormats.DRL );
        rule1.updateContent( "rule 'rule1' \n when Board() \n then customer.setAge(42); \n end");
        rule1.checkin( "" );

        AssetItem rule2 = pkg.addAsset( "rule2", "" );
        rule2.updateFormat( AssetFormats.DRL );
        rule2.updateContent( "agenda-group 'q' \n when \n Board() \n then \n System.err.println(42);" );
        rule2.checkin( "" );

        AssetItem rule3 = pkg.addAsset( "A file", "" );
        rule3.updateFormat( AssetFormats.DRL );
        rule3.updateContent( "package testSimplePackageBuildNoErrors\n rule 'rule3' \n when \n then \n customer.setAge(43); \n end \n" +
                "rule 'rule4' \n when \n then \n System.err.println(44); \n end" );
        rule3.checkin( "" );

        repo.save();


        ContentPackageAssembler asm = new ContentPackageAssembler(pkg);
        assertFalse(asm.hasErrors());
        assertNotNull(asm.getBinaryPackage());
        Package bin = asm.getBinaryPackage();
        assertEquals(pkg.getName(), bin.getName());
        assertTrue(bin.isValid());

        assertEquals(4, bin.getRules().length);

        //now create a snapshot
        repo.createPackageSnapshot( pkg.getName(), "SNAP_1" );

        //and screw up the the non snapshot one
        pkg.updateHeader( "koo koo ca choo" );
        asm = new ContentPackageAssembler(pkg);
        assertTrue(asm.hasErrors());


        //check the snapshot is kosher
        pkg = repo.loadPackageSnapshot( pkg.getName(), "SNAP_1" );
        asm = new ContentPackageAssembler(pkg);
        assertFalse(asm.hasErrors());

    }

    public void testIgnoreArchivedItems() throws Exception {
        RulesRepository repo = getRepo();

        PackageItem pkg = repo.createPackage( "testIgnoreArchivedItems", "" );
        AssetItem model = pkg.addAsset( "model", "qed" );
        model.updateFormat( AssetFormats.MODEL );

        model.updateBinaryContentAttachment( this.getClass().getResourceAsStream( "/billasurf.jar" ) );
        model.checkin( "" );

        pkg.updateHeader( "import com.billasurf.Board\n global com.billasurf.Person customer" );

        AssetItem rule1 = pkg.addAsset( "rule_1", "" );
        rule1.updateFormat( AssetFormats.DRL );
        rule1.updateContent( "rule 'rule1' \n when Board() \n then customer.setAge(42); \n end");
        rule1.checkin( "" );

        AssetItem rule2 = pkg.addAsset( "rule2", "" );
        rule2.updateFormat( AssetFormats.DRL );
        rule2.updateContent( "agenda-group 'q' \n when \n Boardx() \n then \n System.err.println(42);" );
        rule2.checkin( "" );

        repo.save();


        ContentPackageAssembler asm = new ContentPackageAssembler(pkg);
        assertTrue(asm.hasErrors());

        rule2.archiveItem( true );
        rule2.checkin( "" );

        assertTrue(rule2.isArchived());
        asm = new ContentPackageAssembler(pkg);
        assertFalse(asm.hasErrors());


    }

    /**
     * This this case we will test errors that occur in rule assets,
     * not in functions or package header.
     */
    public void testErrorsInRuleAsset() throws Exception {

        RulesRepository repo = getRepo();

        //first, setup the package correctly:
        PackageItem pkg = repo.createPackage( "testErrorsInRuleAsset", "" );
        AssetItem model = pkg.addAsset( "model", "qed" );
        model.updateFormat( AssetFormats.MODEL );
        model.updateBinaryContentAttachment( this.getClass().getResourceAsStream( "/billasurf.jar" ) );
        model.checkin( "" );
        pkg.updateHeader( "import com.billasurf.Board\n global com.billasurf.Person customer" );
        repo.save();

        AssetItem goodRule = pkg.addAsset( "goodRule", "" );
        goodRule.updateFormat( AssetFormats.DRL );
        goodRule.updateContent( "rule 'yeah' \n when \n Board() \n then \n System.out.println(42); end" );
        goodRule.checkin( "" );

        AssetItem badRule = pkg.addAsset( "badRule", "xxx" );
        badRule.updateFormat( AssetFormats.DRL );
        badRule.updateContent( "if something then another" );
        badRule.checkin( "" );

        ContentPackageAssembler asm = new ContentPackageAssembler(pkg);
        assertTrue(asm.hasErrors());
        assertFalse(asm.isPackageConfigurationInError());

        for ( ContentAssemblyError err : asm.getErrors() ) {
            assertTrue(err.itemInError.getName().equals( badRule.getName() ));
            assertNotEmpty(err.errorReport);
        }

    }



    /**
     * This time, we mix up stuff a bit
     *
     */
    public void testRuleAndDSLAndFunction() throws Exception {
        RulesRepository repo = getRepo();

        //first, setup the package correctly:
        PackageItem pkg = repo.createPackage( "testRuleAndDSLAndFunction", "" );
        AssetItem model = pkg.addAsset( "model", "qed" );
        model.updateFormat( AssetFormats.MODEL );
        model.updateBinaryContentAttachment( this.getClass().getResourceAsStream( "/billasurf.jar" ) );
        model.checkin( "" );
        pkg.updateHeader( "import com.billasurf.Board\n global com.billasurf.Person customer" );
        repo.save();

        AssetItem func = pkg.addAsset( "func", "" );
        func.updateFormat( AssetFormats.FUNCTION );
        func.updateContent( "function void foo() { System.out.println(42); }" );
        func.checkin( "" );

        AssetItem dsl = pkg.addAsset( "myDSL", "" );
        dsl.updateFormat( AssetFormats.DSL );
        dsl.updateContent( "[then]call a func=foo();" );
        dsl.checkin( "" );

        AssetItem dsl2 = pkg.addAsset( "myDSL2", "" );
        dsl2.updateFormat( AssetFormats.DSL );
        dsl2.updateContent( "[when]There is a board=Board()" );
        dsl2.checkin( "" );


        AssetItem rule = pkg.addAsset( "myRule", "" );
        rule.updateFormat( AssetFormats.DSL_TEMPLATE_RULE );
        rule.updateContent( "when \n There is a board \n then \n call a func" );
        rule.checkin( "" );

        AssetItem rule2 = pkg.addAsset( "myRule2", "" );
        rule2.updateFormat( AssetFormats.DSL_TEMPLATE_RULE );
        rule2.updateContent( "package testRuleAndDSLAndFunction \n rule 'myRule2222' \n when \n There is a board \n then \n call a func \nend" );
        rule2.checkin( "" );

        AssetItem rule3 = pkg.addAsset( "myRule3", "" );
        rule3.updateFormat( AssetFormats.DRL );
        rule3.updateContent( "package testRuleAndDSLAndFunction\n rule 'rule3' \n when \n Board() \n then \n System.err.println(42); end");
        rule3.checkin( "" );

        repo.save();

        ContentPackageAssembler asm = new ContentPackageAssembler(pkg);
        assertFalse(asm.hasErrors());
        Package bin = asm.getBinaryPackage();
        assertNotNull(bin);
        assertEquals(3, bin.getRules().length);
        assertEquals(1, bin.getFunctions().size());

    }

    public void testShowSource() throws Exception {
        RulesRepository repo = getRepo();

        //first, setup the package correctly:
        PackageItem pkg = repo.createPackage( "testShowSource", "" );

        pkg.updateHeader( "import com.billasurf.Board\n global com.billasurf.Person customer" );
        repo.save();

        AssetItem func = pkg.addAsset( "func", "" );
        func.updateFormat( AssetFormats.FUNCTION );
        func.updateContent( "function void foo() { System.out.println(42); }" );
        func.checkin( "" );

        AssetItem dsl = pkg.addAsset( "myDSL", "" );
        dsl.updateFormat( AssetFormats.DSL );
        dsl.updateContent( "[then]call a func=foo();\n[when]foo=FooBarBaz()" );
        dsl.checkin( "" );

        AssetItem rule = pkg.addAsset( "rule1", "" );
        rule.updateFormat( AssetFormats.DRL );
        rule.updateContent( "rule 'foo' when Goo() then end" );
        rule.checkin( "" );

        AssetItem rule2 = pkg.addAsset( "rule2", "" );
        rule2.updateFormat( AssetFormats.DSL_TEMPLATE_RULE );
        rule2.updateContent( "when \n foo \n then \n call a func" );
        rule2.checkin( "" );

        ContentPackageAssembler asm = new ContentPackageAssembler(pkg, false, null);
        String drl = asm.getDRL();

        assertNotNull(drl);

        assertContains("import com.billasurf.Board\n global com.billasurf.Person customer", drl);
        assertContains("package testShowSource", drl);
        assertContains("function void foo() { System.out.println(42); }", drl);
        assertContains("foo();", drl);
        assertContains("FooBarBaz()", drl);
        assertContains("rule 'foo' when Goo() then end", drl);


    }

    public void testXLSDecisionTable() throws Exception {

        RulesRepository repo = getRepo();

        //first, setup the package correctly:
        PackageItem pkg = repo.createPackage( "testXLSDecisionTable", "" );

        pkg.updateHeader( "import org.acme.insurance.Policy\n import org.acme.insurance.Driver" );
        repo.save();

        InputStream xls = this.getClass().getResourceAsStream( "/SampleDecisionTable.xls" );
        assertNotNull(xls);

        AssetItem asset = pkg.addAsset( "MyDT", "" );
        asset.updateFormat( AssetFormats.DECISION_SPREADSHEET_XLS );
        asset.updateBinaryContentAttachment( xls );
        asset.checkin( "" );

        ContentPackageAssembler asm = new ContentPackageAssembler(pkg);
        if (asm.hasErrors()) {
            System.err.println(asm.getErrors().get( 0 ).errorReport);
            System.err.println(asm.getDRL());
        }
        assertFalse(asm.hasErrors());

        String drl = asm.getDRL();

        assertContains( "policy: Policy", drl );


        Package bin = asm.getBinaryPackage();

        RuleBase rb = RuleBaseFactory.newRuleBase();
        rb.addPackage( bin );

        WorkingMemory wm = rb.newStatefulSession();

        //now create some test data
        Driver driver = new Driver();
        Policy policy = new Policy();

        wm.insert(driver);
        wm.insert(policy);

        wm.fireAllRules();

        assertEquals(120, policy.getBasePrice());

        asset.updateBinaryContentAttachment( this.getClass().getResourceAsStream( "/SampleDecisionTable_WithError.xls" ) );
        asset.checkin( "" );
        asm = new ContentPackageAssembler(pkg);
        assertTrue(asm.hasErrors());
        assertEquals(asset.getName(), asm.getErrors().get( 0 ).itemInError.getName());
        asm = new ContentPackageAssembler(pkg, false);
        assertFalse(asm.hasErrors());
        drl = asm.getDRL();
        assertNotNull(drl);
        assertContains( "Driverx", drl);

    }

    public void testBRXMLWithDSLMixedIn() throws Exception {
        RulesRepository repo = getRepo();

        //create our package
        PackageItem pkg = repo.createPackage( "testBRLWithDSLMixedIn", "" );
        pkg.updateHeader( "import org.drools.Person" );
        AssetItem rule1 = pkg.addAsset( "rule2", "" );
        rule1.updateFormat( AssetFormats.BUSINESS_RULE );

        AssetItem dsl = pkg.addAsset( "MyDSL", "" );
        dsl.updateFormat( AssetFormats.DSL );
        dsl.updateContent( "[when]This is a sentence=Person()\n[then]say {hello}=System.err.println({hello});" );
        dsl.checkin( "" );

        RuleModel model = new RuleModel();
        model.name = "rule2";
        FactPattern pattern = new FactPattern("Person");
        pattern.boundName = "p";
        ActionSetField action = new ActionSetField("p");
        ActionFieldValue value = new ActionFieldValue("age", "42", SuggestionCompletionEngine.TYPE_NUMERIC );
        action.addFieldValue( value );

        model.addLhsItem( pattern );
        model.addRhsItem( action );

        DSLSentence dslCondition = new DSLSentence();
        dslCondition.sentence = "This is a sentence";

        model.addLhsItem( dslCondition );

        DSLSentence dslAction = new DSLSentence();
        dslAction.sentence = "say {42}";

        model.addRhsItem( dslAction );

        rule1.updateContent( BRXMLPersistence.getInstance().marshal( model ) );
        rule1.checkin( "" );
        repo.save();


        //now add a rule with no DSL
        model = new RuleModel();
        model.name = "ruleNODSL";
        pattern = new FactPattern("Person");
        pattern.boundName = "p";
        action = new ActionSetField("p");
        value = new ActionFieldValue("age", "42", SuggestionCompletionEngine.TYPE_NUMERIC );
        action.addFieldValue( value );

        model.addLhsItem( pattern );
        model.addRhsItem( action );


        AssetItem ruleNODSL = pkg.addAsset( "ruleNoDSL", "" );
        ruleNODSL.updateFormat( AssetFormats.BUSINESS_RULE );

        ruleNODSL.updateContent( BRXMLPersistence.getInstance().marshal( model ) );
        ruleNODSL.checkin( "" );


        pkg = repo.loadPackage( "testBRLWithDSLMixedIn" );
        ContentPackageAssembler asm = new ContentPackageAssembler(pkg);
        assertFalse(asm.hasErrors());
        Package bpkg = asm.getBinaryPackage();
        assertEquals(2, bpkg.getRules().length);


    }

    public void testCustomSelector() throws Exception {
        RulesRepository repo = getRepo();

        //create our package
        PackageItem pkg = repo.createPackage( "testCustomSelector", "" );
        pkg.updateHeader( "import org.drools.Person" );
        AssetItem rule1 = pkg.addAsset( "rule1", "" );
        rule1.updateFormat( AssetFormats.DRL );

        rule1.updateContent( "when \n Person() \n then \n System.out.println(\"yeah\");\n" );
        rule1.checkin( "" );

        AssetItem rule2 = pkg.addAsset( "rule2", "" );
        rule2.updateFormat( AssetFormats.DRL );

        rule2.updateContent( "when \n Person() \n then \n System.out.println(\"yeah\");\n" );
        rule2.checkin( "" );



        SelectorManager sm = SelectorManager.getInstance();
        sm.selectors.put( "testSelect", new AssetSelector() {
            public boolean isAssetAllowed(AssetItem asset) {
                return asset.getName().equals( "rule2" );
            }
        });

        ContentPackageAssembler asm = new ContentPackageAssembler(pkg, "testSelect");

        Package pk = asm.getBinaryPackage();
        assertEquals(1, pk.getRules().length);
        assertEquals("rule2", pk.getRules()[0 ].getName() );


        asm = new ContentPackageAssembler(pkg, null);
        pk = asm.getBinaryPackage();
        assertEquals(2, pk.getRules().length);

        asm = new ContentPackageAssembler(pkg, "nothing valid");
        assertTrue(asm.hasErrors());
        assertEquals(1, asm.getErrors().size());
        assertEquals(pkg, asm.getErrors().get( 0 ).itemInError);

        asm = new ContentPackageAssembler(pkg, "");
        pk = asm.getBinaryPackage();
        assertEquals(2, pk.getRules().length);


    }


    private void assertContains(String sub, String text) {
        if (text.indexOf( sub ) == -1) {
            fail("the text: '" + sub +"' was not found.");
        }


    }


    private void assertNotEmpty(String s) {
        if (s == null) fail("should not be null");
        if (s.trim().equals( "" )) fail("should not be empty string");
    }

    private RulesRepository getRepo() throws Exception {
        return new RulesRepository( TestEnvironmentSessionHelper.getSession() );
    }


}