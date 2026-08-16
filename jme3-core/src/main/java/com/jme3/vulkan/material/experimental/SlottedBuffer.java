package com.jme3.vulkan.material.experimental;

import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

import java.util.BitSet;
import java.util.function.BiFunction;

public class SlottedBuffer<T extends EngineBuffer> implements EngineBuffer {

    private final BitSet usedSlots = new BitSet();
    private T buffer;
    private int length = 16;

    public int acquireSlot(BiFunction<Integer, T, T> factory) {
        int i;
        synchronized (usedSlots) {
            i = usedSlots.nextClearBit(0);
            usedSlots.set(i);
        }
        if (buffer == null || i >= length) {
            buffer = factory.apply(length *= 2, buffer);
        }
        return i;
    }

    public void releaseSlot(int slotIndex) {
        usedSlots.clear(slotIndex);
    }

    public T getBuffer() {
        return buffer;
    }

    public int getLength() {
        return length;
    }

    public int getSlotsInUse() {
        return usedSlots.cardinality();
    }

    public boolean isEmpty() {
        return usedSlots.isEmpty();
    }

    @Override
    public void update(CommandBuffer cmd) {
        buffer.update(cmd);
    }

    @Override
    public DataBuffer cache() {
        return buffer.cache();
    }

    @Override
    public void invalidateCache() {
        buffer.invalidateCache();
    }

    @Override
    public int capacity() {
        return buffer.capacity();
    }

    @Override
    public int getBufferLocalOffset() {
        return buffer.getBufferLocalOffset();
    }

    @Override
    public long getHandle() {
        return buffer.getHandle();
    }

    @Override
    public long getDeviceAddress() {
        return buffer.getDeviceAddress();
    }

    @Override
    public Flag<Role> getRoles() {
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
}
