package org.drools;

import java.io.Serializable;

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

public class CheeseEqual
    implements
    Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 400L;
    protected String          type;
    protected int             price;

    public CheeseEqual() {

    }

    public CheeseEqual(final String type,
                       final int price) {
        super();
        this.type = type;
        this.price = price;
    }

    public int getPrice() {
        return this.price;
    }

    public String getType() {
        return this.type;
    }

    public void setPrice(final int price) {
        this.price = price;
    }

    public String toString() {
        return "CheeseEqual( type='" + this.type + "', price=" + this.price + " )";
    }

    public boolean equals(final Object object) {
        if ( this == object ) {
            return true;
        }

        if ( (object == null) || (object.getClass() != this.getClass()) ) {
            return false;
        }

        final CheeseEqual other = (CheeseEqual) object;

        if ( !this.type.equals( other.type ) ) {
            return false;
        }

        if ( this.price != other.price ) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        //like org.apache.commons.lang.builder.HashCodeBuilder
        int hashCode = 17;
        hashCode = hashCode * 37 + this.price;
        hashCode = hashCode * 37 + (this.type == null ? 0 : this.type.hashCode());
        return hashCode;
    }
}