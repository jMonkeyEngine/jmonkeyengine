package com.jme3.vulkan.alloc;

import java.nio.ByteBuffer;

public class SlicePointer implements MemoryPointer {

    private Memory source;
    private int offset, size;

    public SlicePointer(int offset, int size) {
        assert offset >= 0 : "Offset must be non-negative.";
        assert size > 0 : "Size must be positive";
        this.offset = offset;
        this.size = size;
    }

    public void setOffset(int offset) {
        assert offset >= 0 : "Offset must be non-negative.";
        this.offset = offset;
    }

    public void setSize(int size) {
        assert size > 0 : "Size must be positive.";
        this.size = size;
    }

    public void set(int offset, int size) {
        setOffset(offset);
        setSize(size);
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

    @Override
    public void bind(Memory memory) {
        this.source = memory;
    }

    @Override
    public Memory getBoundMemory() {
        return source;
    }

    @Override
    public ByteBuffer map(MappingArena arena) {
        ByteBuffer buf = source.map(arena);
        return buf.position(buf.position() + offset);
    }

    @Override
    public ByteBuffer map() {
        ByteBuffer buf = source.map();
        return buf.position(buf.position() + offset);
    }

    @Override
    public void stage(long offset, long size) {
        source.stage(this.offset + offset, size);
    }

    @Override
    public void stage() {
        source.stage(offset, size);
    }

    @Override
    public void destroy() {
        if (source != null) source.destroy();
    }

}
