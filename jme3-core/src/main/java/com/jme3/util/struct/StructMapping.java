package com.jme3.util.struct;

import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.VirtualBufferMapping;

import java.util.Iterator;

/**
 * Maps a struct to interact with a buffer's memory.
 *
 * @param <T>
 */
public class StructMapping <T extends Struct> implements StructSequence<T>, AutoCloseable {

    private final T struct;
    private final int stride;
    private final int offset;
    private final BufferMapping mapping;
    private int sampledIndex;

    public StructMapping(T struct, BufferMapping mapping) {
        this(struct, mapping, 0);
    }

    public StructMapping(T struct, BufferMapping mapping, int offset) {
        this.struct = struct;
        this.stride = struct.getAlignedSize();
        this.offset = offset;
        this.mapping = new VirtualBufferMapping(mapping);
        sample(0);
    }

    public StructMapping(T struct, MappableBuffer buffer) {
        this(struct, buffer, 0, 0);
    }

    public StructMapping(T struct, MappableBuffer buffer, int offset) {
        this(struct, buffer, offset, 0);
    }

    public StructMapping(T struct, MappableBuffer buffer, int offset, int bytes) {
        this.struct = struct;
        this.stride = struct.getAlignedSize();
        this.offset = offset * stride;
        this.mapping = buffer.map(this.offset, bytes == 0 ? buffer.size().getBytes() - offset : bytes);
        sample(0);
    }

    @Override
    public void close() {
        mapping.close();
    }

    @Override
    public Iterator<Integer> iterator() {
        return new IteratorImpl();
    }

    /**
     * Binds the {@link #get() struct} to the memory address represented by
     * the struct array index. In other words, {@code index} specifies
     * the distance in struct size from the underlying buffer's base memory
     * address the struct should be bound to.
     *
     * @param index struct array index
     */
    @Override
    public void sample(int index) {
        this.sampledIndex = index;
        struct.bind(mapping, offset + index * stride);
    }

    /**
     * Binds {@link #get() struct} to the next struct array element.
     */
    @Override
    public void increment() {
        sample(sampledIndex + 1);
    }

    /**
     * Binds {@link #get() struct} to the previous struct array element.
     */
    @Override
    public void decrement() {
        sample(sampledIndex - 1);
    }

    /**
     * Gets the struct controlled by this struct mapping.
     *
     * @return controlled struct
     */
    @Override
    public T get() {
        return struct;
    }

    private class IteratorImpl implements Iterator<Integer> {

        private final long limit = mapping.getSize() / stride;
        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < limit;
        }

        @Override
        public Integer next() {
            struct.bind(mapping, offset + stride * index);
            return index++;
        }

    }

}
