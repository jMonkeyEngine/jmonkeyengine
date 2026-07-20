package com.jme3.vulkan.alloc.address;

import com.jme3.vulkan.buffer.Handle;

import java.nio.ByteBuffer;

@Deprecated
public class HostAddress implements Address {

    private final Handle<ByteBuffer> source;
    private final ByteBuffer slice;

    public HostAddress(Handle<ByteBuffer> buffer) {
        this.source = buffer;
        this.slice = buffer.get();
    }

    protected HostAddress(Handle<ByteBuffer> source, ByteBuffer slice) {
        this.source = source;
        this.slice = slice;
    }

    @Override
    public Address slice(int offset, int size) {
        return new HostAddress(source, slice.duplicate().position(offset).limit(offset + size).slice());
    }

    @Override
    public int size() {
        return slice.capacity();
    }

    public ByteBuffer data() {
        return slice;
    }

}
