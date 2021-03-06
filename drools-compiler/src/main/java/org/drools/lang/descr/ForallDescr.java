/*
 * Copyright 2006 JBoss Inc
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

package org.drools.lang.descr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author etirelli
 *
 */
public class ForallDescr extends BaseDescr
    implements
    ConditionalElementDescr {

    private static final long serialVersionUID = 400L;

    private static final String BASE_IDENTIFIER = "$__forallBaseIdentifier";

    private List              patterns;

    public ForallDescr() {
        this.patterns = new ArrayList( 2 );
    }

    /* (non-Javadoc)
     * @see org.drools.lang.descr.ConditionalElementDescr#addDescr(org.drools.lang.descr.BaseDescr)
     */
    public void addDescr(final BaseDescr baseDescr) {
        // cast to make sure we are adding a pattern descriptor
        this.patterns.add( baseDescr );
    }
    
    public void insertBeforeLast(final Class clazz ,final BaseDescr baseDescr ) { 
        throw new UnsupportedOperationException( "Can't add descriptors to " + this.getClass().getName() );
    }    

    /* (non-Javadoc)
     * @see org.drools.lang.descr.ConditionalElementDescr#getDescrs()
     */
    public List getDescrs() {
        return this.patterns;
    }

    /**
     * Returns the base pattern from the forall CE
     * @return
     */
    public PatternDescr getBasePattern() {
        if( this.patterns.size() > 1 ) {
            return (PatternDescr) this.patterns.get( 0 );
        } else if( this.patterns.size() == 1 ) {
            // in case there is only one pattern, we do a rewrite, so:
            // forall( Cheese( type == "stilton" ) )
            // becomes
            // forall( BASE_IDENTIFIER : Cheese() Cheese( this == BASE_IDENTIFIER, type == "stilton" ) )
            PatternDescr original = (PatternDescr) this.patterns.get( 0 );
            PatternDescr base = (PatternDescr) original.clone();
            base.getDescrs().clear();
            base.setIdentifier( BASE_IDENTIFIER );
            return base;
        }
        return null;
    }

    /**
     * Returns the remaining patterns from the forall CE
     * @return
     */
    public List getRemainingPatterns() {
        if( this.patterns.size() > 1 ) {
            return this.patterns.subList( 1,
                                          this.patterns.size() );
        } else if( this.patterns.size() == 1 ) {
            // in case there is only one pattern, we do a rewrite, so:
            // forall( Cheese( type == "stilton" ) )
            // becomes
            // forall( BASE_IDENTIFIER : Cheese() Cheese( this == BASE_IDENTIFIER, type == "stilton" ) )
            PatternDescr original = (PatternDescr) this.patterns.get( 0 );
            PatternDescr remaining = (PatternDescr) original.clone();
            VariableRestrictionDescr restr = new VariableRestrictionDescr( "==", BASE_IDENTIFIER );
            FieldConstraintDescr constr = new FieldConstraintDescr("this");
            constr.addRestriction( restr );
            remaining.addConstraint( constr );
            return Collections.singletonList( remaining );
        }
        return Collections.EMPTY_LIST;
    }

    public void addOrMerge(BaseDescr baseDescr) {
        this.patterns.add( baseDescr );        
    }
}
