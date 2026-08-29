package com.jme3.vulkan.alloc;

import com.jme3.util.struct.Struct;

import java.util.BitSet;

public class SparseStructList<T extends Struct> extends StructArray<T> {

    private final BitSet reserved = new BitSet();

    public SparseStructList(int length, T struct) {
        super(length, struct);
    }

    public SparseStructList(int length, SparseStructList<T> array) {
        super(length, array.getStruct());
        reserved.or(array.reserved);
    }

    /**
     * Acquires an available element that is not yet owned. If there are no
     * elements available, a new element is appended to this list.
     *
     * @return index of the acquired element
     */
    public int acquireElement() {
        int i = reserved.nextClearBit(0);
        reserved.set(i);
        if (i >= getLength()) {
            setLength(i + 1);
        }
        return i;
    }

    /**
     * Releases the element at {@code index} from ownership.
     *
     * @param index index of the element to release
     */
    public void releaseElement(int index) {
        reserved.clear(index);
    }

    /**
     * Number of elements currently available in the list.
     *
     * @return number of available elements
     */
    public int available() {
        return getLength() - reserved.cardinality();
    }

}
