package com.jme3.util.struct;

import com.jme3.vulkan.buffers.BufferMapping;

public class StructMapping <T extends Struct> implements AutoCloseable {

    private final BufferMapping mapping;
    private final StructLayout layout;
    private final T struct;

    public StructMapping(BufferMapping mapping, StructLayout layout, T struct) {
        this.mapping = mapping;
        this.layout = layout;
        this.struct = struct;
    }

    @Override
    public void close() {
        struct.write(layout, mapping.getAddress());
    }

    public T getStruct() {
        return struct;
    }

    public static <T extends Struct> StructMapping<T> readWrite(BufferMapping mapping, StructLayout layout, T struct) {
        struct.read(layout, mapping.getAddress());
        return new StructMapping<>(mapping, layout, struct);
    }

    public static <T extends Struct> StructMapping<T> write(BufferMapping mapping, StructLayout layout, T struct) {
        return new StructMapping<>(mapping, layout, struct);
    }

}
