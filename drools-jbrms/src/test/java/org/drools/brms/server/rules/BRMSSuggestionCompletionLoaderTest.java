package org.drools.brms.server.rules;
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



import java.util.Collections;

import junit.framework.TestCase;

import org.drools.brms.client.common.AssetFormats;
import org.drools.brms.client.modeldriven.SuggestionCompletionEngine;
import org.drools.brms.server.util.BRMSSuggestionCompletionLoader;
import org.drools.brms.server.util.TestEnvironmentSessionHelper;
import org.drools.repository.AssetItem;
import org.drools.repository.PackageItem;
import org.drools.repository.RulesRepository;

public class BRMSSuggestionCompletionLoaderTest extends TestCase {

    public void testLoader() throws Exception {

        RulesRepository repo = new RulesRepository(TestEnvironmentSessionHelper.getSession());
        PackageItem item = repo.createPackage( "testLoader", "to test the loader" );
        item.updateHeader( "import java.util.Date" );
        repo.save();

        BRMSSuggestionCompletionLoader  loader = new BRMSSuggestionCompletionLoader();
        String header = item.getHeader();


        SuggestionCompletionEngine engine = loader.getSuggestionEngine( item );
        assertNotNull(engine);

    }

    public void testStripUnNeededFields() {
        SuggestionCompletionLoader loader = new SuggestionCompletionLoader();
        String[] result = loader.removeIrrelevantFields( new String[] {"foo", "toString", "class", "hashCode"} );
        assertEquals(1, result.length);
        assertEquals("foo", result[0]);
    }

    public void testGetShortNameOfClass() {
        SuggestionCompletionLoader loader = new SuggestionCompletionLoader();

        assertEquals("Object", loader.getShortNameOfClass( Object.class.getName() ));

        assertEquals("Foo", loader.getShortNameOfClass( "Foo" ));
    }

    public void testFactTemplates() throws Exception {

        RulesRepository repo = new RulesRepository(TestEnvironmentSessionHelper.getSession());
        PackageItem item = repo.createPackage( "testLoader2", "to test the loader for fact templates" );
        item.updateHeader( "import java.util.Date\ntemplate Person\njava.lang.String name\nDate birthDate\nend" );
        repo.save();

        BRMSSuggestionCompletionLoader loader = new BRMSSuggestionCompletionLoader();

        SuggestionCompletionEngine engine = loader.getSuggestionEngine( item );
        assertNotNull(engine);
        String[] factTypes = engine.getFactTypes();

        assertEquals( 2, factTypes.length );
        assertEquals("Date", factTypes[0]);
        assertEquals("Person", factTypes[1]);

        String[] fieldsForType = engine.getFieldCompletions( "Person" );
        assertEquals( 2, fieldsForType.length );
        assertEquals("birthDate", fieldsForType[0]);
        assertEquals("name", fieldsForType[1]);

        String fieldType = engine.getFieldType( "Person", "name" );
        assertEquals( SuggestionCompletionEngine.TYPE_STRING, fieldType );
        fieldType = engine.getFieldType( "Person", "birthDate" );
        assertEquals( SuggestionCompletionEngine.TYPE_COMPARABLE, fieldType );
    }

    public void testLoadDSLs() throws Exception {
        String dsl = "[when]The agents rating is {rating}=doNothing()\n[then]Send a notification to manufacturing '{message}'=foo()";
        RulesRepository repo = new RulesRepository(TestEnvironmentSessionHelper.getSession());
        PackageItem item = repo.createPackage( "testLoadDSLs", "to test the loader for DSLs" );
        AssetItem asset = item.addAsset( "mydsl", "" );
        asset.updateFormat( AssetFormats.DSL );
        asset.updateContent( dsl );
        asset.checkin( "ok" );

        item = repo.loadPackage( "testLoadDSLs" );
        BRMSSuggestionCompletionLoader loader = new BRMSSuggestionCompletionLoader();


        SuggestionCompletionEngine eng = loader.getSuggestionEngine( item );
        assertNotNull(eng.dataEnumLists);
        assertEquals(Collections.EMPTY_MAP, eng.dataEnumLists);
        assertFalse(loader.hasErrors());
        assertEquals(1, eng.actionDSLSentences.length);
        assertEquals(1, eng.conditionDSLSentences.length);

        assertEquals( "The agents rating is {rating}", eng.conditionDSLSentences[0].sentence );
        assertEquals("Send a notification to manufacturing '{message}'",eng.actionDSLSentences[0].sentence);

        dsl = "koo kooo ca choo";
        asset.updateContent( dsl );
        asset.checkin( "boo" );

        item = repo.loadPackage( "testLoadDSLs" );
        loader = new BRMSSuggestionCompletionLoader();
        loader.getSuggestionEngine( item );
        assertTrue(loader.hasErrors());


    }

    public void testLoadEnumerations() throws Exception {
        String enumeration = "'Person.sex' : ['M', 'F']";
        RulesRepository repo = new RulesRepository(TestEnvironmentSessionHelper.getSession());
        PackageItem item = repo.createPackage( "testLoadEnums", "to test the loader for enums" );
        AssetItem asset = item.addAsset( "myenum", "" );
        asset.updateFormat( AssetFormats.ENUMERATION );
        asset.updateContent( enumeration );
        asset.checkin( "ok" );

        item = repo.loadPackage( "testLoadEnums" );
        BRMSSuggestionCompletionLoader loader = new BRMSSuggestionCompletionLoader();
        SuggestionCompletionEngine sce = loader.getSuggestionEngine( item );

        assertFalse(loader.hasErrors());
        assertEquals(1, sce.dataEnumLists.size());


        asset.updateContent( "goober boy" );
        asset.checkin( "yeah" );
        item = repo.loadPackage( "testLoadEnums" );
        loader = new BRMSSuggestionCompletionLoader();
        sce = loader.getSuggestionEngine( item );
        assertTrue(loader.hasErrors());


    }

    public void testErrors() throws Exception {
        RulesRepository repo = new RulesRepository(TestEnvironmentSessionHelper.getSession());
        PackageItem item = repo.createPackage( "testErrorsInPackage", "to test error handling" );

        BRMSSuggestionCompletionLoader loader = new BRMSSuggestionCompletionLoader();


        assertNotNull(loader.getSuggestionEngine( item ));
        assertFalse(loader.hasErrors());

        item.updateHeader( "gooble de gook" );
        loader = new BRMSSuggestionCompletionLoader();
        loader.getSuggestionEngine( item );
        assertTrue(loader.hasErrors());


        item.updateHeader( "import foo.bar; \nglobal goo.Bar baz;" );
        loader = new BRMSSuggestionCompletionLoader();
        loader.getSuggestionEngine( item );
        assertTrue(loader.hasErrors());


    }

    /**
     * This shows we need to load up the model without anything attached yet.
     */
    public void testModelWithNoAttachment() throws Exception {
        RulesRepository repo = new RulesRepository(TestEnvironmentSessionHelper.getSession());
        PackageItem item = repo.createPackage( "testmodelWithNoAttachment", "to test model loading" );

        item.addAsset( "testModel", "", null, AssetFormats.MODEL );
        repo.save();

        BRMSSuggestionCompletionLoader loader = new BRMSSuggestionCompletionLoader();


        assertNotNull(loader.getSuggestionEngine( item ));
        assertFalse(loader.hasErrors());


    }

}