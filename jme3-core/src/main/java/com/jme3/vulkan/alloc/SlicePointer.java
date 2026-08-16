package com.jme3.vulkan.alloc;

import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

public class SlicePointer implements RelativeBuffer {

    private int offset, size;
    private EngineBuffer source;

    public SlicePointer(int offset) {
        this(offset, -1);
    }

    public SlicePointer(int offset, int size) {
        assert offset >= 0 : "Offset must be non-negative.";
        this.offset = offset;
        this.size = size;
    }

    @Override
    public void bind(EngineBuffer source) {
        this.source = source;
    }

    @Override
    public void update(CommandBuffer cmd) {
        source.update(cmd);
    }

    @Override
    public DataBuffer cache() {
        if (size >= 0) {
            return source.cache().offset(offset);
        } else {
            return source.cache().offset(offset, size);
        }
    }

    @Override
    public void invalidateCache() {
        source.invalidateCache();
    }

    @Override
    public int capacity() {
        return size >= 0 ? size : source.capacity() - offset;
    }

    @Override
    public int getBufferLocalOffset() {
        return source.getBufferLocalOffset() + offset;
    }

    @Override
    public long getHandle() {
        return source.getHandle();
    }

    @Override
    public long getDeviceAddress() {
        return source.getDeviceAddress() + offset;
    }

    @Override
    public Flag<Role> getRoles() {
        return source.getRoles();
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return source.getMemoryProperties();
    }

    @Override
    public boolean isDeviceAccessible() {
        return source.isDeviceAccessible();
    }

    public void setOffset(int offset) {
        assert offset >= 0 : "Offset must be non-negative.";
        this.offset = offset;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

}
