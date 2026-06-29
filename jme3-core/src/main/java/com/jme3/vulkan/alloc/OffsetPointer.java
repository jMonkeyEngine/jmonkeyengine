package com.jme3.vulkan.alloc;

import com.jme3.vulkan.buffer.BufferMapping;
import com.jme3.vulkan.buffer.EngineBuffer;

public class OffsetPointer implements RelativeAddress {

    private MemoryAddress parent;
    private int offset;

    public OffsetPointer(int offset) {
        assert offset >= 0 : "Offset must be non-negative.";
        this.offset = offset;
    }

    public void setOffset(int offset) {
        assert offset >= 0 : "Offset must be non-negative.";
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public void bind(MemoryAddress parent) {
        this.parent = parent;
    }

    @Override
    public MemoryAddress getParentAddress() {
        return parent;
    }

    @Override
    public BufferMapping map() {
        return parent.map().offset(offset);
    }

    @Override
    public EngineBuffer getSourceBuffer() {
        return parent.getSourceBuffer();
    }

    @Override
    public int size() {
        return parent.size();
    }

}
