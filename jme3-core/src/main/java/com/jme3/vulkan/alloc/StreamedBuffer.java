package com.jme3.vulkan.alloc;

import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.commands.RenderCommands;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

public class StreamedBuffer implements EngineBuffer, Memory {



    @Override
    public void resize(RenderCommands cmd, int bytes) {

    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public DataBuffer cache() {
        return null;
    }

    @Override
    public void flushCache() {

    }

    @Override
    public void invalidateCache() {

    }

    @Override
    public int capacity() {
        return 0;
    }

    @Override
    public int getBufferLocalOffset() {
        return 0;
    }

    @Override
    public long getHandle() {
        return 0;
    }

    @Override
    public long getDeviceAddress() {
        return 0;
    }

    @Override
    public Flag<Role> getRoles() {
        return null;
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return null;
    }
}
