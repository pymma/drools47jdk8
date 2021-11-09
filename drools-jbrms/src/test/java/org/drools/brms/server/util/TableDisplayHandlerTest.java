package org.drools.brms.server.util;
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



import java.lang.reflect.Method;

import junit.framework.TestCase;

public class TableDisplayHandlerTest extends TestCase {

    public void testRowLoaders() throws Exception {
        TableDisplayHandler handler = new TableDisplayHandler(TableDisplayHandler.DEFAULT_TABLE_TEMPLATE);
        RowLoader loader = new RowLoader(TableDisplayHandler.DEFAULT_TABLE_TEMPLATE);
        
        assertEquals(4, loader.getHeaders().length);
        String[] headers = loader.getHeaders();
        assertEquals("*", headers[0]);
        assertEquals("Name", headers[1]);
        assertEquals("Last modified", headers[2]);
        assertEquals("Status", headers[3]);
        
        assertEquals(4, loader.extractors.size());
        assertTrue(loader.extractors.get( 0 ) instanceof Method);
        assertEquals("getLastModified", ((Method)loader.extractors.get( 2 )).getName());
    }
}