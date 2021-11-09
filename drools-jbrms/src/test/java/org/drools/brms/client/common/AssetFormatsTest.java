package org.drools.brms.client.common;
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

public class AssetFormatsTest extends TestCase {


    public void testGrouping() {
        String[] formats = AssetFormats.BUSINESS_RULE_FORMATS;
        for ( int i = 0; i < formats.length; i++ ) {
            String fmt = formats[i];
            if (! (fmt.equals( AssetFormats.BUSINESS_RULE )
                    ||
                    fmt.equals( AssetFormats.DECISION_SPREADSHEET_XLS )
                    ||
                    fmt.equals( AssetFormats.DSL_TEMPLATE_RULE )
            ) ) {
                fail("Incorrect grouping of business rules.");
            }
        }

        formats = AssetFormats.TECHNICAL_RULE_FORMATS;
        for ( int i = 0; i < formats.length; i++ ) {
            String fmt = formats[i];
            if (! (fmt.equals( AssetFormats.RULE_FLOW_RF )
                    ||
                    fmt.equals( AssetFormats.DRL)
                    ||
                    fmt.equals(AssetFormats.ENUMERATION))
            ) {
                fail("Incorrect grouping of technical rules.");
            }
        }


    }

    public void testPackageDependencies() {
        assertFalse(AssetFormats.isPackageDependency(AssetFormats.BUSINESS_RULE));
        assertFalse(AssetFormats.isPackageDependency(AssetFormats.DRL));
        assertTrue(AssetFormats.isPackageDependency(AssetFormats.DSL));
        assertTrue(AssetFormats.isPackageDependency(AssetFormats.MODEL));
        assertTrue(AssetFormats.isPackageDependency(AssetFormats.FUNCTION));
        assertTrue(AssetFormats.isPackageDependency( AssetFormats.ENUMERATION ));
    }

}