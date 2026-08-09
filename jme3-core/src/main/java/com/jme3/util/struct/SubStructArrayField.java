package com.jme3.util.struct;

import com.jme3.vulkan.alloc.StructArray;
import com.jme3.vulkan.buffer.DataBuffer;

public class SubStructArrayField <T extends Struct> implements StructField<StructArray<T>>, StructuredArray<T> {

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
        return this.offset = offset + alias.capacity();
    }

    @Override
    public Struct getBoundStruct() {
        return struct;
    }

    @Override
    public DataBuffer cache() {
        return struct.cache().offset(offset);
    }

    @Override
    public void set(StructArray<T> value) {
        alias(value);
    }

    @Override
    public void alias(StructArray<T> value) {
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
    public int capacity() {
        return alias.capacity();
    }

    @Override
    public int getAlignment() {
        return alignment;
    }

    @Override
    public int getBufferLocalOffset() {
        return struct.getBufferLocalOffset() + offset;
    }

    @Override
    public T index(int index) {
        return alias.index(index);
    }

    @Override
    public int getIndex() {
        return alias.getIndex();
    }

    @Override
    public int getLength() {
        return alias.getLength();
    }

    public <E extends Struct> E index(int index, E struct) {
        return alias.index(index, struct);
    }

}
