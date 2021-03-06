/**
 * 
 */
package org.drools.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ObjectHashMap extends AbstractHashTable
    implements
    Externalizable {

    private static final long serialVersionUID = 400L;

    public ObjectHashMap() {
        this( 16,
              0.75f );
    }

    public ObjectHashMap(final int capacity,
                         final float loadFactor) {
        super( capacity,
               loadFactor );
    }

    public ObjectHashMap(final Entry[] table) {
        super( 0.75f,
               table );
    }

    public ObjectHashMap(final float loadFactor,
                         final Entry[] table) {
        super( loadFactor,
               table );
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        super.readExternal( in );
        resize( table.length,
                true );
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal( out );
    }

    protected void updateHashCode(Entry entry) {
        ((ObjectEntry) entry).setHashCode( this.comparator.hashCodeOf( ((ObjectEntry) entry).getKey() ) );
    }

    public Object put(final Object key,
                      final Object value) {
        return put( key,
                    value,
                    true );
    }

    public void clear() {
        this.table = new Entry[Math.min( this.table.length,
                                         16 )];
        this.threshold = (int) (this.table.length * this.loadFactor);
    }

    public Object put(final Object key,
                      final Object value,
                      final boolean checkExists) {
        final int hashCode = this.comparator.hashCodeOf( key );
        final int index = indexOf( hashCode,
                                   this.table.length );

        // scan the linked entries to see if it exists
        if ( checkExists ) {
            ObjectEntry current = (ObjectEntry) this.table[index];
            while ( current != null ) {
                if ( hashCode == current.hashCode && this.comparator.equal( key,
                                                                            current.key ) ) {
                    final Object oldValue = current.value;
                    current.value = value;
                    return oldValue;
                }
                current = (ObjectEntry) current.getNext();
            }
        }

        // We aren't checking the key exists, or it didn't find the key
        final ObjectEntry entry = new ObjectEntry( key,
                                                   value,
                                                   hashCode );
        entry.next = this.table[index];
        this.table[index] = entry;

        if ( this.size++ >= this.threshold ) {
            resize( 2 * this.table.length, false );
        }
        return null;
    }

    public Object get(final Object key) {
        final int hashCode = this.comparator.hashCodeOf( key );
        final int index = indexOf( hashCode,
                                   this.table.length );

        ObjectEntry current = (ObjectEntry) this.table[index];
        while ( current != null ) {
            if ( hashCode == current.hashCode && this.comparator.equal( key,
                                                                        current.key ) ) {
                return current.value;
            }
            current = (ObjectEntry) current.getNext();
        }
        return null;
    }

    public Object remove(final Object key) {
        final int hashCode = this.comparator.hashCodeOf( key );
        final int index = indexOf( hashCode,
                                   this.table.length );

        ObjectEntry previous = (ObjectEntry) this.table[index];
        ObjectEntry current = previous;
        while ( current != null ) {
            final ObjectEntry next = (ObjectEntry) current.getNext();
            if ( hashCode == current.hashCode && this.comparator.equal( key,
                                                                        current.key ) ) {
                if ( previous == current ) {
                    this.table[index] = next;
                } else {
                    previous.setNext( next );
                }
                current.setNext( null );
                this.size--;
                return current.value;
            }
            previous = current;
            current = next;
        }
        return null;
    }

    public Entry getBucket(final Object object) {
        final int hashCode = this.comparator.hashCodeOf( object );
        final int index = indexOf( hashCode,
                                   this.table.length );

        return this.table[index];
    }

    public static class ObjectEntry
        implements
        Entry {

        private static final long serialVersionUID = 400L;

        private Object            key;

        private Object            value;

        private int               hashCode;

        private Entry             next;

        public ObjectEntry(final Object key,
                           final Object value,
                           final int hashCode) {
            this.key = key;
            this.value = value;
            this.hashCode = hashCode;
        }

        public void setHashCode(int hashCode) {
            this.hashCode = hashCode;
        }

        public Object getValue() {
            return this.value;
        }

        public Object getKey() {
            return this.key;
        }

        public Entry getNext() {
            return this.next;
        }

        public void setNext(final Entry next) {
            this.next = next;
        }

        public int hashCode() {
            return this.hashCode;
        }

        public boolean equals(final Object object) {
            if ( object == this ) {
                return true;
            }

            // assumes we never have null or wrong class

            final ObjectEntry other = (ObjectEntry) object;
            return this.key.equals( other.key ) && this.value.equals( other.value );
        }
    }
}