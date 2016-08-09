/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.util;

import java.util.*;

/**
 *  <p>Provides a list with similar modification semantics to java.util.concurrent's
 *  CopyOnWriteArrayList except that it is not concurrent and also provides
 *  direct access to the current array.  This List allows modification of the
 *  contents while iterating as any iterators will be looking at a snapshot of
 *  the list at the time they were created.  Similarly, access the raw internal
 *  array is only presenting a snap shot and so can be safely iterated while
 *  the list is changing.</p>
 *
 *  <p>All modifications, including set() operations will cause a copy of the
 *  data to be created that replaces the old version.  Because this list is
 *  not designed for threading concurrency it further optimizes the "many modifications"
 *  case by buffering them as a normal ArrayList until the next time the contents
 *  are accessed.</p>
 *
 *  <p>Normal list modification performance should be equal to ArrayList in a
 *  many situations and always better than CopyOnWriteArrayList.  Optimum usage
 *  is when modifications are done infrequently or in batches... as is often the
 *  case in a scene graph.  Read operations perform superior to all other methods
 *  as the array can be accessed directly.</p>
 *
 *  <p>Important caveats over normal java.util.Lists:</p>
 *  <ul>
 *  <li>Even though this class supports modifying the list, the subList() method
 *  returns a read-only list.  This technically breaks the List contract.</li>
 *  <li>The ListIterators returned by this class only support the remove()
 *  modification method.  add() and set() are not supported on the iterator.
 *  Even after ListIterator.remove() or Iterator.remove() is called, this change
 *  is not reflected in the iterator instance as it is still refering to its
 *  original snapshot.
 *  </ul>
 *
 *  @version   $Revision$
 *  @author    Paul Speed
 */
public class SafeArrayList<E> implements List<E>, Cloneable {

    // Implementing List directly to avoid accidentally acquiring
    // incorrect or non-optimal behavior from AbstractList.  For
    // example, the default iterator() method will not work for
    // this list.

    // Note: given the particular use-cases this was intended,
    //       it would make sense to nerf the public mutators and
    //       make this publicly act like a read-only list.
    //       SafeArrayList-specific methods could then be exposed
    //       for the classes like Node and Spatial to use to manage
    //       the list.  This was the callers couldn't remove a child
    //       without it being detached properly, for example.

    private Class<E> elementType;
    private List<E> buffer;
    private E[] backingArray;
    private int size = 0;

    public SafeArrayList(Class<E> elementType) {
        this.elementType = elementType;
    }

    public SafeArrayList(Class<E> elementType, Collection<? extends E> c) {
        this.elementType = elementType;
        addAll(c);
    }

    public SafeArrayList<E> clone() {
        try {
            SafeArrayList<E> clone = (SafeArrayList<E>)super.clone();

            // Clone whichever backing store is currently active
            if( backingArray != null ) {
                clone.backingArray = backingArray.clone();
            }
            if( buffer != null ) {
                clone.buffer = (List<E>)((ArrayList<E>)buffer).clone();
            }

            return clone;
        } catch( CloneNotSupportedException e ) {
            throw new AssertionError();
        }
    }

    protected final <T> T[] createArray(Class<T> type, int size) {
        return (T[])java.lang.reflect.Array.newInstance(type, size);
    }

    protected final E[] createArray(int size) {
        return createArray(elementType, size);
    }

    /**
     *  Returns a current snapshot of this List's backing array that
     *  is guaranteed not to change through further List manipulation.
     *  Changes to this array may or may not be reflected in the list and
     *  should be avoided.
     */
    public final E[] getArray() {
        if( backingArray != null )
            return backingArray;

        if( buffer == null ) {
            backingArray = createArray(0);
        } else {
            // Only keep the array or the buffer but never both at
            // the same time.  1) it saves space, 2) it keeps the rest
            // of the code safer.
            backingArray = buffer.toArray( createArray(buffer.size()) );
            buffer = null;
        }
        return backingArray;
    }

    protected final List<E> getBuffer() {
        if( buffer != null )
            return buffer;

        if( backingArray == null ) {
            buffer = new ArrayList();
        } else {
            // Only keep the array or the buffer but never both at
            // the same time.  1) it saves space, 2) it keeps the rest
            // of the code safer.
            buffer = new ArrayList( Arrays.asList(backingArray) );
            backingArray = null;
        }
        return buffer;
    }

    public final int size() {
        return size;
    }

    public final boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    public Iterator<E> iterator() {
        return listIterator();
    }

    public Object[] toArray() {
        return getArray();
    }

    public <T> T[] toArray(T[] a) {

        E[] array = getArray();
        if (a.length < array.length) {
            return (T[])Arrays.copyOf(array, array.length, a.getClass());
        }

        System.arraycopy( array, 0, a, 0, array.length );

        if (a.length > array.length) {
            a[array.length] = null;
        }

        return a;
    }

    public boolean add(E e) {
        boolean result = getBuffer().add(e);
        size = getBuffer().size();
        return result;
    }

    public boolean remove(Object o) {
        boolean result = getBuffer().remove(o);
        size = getBuffer().size();
        return result;
    }

    public boolean containsAll(Collection<?> c) {
        return Arrays.asList(getArray()).containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        boolean result = getBuffer().addAll(c);
        size = getBuffer().size();
        return result;
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        boolean result = getBuffer().addAll(index, c);
        size = getBuffer().size();
        return result;
    }

    public boolean removeAll(Collection<?> c) {
        boolean result = getBuffer().removeAll(c);
        size = getBuffer().size();
        return result;
    }

    public boolean retainAll(Collection<?> c) {
        boolean result = getBuffer().retainAll(c);
        size = getBuffer().size();
        return result;
    }

    public void clear() {
        getBuffer().clear();
        size = 0;
    }

    public boolean equals(Object o) {
        if( o == this )
            return true;
        if( !(o instanceof List) ) //covers null too
            return false;
        List other = (List)o;
        Iterator i1 = iterator();
        Iterator i2 = other.iterator();
        while( i1.hasNext() && i2.hasNext() ) {
            Object o1 = i1.next();
            Object o2 = i2.next();
            if( o1 == o2 )
                continue;
            if( o1 == null || !o1.equals(o2) )
                return false;
        }
        return !(i1.hasNext() || i2.hasNext());
    }

    public int hashCode() {
        // Exactly the hash code described in the List interface, basically
        E[] array = getArray();
        int result = 1;
        for( E e : array ) {
            result = 31 * result + (e == null ? 0 : e.hashCode());
        }
        return result;
    }

    public final E get(int index) {
        if( backingArray != null )
            return backingArray[index];
        if( buffer != null )
            return buffer.get(index);
        throw new IndexOutOfBoundsException( "Index:" + index + ", Size:0" );
    }

    public E set(int index, E element) {
        return getBuffer().set(index, element);
    }

    public void add(int index, E element) {
        getBuffer().add(index, element);
        size = getBuffer().size();
    }

    public E remove(int index) {
        E result = getBuffer().remove(index);
        size = getBuffer().size();
        return result;
    }

    public int indexOf(Object o) {
        E[] array = getArray();
        for( int i = 0; i < array.length; i++ ) {
            E element = array[i];
            if( element == o ) {
                return i;
            }
            if( element != null && element.equals(o) ) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(Object o) {
        E[] array = getArray();
        for( int i = array.length - 1; i >= 0; i-- ) {
            E element = array[i];
            if( element == o ) {
                return i;
            }
            if( element != null && element.equals(o) ) {
                return i;
            }
        }
        return -1;
    }

    public ListIterator<E> listIterator() {
        return new ArrayIterator<E>(getArray(), 0);
    }

    public ListIterator<E> listIterator(int index) {
        return new ArrayIterator<E>(getArray(), index);
    }

    public List<E> subList(int fromIndex, int toIndex) {

        // So far JME doesn't use subList that I can see so I'm nerfing it.
        List<E> raw =  Arrays.asList(getArray()).subList(fromIndex, toIndex);
        return Collections.unmodifiableList(raw);
    }

    public String toString() {

        E[] array = getArray();
        if( array.length == 0 ) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for( int i = 0; i < array.length; i++ ) {
            if( i > 0 )
                sb.append( ", " );
            E e = array[i];
            sb.append( e == this ? "(this Collection)" : e );
        }
        sb.append(']');
        return sb.toString();
    }

    protected class ArrayIterator<E> implements ListIterator<E> {
        private E[] array;
        private int next;
        private int lastReturned;

        protected ArrayIterator( E[] array, int index ) {
            this.array = array;
            this.next = index;
            this.lastReturned = -1;
        }

        public boolean hasNext() {
            return next != array.length;
        }

        public E next() {
            if( !hasNext() )
                throw new NoSuchElementException();
            lastReturned = next++;
            return array[lastReturned];
        }

        public boolean hasPrevious() {
            return next != 0;
        }

        public E previous() {
            if( !hasPrevious() )
                throw new NoSuchElementException();
            lastReturned = --next;
            return array[lastReturned];
        }

        public int nextIndex() {
            return next;
        }

        public int previousIndex() {
            return next - 1;
        }

        public void remove() {
            // This operation is not so easy to do but we will fake it.
            // The issue is that the backing list could be completely
            // different than the one this iterator is a snapshot of.
            // We'll just remove(element) which in most cases will be
            // correct.  If the list had earlier .equals() equivalent
            // elements then we'll remove one of those instead.  Either
            // way, none of those changes are reflected in this iterator.
            SafeArrayList.this.remove( array[lastReturned] );
        }

        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }
}
