package org.drools.rule;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.drools.spi.Constraint;
import org.drools.spi.Extractor;
import org.drools.spi.ObjectType;
import org.drools.spi.PatternExtractor;
import org.drools.spi.Constraint.ConstraintType;

public class Pattern
    implements
    RuleConditionElement {
    /**
     * 
     */
    private static final long serialVersionUID = 400L;
    private final ObjectType  objectType;
    private List              constraints      = Collections.EMPTY_LIST;
    final Declaration         declaration;
    private Map               declarations;
    private final int         index;
    private PatternSource     source;

    // this is the offset of the related fact inside a tuple. i.e:
    // the position of the related fact inside the tuple; 
    private int               offset;

    public Pattern(final int index,
                   final ObjectType objectType) {
        this( index,
              index,
              objectType,
              null );
    }

    public Pattern(final int index,
                   final ObjectType objectType,
                   final String identifier) {
        this( index,
              index,
              objectType,
              identifier );
    }

    public Pattern(final int index,
                   final int offset,
                   final ObjectType objectType,
                   final String identifier) {
        this( index,
              offset,
              objectType,
              identifier,
              false );
    }

    public Pattern(final int index,
                   final int offset,
                   final ObjectType objectType,
                   final String identifier,
                   final boolean isInternalFact) {
        this.index = index;
        this.offset = offset;
        this.objectType = objectType;
        if ( identifier != null && (!identifier.equals( "" )) ) {
            this.declaration = new Declaration( identifier,
                                                new PatternExtractor( objectType ),
                                                this,
                                                isInternalFact );
            this.declarations = new HashMap( 2 ); // default to avoid immediate resize
            this.declarations.put( this.declaration.getIdentifier(),
                                   this.declaration );
        } else {
            this.declaration = null;
        }
    }

    public Object clone() {
        final String identifier = (this.declaration != null) ? this.declaration.getIdentifier() : null;
        final Pattern clone = new Pattern( this.index,
                                           this.offset,
                                           this.objectType,
                                           identifier,
                                           this.declaration != null ? this.declaration.isInternalFact() : false );
        if( this.getSource() != null ) {
            clone.setSource( (PatternSource) this.getSource().clone() );
        }

        for ( final Iterator it = this.constraints.iterator(); it.hasNext(); ) {
            final Object constr = it.next();
            if ( constr instanceof Declaration ) {
                final Declaration decl = (Declaration) constr;
                clone.addDeclaration( decl.getIdentifier(),
                                      decl.getExtractor() );
            } else {
                Constraint constraint = (Constraint) ((Constraint) constr).clone();
                
                // we must update pattern references in cloned declarations
                Declaration[] oldDecl = ((Constraint) constr).getRequiredDeclarations();
                Declaration[] newDecl = constraint.getRequiredDeclarations();
                for( int i = 0; i < newDecl.length; i++ ) {
                    if( newDecl[i].getPattern() == this ) {
                        newDecl[i].setPattern( clone );
                        // we still need to call replace because there might be nested declarations to replace
                        constraint.replaceDeclaration( oldDecl[i], newDecl[i] );
                    }
                }
                
                clone.addConstraint( constraint );
            }
        }
        return clone;
    }

    public ObjectType getObjectType() {
        return this.objectType;
    }

    public PatternSource getSource() {
        return source;
    }

    public void setSource(PatternSource source) {
        this.source = source;
    }

    public List getConstraints() {
        return Collections.unmodifiableList( this.constraints );
    }

    public void addConstraint(final Constraint constraint) {
        if ( this.constraints == Collections.EMPTY_LIST ) {
            this.constraints = new ArrayList( 1 );
        }
        if( constraint.getType().equals( Constraint.ConstraintType.UNKNOWN ) ) {
            this.setConstraintType( (MutableTypeConstraint) constraint );
        }
        this.constraints.add( constraint );
    }

    public Declaration addDeclaration(final String identifier,
                                      final Extractor extractor) {
        if ( this.constraints == Collections.EMPTY_LIST ) {
            this.constraints = new ArrayList( 1 );
        }
        final Declaration declaration = new Declaration( identifier,
                                                         extractor,
                                                         this );
        this.constraints.add( declaration );
        if ( this.declarations == null ) {
            this.declarations = new HashMap( 2 ); // default to avoid immediate resize
        }
        this.declarations.put( declaration.getIdentifier(),
                               declaration );
        return declaration;

    }

    public boolean isBound() {
        return (this.declaration != null);
    }

    public Declaration getDeclaration() {
        return this.declaration;
    }

    public int getIndex() {
        return this.index;
    }

    /**
     * The offset of the fact related to this pattern 
     * inside the tuple
     * 
     * @return the offset
     */
    public int getOffset() {
        return this.offset;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

    public Map getInnerDeclarations() {
        return (this.declarations != null) ? this.declarations : Collections.EMPTY_MAP;
    }

    public Map getOuterDeclarations() {
        return (this.declarations != null) ? this.declarations : Collections.EMPTY_MAP;
    }

    public Declaration resolveDeclaration(final String identifier) {
        return (this.declarations != null) ? (Declaration) this.declarations.get( identifier ) : null;
    }

    public String toString() {
        return "Pattern type='" + ((this.objectType == null) ? "null" : this.objectType.toString()) + "', index='" + this.index + "', offset='" + this.getOffset() + "', identifer='" + ((this.declaration == null) ? "" : this.declaration.toString())
               + "'";
    }

    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + this.constraints.hashCode();
        result = PRIME * result + ((this.declaration == null) ? 0 : this.declaration.hashCode());
        result = PRIME * result + this.index;
        result = PRIME * result + ((this.objectType == null) ? 0 : this.objectType.hashCode());
        result = PRIME * result + this.offset;
        result = PRIME * result + ((this.source == null) ? 0 : this.source.hashCode());
        return result;
    }

    public boolean equals(final Object object) {
        if ( this == object ) {
            return true;
        }

        if ( object == null || getClass() != object.getClass() ) {
            return false;
        }

        final Pattern other = (Pattern) object;

        if ( !this.constraints.equals( other.constraints ) ) {
            return false;
        }

        if ( this.declaration == null ) {
            if ( other.declaration != null ) {
                return false;
            }
        } else if ( !this.declaration.equals( other.declaration ) ) {
            return false;
        }

        if ( this.index != other.index ) {
            return false;
        }

        if ( !this.objectType.equals( other.objectType ) ) {
            return false;
        }
        if ( this.offset != other.offset ) {
            return false;
        }
        return (this.source == null) ? other.source == null : this.source.equals( other.source );
    }

    public List getNestedElements() {
        return this.source != null ? Collections.singletonList( this.source ) : Collections.EMPTY_LIST;
    }
    
    /**
     * @param constraint
     */
    private void setConstraintType(final MutableTypeConstraint constraint) {
        final Declaration[] declarations = constraint.getRequiredDeclarations();

        boolean isAlphaConstraint = true;
        for ( int i = 0; isAlphaConstraint && i < declarations.length; i++ ) {
            if ( !declarations[i].isGlobal() && declarations[i].getPattern() != this ) {
                isAlphaConstraint = false;
            }
        }

        ConstraintType type = isAlphaConstraint ? ConstraintType.ALPHA : ConstraintType.BETA; 
        constraint.setType( type );
    }

}