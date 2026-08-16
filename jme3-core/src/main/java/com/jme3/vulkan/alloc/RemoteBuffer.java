package com.jme3.vulkan.alloc;

import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

import java.nio.ByteBuffer;

public class RemoteBuffer implements EngineBuffer, Memory {

    private final EngineBuffer remote;
    private final DataBuffer cache;

    public RemoteBuffer(EngineBuffer remote) {
        this.remote = remote;
        this.cache = new DataBuffer(ByteBuffer.wrap(new byte[remote.capacity()]));
    }

    @Override
    public int size() {
        return cache.capacity();
    }

    @Override
    public DataBuffer cache() {
        return cache.clear();
    }

    @Override
    public void flushCache() {
        cmd.cmdStreamToRemote(cache, remote);
    }

    @Override
    public void invalidateCache() {
        cmd.cmdStreamFromRemote(remote, cache.getBytes());
    }

    @Override
    public int capacity() {
        return remote.capacity();
    }

    @Override
    public int getBufferLocalOffset() {
        return remote.getBufferLocalOffset();
    }

    @Override
    public long getHandle() {
        return remote.getHandle();
    }

    @Override
    public long getDeviceAddress() {
        return remote.getDeviceAddress();
    }

    @Override
    public Flag<Role> getRoles() {
        return remote.getRoles();
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return remote.getMemoryProperties();
    }
}
