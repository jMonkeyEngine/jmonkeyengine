package com.jme3.util.struct;

import com.jme3.vulkan.alloc.StructArray;
import com.jme3.vulkan.buffer.BufferMapping;
import com.jme3.vulkan.buffer.EngineBuffer;

public class SubStructArrayField <T extends Struct> implements StructField<StructArray<T>> {

    private final String name;
    private StructArray<T> alias;
    private Struct struct;
    private int offset, alignment;

    public SubStructArrayField(StructArray<T> alias) {
        this(null, alias);
    }

    public SubStructArrayField(String name, StructArray<T> alias) {
        assert alias != null : "Alias cannot be null.";
        this.name = name;
        this.alias = alias;
        this.alias.bind(this);
    }

    @Override
    public int bind(Struct struct, int offset) {
        this.struct = struct;
        for (T a : alias) {
            a.bind(struct.getLayout());
        }
        alignment = Math.max(alias.getStruct().getAlignment(), struct.getLayout().getMinStructAlignment());
        return this.offset = offset + alias.getByteSize();
    }

    @Override
    public BufferMapping map() {
        return struct.map().offset(offset);
    }

    @Override
    public EngineBuffer getSourceBuffer() {
        return struct.getSourceBuffer();
    }

    @Override
    public int size() {
        return alias.size();
    }

    @Override
    public void set(StructArray<T> value) {
        setAlias(value);
    }

    @Override
    public void setAlias(StructArray<T> value) {
        assert alias != null : "Alias cannot be null.";
        alias.bind(null);
        alias = value;
        alignment = struct.getLayout() != null ? struct.getLayout().getMinStructAlignment() : 0;
    }

    @Override
    public StructArray<T> get() {
        return alias;
    }

    @Override
    public StructArray<T> alias() {
        return alias;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getSize() {
        return alias.getByteSize();
    }

    @Override
    public int getAlignment() {
        return alignment;
    }

    public T index(int index) {
        return alias.index(index);
    }

    public <E extends Struct> E index(int index, E struct) {
        return alias.index(index, struct);
    }

}
