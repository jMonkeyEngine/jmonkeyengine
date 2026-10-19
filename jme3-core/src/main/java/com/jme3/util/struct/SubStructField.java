package com.jme3.util.struct;

import com.jme3.math.FastMath;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;
import org.checkerframework.checker.nullness.qual.Nullable;

@Deprecated
public class SubStructField <T extends Struct> implements StructField<T> {

    private final String name;
    private T alias;
    private Struct struct;
    private int structOffset;
    private GpuBuffer buffer;
    private int bufferOffset;

    public SubStructField(T alias) {
        this(null, alias);
    }

    public SubStructField(String name, T alias) {
        this.name = name;
        this.alias = alias;
    }

    @Override
    public void bind(Struct struct) {
        this.struct = struct;
        this.alias.bind(struct.getLayout());
    }

    @Override
    public int bind(StructLayout layout, int offset) {
        return this.structOffset = FastMath.toMultipleOfPow2(offset, alignment());
    }

    @Override
    public Struct getStruct() {
        return struct;
    }

    @Override
    public int getStructLocalOffset() {
        return structOffset;
    }

    @Override
    public void bind(@Nullable GpuBuffer buffer, int baseOffset) {
        this.buffer = buffer;
        this.bufferOffset = baseOffset;
        alias.bind(buffer, baseOffset + structOffset);
    }

    @Override
    public GpuBuffer getBuffer() {
        return buffer;
    }

    @Override
    public int offset() {
        return bufferOffset + structOffset;
    }

    @Override
    public int size() {
        return alias.size();
    }

    @Override
    public DataBuffer cache() {
        return struct.cache().offset(structOffset);
    }

    @Override
    public void set(T value) {
        alias(value);
    }

    @Override
    public void alias(T value) {
        assert value != null : "Alias cannot be null.";
        this.alias = value;
        this.alias.bind(struct.getLayout());
        this.alias.bind(buffer, bufferOffset);
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
    public int alignment() {
        return struct.alignment();
    }

}
