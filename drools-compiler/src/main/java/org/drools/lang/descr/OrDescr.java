package org.drools.lang.descr;

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
import java.util.List;

public class OrDescr extends BaseDescr
    implements
    ConditionalElementDescr {
    /**
     * 
     */
    private static final long serialVersionUID = 400L;
    private List              descrs           = new ArrayList();

    public OrDescr() {
    }
    
    public void insertBeforeLast(final Class clazz ,final BaseDescr baseDescr ) {        
        if(clazz.isInstance( baseDescr )) {
            for ( int i = this.descrs.size()-1; i >= 0; i-- ) {
                if ( this.descrs.get( i ) instanceof FieldConstraintDescr ) {
                    insertDescr( i-1, baseDescr );
                    return;
                }                
            }
        }        
    }
    
    public void insertDescr(int index, final BaseDescr baseDescr) {
        this.descrs.add( index, baseDescr );
    }    

    public void addDescr(final BaseDescr baseDescr) {
        this.descrs.add( baseDescr );
    }


    public List getDescrs() {
        return this.descrs;
    }

    public void addOrMerge(final BaseDescr baseDescr) {
        if( baseDescr instanceof OrDescr ) {
            this.descrs.addAll( ((OrDescr)baseDescr).getDescrs() );
        } else {
            this.descrs.add( baseDescr );
        }
    }
    
    
}