package org.drools.brms.server.contenthandler;
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



import junit.framework.TestCase;

import org.drools.brms.client.common.AssetFormats;
import org.drools.brms.client.rpc.BuilderResult;
import org.drools.brms.server.util.TestEnvironmentSessionHelper;
import org.drools.repository.AssetItem;
import org.drools.repository.PackageItem;
import org.drools.repository.RulesRepository;

public class ContentHandlerTest extends TestCase {

    public void testContentFormat() {
        assertTrue(ContentHandler.getHandler( AssetFormats.DRL ) instanceof DRLFileContentHandler);
        assertTrue(ContentHandler.getHandler( AssetFormats.DSL ) instanceof DSLDefinitionContentHandler);
        assertTrue(ContentHandler.getHandler( AssetFormats.DSL_TEMPLATE_RULE ) instanceof DSLRuleContentHandler);
        assertTrue(ContentHandler.getHandler( AssetFormats.BUSINESS_RULE ) instanceof BRLContentHandler);
        assertTrue(ContentHandler.getHandler( AssetFormats.DECISION_SPREADSHEET_XLS ) instanceof DecisionTableXLSHandler);
        assertTrue(ContentHandler.getHandler( AssetFormats.ENUMERATION ) instanceof EnumerationContentHandler);

        try {
            ContentHandler.getHandler( "XXX" );
            fail("should have thrown an exception");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    public void testRuleAssetType() {
        assertTrue(ContentHandler.getHandler( AssetFormats.DRL ).isRuleAsset());
        assertTrue(ContentHandler.getHandler( AssetFormats.DSL_TEMPLATE_RULE ).isRuleAsset());
        assertTrue(ContentHandler.getHandler( AssetFormats.BUSINESS_RULE ).isRuleAsset());
        assertTrue(ContentHandler.getHandler( AssetFormats.DECISION_SPREADSHEET_XLS ).isRuleAsset());

        assertFalse(ContentHandler.getHandler( AssetFormats.DSL ).isRuleAsset());
        assertFalse(ContentHandler.getHandler( AssetFormats.MODEL ).isRuleAsset());
        assertFalse(ContentHandler.getHandler( AssetFormats.ENUMERATION ).isRuleAsset());
    }

    public void testValidating() throws Exception {
        RulesRepository repo = new RulesRepository( TestEnvironmentSessionHelper.getSession() );
        PackageItem pkg = repo.loadDefaultPackage();
        AssetItem asset = pkg.addAsset( "testValidatingEnum", "" );
        asset.updateFormat( AssetFormats.ENUMERATION );
        asset.updateContent( "'Person.age' : [1, 2, 3]" );

        EnumerationContentHandler ch = new EnumerationContentHandler();
        BuilderResult[] result =  ch.validateAsset( asset );
        assertNotNull(result);
        assertEquals(0, result.length);

        asset.updateContent( "goober boy" );
        result = ch.validateAsset( asset );
        assertFalse(result.length == 0);
        assertEquals(asset.getName(), result[0].assetName);
        assertEquals(asset.getFormat(), result[0].assetFormat);
        assertNotNull(result[0].message);
        assertEquals(asset.getUUID(), result[0].uuid);



    }

}