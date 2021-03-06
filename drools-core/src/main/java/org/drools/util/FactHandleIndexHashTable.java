/**
 * 
 */
package org.drools.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.drools.common.InternalFactHandle;
import org.drools.reteoo.FactHandleMemory;
import org.drools.reteoo.ReteTuple;

public class FactHandleIndexHashTable extends AbstractHashTable
    implements
    FactHandleMemory,
    Externalizable {

    private static final long           serialVersionUID = 400L;

    public static final int             PRIME            = 31;

    private int                         startResult;

    private FieldIndexHashTableIterator tupleValueIterator;

    private int                         factSize;

    private Index                       index;

    // used only by serialization
    public FactHandleIndexHashTable() {
    }

    public FactHandleIndexHashTable(final FieldIndex[] index) {
        this( 16,
              0.75f,
              index );
    }

    public FactHandleIndexHashTable(final int capacity,
                                    final float loadFactor,
                                    final FieldIndex[] index) {
        super( capacity,
               loadFactor );

        this.startResult = FactHandleIndexHashTable.PRIME;
        for ( int i = 0, length = index.length; i < length; i++ ) {
            this.startResult = FactHandleIndexHashTable.PRIME * this.startResult + index[i].getExtractor().getIndex();
        }

        switch ( index.length ) {
            case 0 :
                throw new IllegalArgumentException( "FieldIndexHashTable cannot use an index[] of length  0" );
            case 1 :
                this.index = new SingleIndex( index,
                                              this.startResult );
                break;
            case 2 :
                this.index = new DoubleCompositeIndex( index,
                                                       this.startResult );
                break;
            case 3 :
                this.index = new TripleCompositeIndex( index,
                                                       this.startResult );
                break;
            default :
                throw new IllegalArgumentException( "FieldIndexHashTable cannot use an index[] of length  great than 3" );
        }
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        super.readExternal( in );
        startResult = in.readInt();
        factSize = in.readInt();
        tupleValueIterator = (FieldIndexHashTableIterator) in.readObject();
        index = (Index) in.readObject();

        resize( table.length,
                true );
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal( out );
        out.writeInt( startResult );
        out.writeInt( factSize );
        out.writeObject( tupleValueIterator );
        out.writeObject( index );
    }

    protected void updateHashCode(Entry entry) {
        // in theory, all handles inside an entry have the same hashcode
        ((FieldIndexEntry) entry).setHashCode( this.index.hashCodeOf( ((FieldIndexEntry) entry).getFirst().getFactHandle().getObject() ) );
    }

    public Iterator iterator() {
        throw new UnsupportedOperationException( "FieldIndexHashTable does not support  iterator()" );
    }

    public Iterator iterator(final ReteTuple tuple) {
        if ( this.tupleValueIterator == null ) {
            this.tupleValueIterator = new FieldIndexHashTableIterator();
        }
        final FieldIndexEntry entry = get( tuple );
        this.tupleValueIterator.reset( (entry != null) ? entry.first : null );
        return this.tupleValueIterator;
    }

    public boolean isIndexed() {
        return true;
    }

    public Index getIndex() {
        return this.index;
    }

    public Entry getBucket(final Object object) {
        final int hashCode = this.index.hashCodeOf( object );
        final int index = indexOf( hashCode,
                                   this.table.length );

        return this.table[index];
    }

    /**
     * Fast re-usable iterator
     *
     */
    public static class FieldIndexHashTableIterator
        implements
        Iterator {
        private Entry entry;

        public FieldIndexHashTableIterator() {

        }

        /* (non-Javadoc)
         * @see org.drools.util.Iterator#next()
         */
        public Object next() {
            final Entry current = this.entry;
            this.entry = (this.entry != null) ? this.entry.getNext() : null;
            return current;
        }

        /* (non-Javadoc)
         * @see org.drools.util.Iterator#reset()
         */
        public void reset(final Entry entry) {
            this.entry = entry;
        }
    }

    public Entry[] toArray() {
        Entry[] result = new Entry[this.factSize];
        int index = 0;
        for ( int i = 0; i < this.table.length; i++ ) {
            FieldIndexEntry fieldIndexEntry = (FieldIndexEntry) this.table[i];
            while ( fieldIndexEntry != null ) {
                Entry entry = fieldIndexEntry.getFirst();
                while ( entry != null ) {
                    result[index++] = entry;
                    entry = entry.getNext();
                }
                fieldIndexEntry = (FieldIndexEntry) fieldIndexEntry.getNext();
            }
        }
        return result;
    }

    public boolean add(final InternalFactHandle handle) {
        final FieldIndexEntry entry = getOrCreate( handle.getObject() );
        entry.add( handle );
        this.factSize++;
        return true;
    }

    public boolean add(final InternalFactHandle handle,
                       final boolean checkExists) {
        throw new UnsupportedOperationException( "FieldIndexHashTable does not support add(InternalFactHandle handle, boolean checkExists)" );
    }

    public boolean remove(final InternalFactHandle handle) {
        final Object object = handle.getObject();
        //this.index.setCachedValue( object );
        final int hashCode = this.index.hashCodeOf( object );

        final int index = indexOf( hashCode,
                                   this.table.length );

        // search the table for  the Entry, we need to track previous  and next, so if the 
        // Entry is empty after  its had the FactEntry removed, we must remove  it from the table
        FieldIndexEntry previous = (FieldIndexEntry) this.table[index];
        FieldIndexEntry current = previous;
        while ( current != null ) {
            final FieldIndexEntry next = (FieldIndexEntry) current.next;
            if ( current.matches( object,
                                  hashCode ) ) {
                if ( current.remove( handle ) != null ) {
                    this.factSize--;
                    // If the FactEntryIndex is empty, then remove it from the hash table
                    if ( current.first == null ) {
                        if ( previous == current ) {
                            this.table[index] = next;
                        } else {
                            previous.next = next;
                        }
                        current.next = null;
                        this.size--;
                    }
                    return true;
                }
            }
            previous = current;
            current = next;
        }
        return false;
    }

    public boolean contains(final InternalFactHandle handle) {
        final Object object = handle.getObject();
        //this.index.setCachedValue( object );

        final int hashCode = this.index.hashCodeOf( object );

        final int index = indexOf( hashCode,
                                   this.table.length );

        FieldIndexEntry current = (FieldIndexEntry) this.table[index];
        while ( current != null ) {
            if ( current.matches( object,
                                  hashCode ) ) {
                return true;
            }
            current = (FieldIndexEntry) current.next;
        }
        return false;
    }

    public FieldIndexEntry get(final ReteTuple tuple) {
        //this.index.setCachedValue( tuple );

        final int hashCode = this.index.hashCodeOf( tuple );

        final int index = indexOf( hashCode,
                                   this.table.length );
        FieldIndexEntry entry = (FieldIndexEntry) this.table[index];

        while ( entry != null ) {
            if ( entry.matches( tuple,
                                hashCode ) ) {
                return entry;
            }
            entry = (FieldIndexEntry) entry.getNext();
        }

        return entry;
    }

    /**
     * We use this method to aviod to table lookups for the same hashcode; which is what we would have to do if we did
     * a get and then a create if the value is null.
     * 
     * @param value
     * @return
     */
    private FieldIndexEntry getOrCreate(final Object object) {
        //this.index.setCachedValue( object );

        final int hashCode = this.index.hashCodeOf( object );

        final int index = indexOf( hashCode,
                                   this.table.length );
        FieldIndexEntry entry = (FieldIndexEntry) this.table[index];

        while ( entry != null ) {
            if ( entry.matches( object,
                                hashCode ) ) {
                return entry;
            }
            entry = (FieldIndexEntry) entry.next;
        }

        if ( entry == null ) {
            entry = new FieldIndexEntry( this.index,
                                         hashCode );
            entry.next = this.table[index];
            this.table[index] = entry;

            if ( this.size++ >= this.threshold ) {
                resize( 2 * this.table.length,
                        false );
            }
        }
        return entry;
    }

    public int size() {
        return this.factSize;
    }

    public static class FieldIndexEntry
        implements
        Entry {

        private static final long serialVersionUID = 400L;
        private Entry             next;
        private FactEntryImpl     first;
        private int               hashCode;
        private Index             index;

        public FieldIndexEntry(final Index index,
                               final int hashCode) {
            this.index = index;
            this.hashCode = hashCode;
        }

        public Entry getNext() {
            return this.next;
        }

        public void setNext(final Entry next) {
            this.next = next;
        }

        public FactEntryImpl getFirst() {
            return this.first;
        }

        public void add(final InternalFactHandle handle) {
            final FactEntryImpl entry = new FactEntryImpl( handle );
            entry.next = this.first;
            this.first = entry;
        }

        public FactEntryImpl get(final InternalFactHandle handle) {
            final long id = handle.getId();
            FactEntryImpl current = this.first;
            while ( current != null ) {
                if ( current.handle.getId() == id ) {
                    return current;
                }
                current = (FactEntryImpl) current.next;
            }
            return null;
        }

        public FactEntryImpl remove(final InternalFactHandle handle) {
            final long id = handle.getId();

            FactEntryImpl previous = this.first;
            FactEntryImpl current = previous;
            while ( current != null ) {
                final FactEntryImpl next = (FactEntryImpl) current.next;
                if ( current.handle.getId() == id ) {
                    if ( this.first == current ) {
                        this.first = next;
                    } else {
                        previous.next = next;
                    }
                    current.next = null;
                    return current;
                }
                previous = current;
                current = next;
            }
            return current;
        }

        //        public boolean matches(int otherHashCode) {
        //            return this.hashCode == otherHashCode && this.index.equal( this.first.getFactHandle().getObject() );
        //        }

        public boolean matches(final Object object,
                               final int objectHashCode) {
            return this.hashCode == objectHashCode && this.index.equal( this.first.getFactHandle().getObject(),
                                                                        object );
        }

        public boolean matches(final ReteTuple tuple,
                               final int tupleHashCode) {
            return this.hashCode == tupleHashCode && this.index.equal( this.first.getFactHandle().getObject(),
                                                                       tuple );
        }

        public int hashCode() {
            return this.hashCode;
        }

        public boolean equals(final Object object) {
            final FieldIndexEntry other = (FieldIndexEntry) object;
            return this.hashCode == other.hashCode && this.index == other.index;
        }

        public String toString() {
            return "FieldIndexEntry( hashCode=" + this.hashCode + " first=" + this.first + " )";
        }

        public void setHashCode(int hashCode) {
            this.hashCode = hashCode;
        }
    }

}