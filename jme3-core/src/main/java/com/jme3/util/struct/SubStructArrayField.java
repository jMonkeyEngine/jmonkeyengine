package com.jme3.util.struct;

import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;
import org.checkerframework.checker.nullness.qual.Nullable;

@Deprecated
public class SubStructArrayField <T extends Struct> implements StructField<StructArray<T>>, StructuredArray<T> {

    private final String name;
    private StructArray<T> alias;
    private Struct struct;
    private int offset, alignment;
    private GpuBuffer buffer;
    private int bufferOffset;

    public SubStructArrayField(StructArray<T> alias) {
        this(null, alias);
    }

    public SubStructArrayField(String name, StructArray<T> alias) {
        assert alias != null : "Alias cannot be null.";
        this.name = name;
        this.alias = alias;
    }

    @Override
    public void bind(Struct struct) {
        this.struct = struct;
    }

    @Override
    public int bind(StructLayout layout, int offset) {
        alignment = Math.max(alias.getStruct().getAlignment(), struct.getLayout().getMinStructAlignment());
        return this.offset = offset + alias.size();
    }

    @Override
    public int getStructLocalOffset() {
        return offset;
    }

    @Override
    public void bind(@Nullable GpuBuffer buffer, int baseOffset) {
        this.buffer = buffer;
        this.bufferOffset = baseOffset;
        alias.bind(buffer, bufferOffset + offset);
    }

    @Override
    public GpuBuffer getBuffer() {
        return buffer;
    }

    @Override
    public int offset() {
        return bufferOffset + offset;
    }

    @Override
    public int size() {
        return alias.size();
    }

    @Override
    public Struct getStruct() {
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
    public int alignment() {
        return alignment;
    }

    @Override
    public T index(int index) {
        return alias.index(index);
    }

    @Override
    public int getLength() {
        return alias.getLength();
    }

    @Override
    public <E extends Struct> E index(int index, E struct) {
        return alias.index(index, struct);
    }

}
