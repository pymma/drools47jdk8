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

import junit.framework.TestCase;

import org.drools.common.DefaultFactHandle;

public class FactHandleTest extends TestCase {
    /*
     * Class under test for void FactHandleImpl(long)
     */
    public void testFactHandleImpllong() {
        final DefaultFactHandle f0 = new DefaultFactHandle( 134,
                                                            "cheese" );
        assertEquals( 134,
                      f0.getId() );
        assertEquals( 134,
                      f0.getRecency() );
    }

    /*
     * Class under test for void FactHandleImpl(long, long)
     */
    public void testFactHandleImpllonglong() {
        final DefaultFactHandle f0 = new DefaultFactHandle( 134,
                                                            "cheese",
                                                            678 );
        assertEquals( 134,
                      f0.getId() );
        assertEquals( 678,
                      f0.getRecency() );
    }

    /*
     * Class under test for boolean equals(Object)
     */
    public void testEqualsObject() {
        final DefaultFactHandle f0 = new DefaultFactHandle( 134,
                                                            "cheese" );
        final DefaultFactHandle f1 = new DefaultFactHandle( 96,
                                                            "cheese" );
        final DefaultFactHandle f3 = new DefaultFactHandle( 96,
                                                            "cheese" );

        assertFalse( "f0 should not equal f1",
                     f0.equals( f1 ) );
        assertEquals( f1,
                      f3 );
        assertNotSame( f1,
                       f3 );
    }

    public void testHashCode() {
        final DefaultFactHandle f0 = new DefaultFactHandle( 234,
                                                            "cheese" );
        assertEquals( "cheese".hashCode(),
                      f0.getObjectHashCode() );

        assertEquals( 234,
                      f0.hashCode() );
    }

    public void testInvalidate() {
        final DefaultFactHandle f0 = new DefaultFactHandle( 134,
                                                            "cheese" );
        assertEquals( 134,
                      f0.getId() );

        f0.invalidate();
        assertEquals( -1,
                      f0.getId() );
    }

}