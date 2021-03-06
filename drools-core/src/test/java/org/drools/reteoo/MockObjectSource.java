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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.drools.common.BaseNode;
import org.drools.common.InternalFactHandle;
import org.drools.common.InternalWorkingMemory;
import org.drools.spi.PropagationContext;

public class MockObjectSource extends ObjectSource {
    /**
     * 
     */
    private static final long serialVersionUID = 400L;

    private int               attached;

    private int               updated;

    private List              facts;

    public MockObjectSource(final int id) {
        super( id );
        this.facts = new ArrayList();
    }

    public void attach() {
        this.attached++;

    }

    public int getAttached() {
        return this.attached;
    }

    public int getUdated() {
        return this.updated;
    }

    public void addFact(final InternalFactHandle handle) {
        this.facts.add( handle );
    }

    public void updateSink(final ObjectSink sink,
                           final PropagationContext context,
                           final InternalWorkingMemory workingMemory) {
        this.updated++;
        for ( final Iterator it = this.facts.iterator(); it.hasNext(); ) {
            final InternalFactHandle handle = (InternalFactHandle) it.next();
            sink.assertObject( handle,
                               context,
                               workingMemory );
        }
    }

    public void remove(final RuleRemovalContext context,
                       final BaseNode node,
                       final InternalWorkingMemory[] workingMemories) {
        // TODO Auto-generated method stub

    }

    public void attach(final InternalWorkingMemory[] workingMemories) {
        // TODO Auto-generated method stub

    }

}