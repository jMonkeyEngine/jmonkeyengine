package com.jme3.vulkan.buffer;

import com.jme3.util.destruction.ResourceLifecycle;
import com.jme3.vulkan.buffer.alloc.BufferType;
import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.frames.FrameIndex;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

/**
 * A {@link GpuBuffer} for data rewritten every frame, such as per-frame camera or
 * timing uniforms.
 * <p>
 * A single buffer written every frame would force the CPU to either stall until the
 * device is done reading the previous frame's data, or race it. Instead, this keeps one
 * host-visible, persistently mapped {@link BufferType#Dynamic} copy per frame-in-flight
 * slot reported by a {@link FrameIndex}, and every {@link GpuBuffer} method transparently
 * operates on whichever copy belongs to the current frame. Writing through
 * {@link #cache()} this frame never risks racing the device, which is still at most
 * reading the other slots from earlier frames.
 * <p>
 * This relies on whoever advances the {@link FrameIndex} to already guarantee the
 * device has finished with a slot before it comes back around to the CPU (typically by
 * waiting on that slot's completion fence before reusing it) — this class only picks
 * which slot to use each time it's asked, it does not itself wait for anything.
 */
public class RapidBuffer implements GpuBuffer {

    private final FrameIndex frames;
    private final GpuBuffer[] slots;

    public RapidBuffer(MemoryAllocator allocator, FrameIndex frames, int capacity, Flag<Role> roles) {
        this.frames = frames;
        this.slots = new GpuBuffer[frames.count()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = allocator.createBuffer(BufferType.Streaming, capacity, roles);
        }
    }

    public RapidBuffer(FrameIndex frames, GpuBuffer[] buffers) {
        assert buffers.length == frames.count();
        this.frames = frames;
        this.slots = buffers;
    }

    private GpuBuffer current() {
        return slots[frames.current()];
    }

    @Override
    public DataBuffer cache() {
        return current().cache();
    }

    @Override
    public void flushCache() {
        current().flushCache();
    }

    @Override
    public void invalidateCache() {
        current().invalidateCache();
    }

    @Override
    public int capacity() {
        return current().capacity();
    }

    @Override
    public int offset() {
        return current().offset();
    }

    @Override
    public long getHandle() {
        return current().getHandle();
    }

    @Override
    public Flag<Role> getRoles() {
        return current().getRoles();
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return current().getMemoryProperties();
    }

    @Override
    public ResourceLifecycle getLifecycle() {
        return current().getLifecycle();
    }

    /**
     * Gets the backing buffer for frame-in-flight slot {@code i} directly, mainly for
     * inspection or testing; prefer the {@link GpuBuffer} methods on this class, which
     * always operate on the current slot.
     *
     * @param i slot index, in {@code [0, getFrameCount())}
     * @return the slot's backing buffer
     */
    public GpuBuffer getSlot(int i) {
        return slots[i];
    }

    /**
     * Gets the number of frame-in-flight slots this buffer keeps a copy for.
     *
     * @return slot count
     */
    public int getFrameCount() {
        return slots.length;
    }

}
