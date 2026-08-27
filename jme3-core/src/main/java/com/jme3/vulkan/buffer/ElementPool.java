package com.jme3.vulkan.buffer;

import com.jme3.util.struct.Struct;
import com.jme3.vulkan.alloc.StructArray;

/**
 * Buffer that allocates sets of adjacent elements in a struct array on demand.
 */
public class ElementPool <T extends Struct> extends StructArray<T> {

    private final DynamicBuffer<ElementPool<T>> buffer = new DynamicBuffer<>();

    public ElementPool(int length, T struct) {
        super(length, struct);
    }

}
