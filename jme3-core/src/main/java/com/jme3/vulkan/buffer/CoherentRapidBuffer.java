package com.jme3.vulkan.buffer;

import com.jme3.util.destruction.ResourceLifecycle;
import com.jme3.vulkan.buffer.alloc.BufferType;
import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.buffer.tracking.BufferTracker;
import com.jme3.vulkan.buffer.tracking.NullBufferTracker;
import com.jme3.vulkan.frames.FrameIndex;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class CoherentRapidBuffer implements GpuBuffer {

    private final FrameIndex frames;
    private final GpuBuffer[] buffers;
    private final DataBuffer cache;

    public CoherentRapidBuffer(MemoryAllocator allocator, FrameIndex frames, int capacity, Flag<Role> roles) {
        this.frames = frames;
        this.buffers = new GpuBuffer[frames.count()];
        this.cache = new DataBuffer(ByteBuffer.wrap(new byte[capacity]), new MultiTracker());
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = allocator.createBuffer(BufferType.Streaming, capacity, roles);
        }
    }

    public CoherentRapidBuffer(FrameIndex frames, GpuBuffer[] buffers) {
        assert buffers.length == frames.count();
        this.frames = frames;
        this.buffers = buffers;
        this.cache = new DataBuffer(ByteBuffer.wrap(new byte[buffers[0].capacity()]));
    }

    @Override
    public void flushCache() {
        GpuBuffer current = buffers[frames.current()];
        cache.copyTo(current.cache(), new BufferCopy().add(current.cache().getTracker()));
        current.flushCache();
        current.cache().getTracker().clear();
    }

    @Override
    public void invalidateCache() {
        GpuBuffer current = buffers[frames.current()];
        current.invalidateCache();
        current.cache().copyAllTo(cache);
        for (GpuBuffer b : buffers) {
            b.cache().getTracker().add(0, cache.capacity());
        }
    }

    @Override
    public int capacity() {
        return cache.capacity();
    }

    @Override
    public int offset() {
        return buffers[frames.current()].offset();
    }

    @Override
    public long getHandle() {
        return buffers[frames.current()].getHandle();
    }

    @Override
    public Flag<Role> getRoles() {
        return buffers[frames.current()].getRoles();
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return buffers[frames.current()].getMemoryProperties();
    }

    @Override
    public DataBuffer cache() {
        return cache;
    }

    @Override
    public ResourceLifecycle getLifecycle() {
        return buffers[frames.current()].getLifecycle();
    }

    private class MultiTracker implements BufferTracker {

        @Override
        public void add(int start, int size) {
            for (GpuBuffer b : buffers) {
                b.cache().getTracker().add(start, size);
            }
        }

        @Override
        public void remove(int start, int size) {
            for (GpuBuffer b : buffers) {
                b.cache().getTracker().remove(start, size);
            }
        }

        @Override
        public void clear() {
            for (GpuBuffer b : buffers) {
                b.cache().getTracker().clear();
            }
        }

        @Override
        public int getNumIslands() {
            return 0;
        }

        @Override
        public int getNumCovered() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Iterator<Island> iterator() {
            return NullBufferTracker.ITERATOR;
        }

    }

}
