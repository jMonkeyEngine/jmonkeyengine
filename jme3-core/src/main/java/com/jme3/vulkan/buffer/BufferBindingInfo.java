package com.jme3.vulkan.buffer;

public class BufferBindingInfo {

    private GpuBuffer buffer;
    private int offset, limit;

    public BufferBindingInfo() {}

    public DataBuffer cache() {
        return buffer.cache().slice(offset, limit);
    }

    public void unbind() {
        buffer = null;
    }

    public GpuBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(GpuBuffer buffer) {
        this.buffer = buffer;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getSize() {
        return limit - offset;
    }

    public boolean isBound() {
        return buffer != null;
    }

}
