package com.jme3.util.struct;

import com.jme3.math.FastMath;
import com.jme3.vulkan.alloc.MemoryAddress;
import com.jme3.vulkan.buffer.BufferMapping;
import com.jme3.vulkan.buffer.EngineBuffer;

public class SubStructField <T extends Struct> implements StructField<T> {

    private final String name;
    private T alias;
    private Struct struct;
    private int offset;

    public SubStructField(T alias) {
        this(null, alias);
    }

    public SubStructField(String name, T alias) {
        this.name = name;
        this.alias = alias;
        this.alias.bind(this);
    }

    @Override
    public int bind(Struct struct, int offset) {
        this.struct = struct;
        this.alias.bind(struct.getLayout());
        return this.offset = FastMath.toMultipleOf(offset, getAlignment());
    }

    @Override
    public BufferMapping map() {
        return struct.map().region(offset, struct.getSize());
    }

    @Override
    public EngineBuffer getSourceBuffer() {
        return struct.getSourceBuffer();
    }

    @Override
    public int size() {
        return struct.getSize();
    }

    @Override
    public void set(T value) {
        setAlias(value);
    }

    @Override
    public void setAlias(T value) {
        assert value != null : "Alias cannot be null.";
        this.alias.bind((MemoryAddress)null);
        this.alias = value;
        this.alias.bind(struct.getLayout());
        this.alias.bind(this);
    }

    @Override
    public T get() {
        return alias;
    }

    @Override
    public T alias() {
        return alias;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getSize() {
        return struct.getSize();
    }

    @Override
    public int getAlignment() {
        return struct.getAlignment();
    }

}
