package com.jme3.util.struct;

import com.jme3.vulkan.alloc.MappingArena;
import com.jme3.vulkan.alloc.StructArray;

import java.nio.ByteBuffer;

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
        return this.offset = offset + alias.getAlignedSize();
    }

    @Override
    public ByteBuffer map(MappingArena arena) {
        ByteBuffer buf = struct.map(arena);
        return buf.position(buf.position() + offset);
    }

    @Override
    public ByteBuffer map() {
        ByteBuffer buf = struct.map();
        return buf.position(buf.position() + offset);
    }

    @Override
    public void stage(long offset, long size) {
        struct.stage(this.offset + offset, size);
    }

    @Override
    public void stage() {
        struct.stage(offset, struct.getAlignedSize());
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
        return alias.getAlignedSize();
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
