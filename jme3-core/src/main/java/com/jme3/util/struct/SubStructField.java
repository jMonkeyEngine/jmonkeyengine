package com.jme3.util.struct;

import com.jme3.math.FastMath;
import com.jme3.vulkan.buffer.DataBuffer;

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
    public DataBuffer cache() {
        return struct.cache().offset(offset);
    }

    @Override
    public void set(T value) {
        alias(value);
    }

    @Override
    public void alias(T value) {
        assert value != null : "Alias cannot be null.";
        this.alias.bind((Memory)null);
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
