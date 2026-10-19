package com.jme3.util.struct;

import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;

import java.util.List;
import java.util.function.Consumer;

/**
 * Concurrent element of a struct array.
 *
 * @param <T> struct type
 */
public class ConcurrentElement <T extends Struct> extends Struct<StructField> implements AutoCloseable {

    private final T struct;
    private final Consumer<ConcurrentElement<T>> release;
    private long layoutVersion = -1;

    public ConcurrentElement(T struct, Consumer<ConcurrentElement<T>> release) {
        this.struct = struct;
        this.release = release;
    }

    @Override
    public void close() {
        release.accept(this);
    }

    @Override
    public void setupLayout(StructLayout layout) {
        struct.setupLayout(layout);
    }

    @Override
    public void bind(GpuBuffer buffer, int offset) {
        struct.bind(buffer, offset);
    }

    @Override
    public void unbind() {
        struct.unbind();
    }

    @Override
    public DataBuffer cache() {
        return struct.cache();
    }

    @Override
    public int alignment() {
        return struct.alignment();
    }

    @Override
    public int size() {
        return struct.size();
    }

    @Override @SuppressWarnings("unchecked")
    public List<StructField> getFields() {
        return struct.getFields();
    }

    public void bind(StructLayout layout, long version, GpuBuffer buffer, int offset) {
        if (layoutVersion != version) {
            struct.setupLayout(layout);
            layoutVersion = version;
        }
        struct.bind(buffer, offset);
    }

    public T get() {
        return struct;
    }

}
