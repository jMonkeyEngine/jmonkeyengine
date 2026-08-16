package com.jme3.vulkan.buffer;

import com.jme3.vulkan.buffer.tracking.BufferTracker;
import com.jme3.vulkan.buffer.tracking.ExactBufferTracker;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

public class PartitionedBuffer implements EngineBuffer {

    private final EngineBuffer buffer;
    private final BufferTracker consumed = new ExactBufferTracker();

    public PartitionedBuffer(EngineBuffer buffer) {
        this.buffer = buffer;
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

    /**
     * Creates a {@link Partition} in this buffer of {@code capacity} bytes from a continuous
     * unconsumed region. If no continuous region is available then no partition is created
     * and {@code null} is returned.
     *
     * @param capacity size in bytes of the partition
     * @return the created partition, or null if no room for the partition was found
     */
    public Partition create(int capacity) {
        if (consumed.isEmpty()) {
            consumed.add(0, capacity);
            return new Partition(0, capacity);
        }
        for (BufferTracker.Island i : consumed) {
            if (i.getAvailableAfter(buffer.capacity()) >= capacity) {
                consumed.add(i.getEnd(), capacity);
                return new Partition(i.getEnd(), capacity);
            }
        }
        return null;
    }

    public Partition create(int capacity, boolean exceptOnFail) {
        Partition p = create(capacity);
        if (exceptOnFail && p == null) {
            throw new NullPointerException("Failed to create partition: not enough continuous space found.");
        }
        return p;
    }

    public class Partition implements EngineBuffer {

        private final int offset, capacity;

        private Partition(int offset, int capacity) {
            this.offset = offset;
            this.capacity = capacity;
        }

        @Override
        public void update(CommandBuffer cmd) {
            PartitionedBuffer.this.update(cmd);
        }

        @Override
        public DataBuffer cache() {
            return PartitionedBuffer.this.cache().offset(offset, capacity);
        }

        @Override
        public void invalidateCache() {
            PartitionedBuffer.this.invalidateCache();
        }

        @Override
        public int capacity() {
            return capacity;
        }

        @Override
        public int getBufferLocalOffset() {
            return PartitionedBuffer.this.getBufferLocalOffset() + offset;
        }

        @Override
        public long getHandle() {
            return PartitionedBuffer.this.getHandle();
        }

        @Override
        public long getDeviceAddress() {
            return PartitionedBuffer.this.getDeviceAddress() + offset;
        }

        @Override
        public Flag<Role> getRoles() {
            return PartitionedBuffer.this.getRoles();
        }

        @Override
        public Flag<MemoryProp> getMemoryProperties() {
            return PartitionedBuffer.this.getMemoryProperties();
        }

        @Override
        public boolean isDeviceAccessible() {
            return PartitionedBuffer.this.isDeviceAccessible();
        }

        /**
         * Releases the buffer region consumed by this partition.
         */
        public void release() {
            consumed.remove(offset, capacity);
        }

    }

}
