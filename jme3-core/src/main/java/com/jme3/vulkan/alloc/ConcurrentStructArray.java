package com.jme3.vulkan.alloc;

import com.jme3.util.struct.Struct;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ConcurrentStructArray <T extends Struct> extends StructArray<T> {

    private final Supplier<T> factory;
    private final List<Index<T>> structs = new ArrayList<>();

    public ConcurrentStructArray(Supplier<T> factory) {
        this(factory, null);
    }

    public ConcurrentStructArray(Supplier<T> factory, Memory source) {
        super(factory.get(), source);
        this.factory = factory;
        structs.add(getSharedStruct());
    }

    @Override
    public T index(int i) {
        if (i >= getLength()) {
            throw new IndexOutOfBoundsException(i + " is out of bounds.");
        }
        while (i >= structs.size()) {
            Index<T> s = new Index<>(factory.get(), structs.size() * getByteStride());
            s.getPointer().bind(this);
            structs.add(s);
        }
        return structs.get(i).getStruct();
    }

}
