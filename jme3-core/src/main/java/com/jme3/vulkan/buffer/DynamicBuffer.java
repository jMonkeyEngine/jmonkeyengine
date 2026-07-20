package com.jme3.vulkan.buffer;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

public abstract class DynamicBuffer <T extends EngineBuffer> implements EngineBuffer {

    private T buffer;
    private int targetCapacity;
    private Flag<BufferRole> targetRoles;

    public DynamicBuffer(int capacity, Flag<BufferRole> roles) {
        this.targetCapacity = capacity;
        this.targetRoles = roles;
    }

    @Override
    public void update(CommandBuffer cmd) {
        buffer.update(cmd);
    }

    @Override
    public DataBuffer cache() {
        update();
        return buffer.cache();
    }

    @Override
    public void flushCache() {
        update();
        buffer.flushCache();
    }

    @Override
    public void invalidateCache() {
        update();
        buffer.invalidateCache();
    }

    @Override
    public int capacity() {
        return targetCapacity;
    }

    @Override
    public int getInternalOffset() {
        update();
        return buffer.getInternalOffset();
    }

    @Override
    public long getHandle() {
        update();
        return buffer.getHandle();
    }

    @Override
    public long getDeviceAddress() {
        update();
        return buffer.getDeviceAddress();
    }

    @Override
    public Flag<BufferRole> getRoles() {
        return buffer.getRoles();
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return buffer.getMemoryProperties();
    }

    @Override
    public boolean isDeviceAccessible() {
        return buffer.isDeviceAccessible();
    }

    public void update() {
        if (buffer == null) {
            buffer = createBuffer(pickNextSize(0, targetCapacity), targetRoles.add(BufferRole.TransferSrc));
        } else if (buffer.capacity() < targetCapacity || !buffer.getRoles().contains(targetRoles)) {
            T temp = createBuffer(pickNextSize(buffer.capacity(), targetCapacity),
                    targetRoles.add(buffer.getRoles(), BufferRole.TransferSrc, BufferRole.TransferDst));
            copy(buffer, temp);
            buffer = temp;
        }
    }

    public void setCapacity(int capacity) {
        assert capacity > 0 : "Capacity must be positive.";
        targetCapacity = capacity;
    }

    public void addRoles(Flag<BufferRole> roles) {
        targetRoles = roles.addNonNull(targetRoles);
    }

    protected abstract T createBuffer(int bytes, Flag<BufferRole> roles);

    protected abstract void copy(T src, T dst);

    protected int pickNextSize(int currentSize, int requestedSize) {
        // next power of two at or above requestedSize
        return Math.max(Integer.highestOneBit(requestedSize - 1) << 1, Math.max(1, currentSize));
    }

}
