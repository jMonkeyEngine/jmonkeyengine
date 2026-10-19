package com.jme3.vulkan.buffer;

import com.jme3.util.destruction.ResourceLifecycle;
import com.jme3.vulkan.buffer.alloc.BufferType;
import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.frames.FrameIndex;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

import java.util.function.Function;

/**
 * A {@link GpuBuffer} for data rewritten every frame, backed by whichever strategy the
 * device's memory actually supports.
 * <p>
 * Tries to allocate one host-visible {@link BufferType#Dynamic} copy per frame-in-flight
 * slot, the same way {@link RapidBuffer}/{@link CoherentRapidBuffer} do, so that writing
 * this frame's copy never races the device reading an earlier frame's. If the allocator
 * ever hands back a buffer that is not host-accessible — i.e. the device has no
 * host-visible memory type suitable for this buffer's capacity and roles — per-frame
 * copies are pointless, since the CPU could not write to them directly anyway. In that
 * case every buffer allocated so far is destroyed, and the one buffer that revealed the
 * problem is wrapped with {@code staticFallback} instead (ordinarily a {@link
 * StaticBuffer}, written through a staging buffer and device copy commands rather than a
 * direct host mapping).
 * <p>
 * Use {@link #rapid} or {@link #coherentRapid} to construct one, depending on whether
 * {@link #cache()} should reflect every frame-in-flight slot at once
 * ({@link CoherentRapidBuffer}) or only the current slot ({@link RapidBuffer}).
 */
public class DynamicBuffer implements GpuBuffer {

    private final GpuBuffer buffer;

    /**
     * Builds a buffer whose {@link #cache()} is coherent across every frame-in-flight
     * slot (see {@link CoherentRapidBuffer}), or falls back to {@code staticFallback} if
     * per-frame host-visible copies aren't possible.
     *
     * @param allocator allocates the per-frame {@link BufferType#Dynamic} buffers
     * @param frames reports which frame-in-flight slot is current
     * @param capacity size in bytes of each per-frame copy
     * @param roles usage roles each per-frame copy must support
     * @param staticFallback wraps the single device-local-only buffer for the case where
     *                       per-frame host-visible copies are not possible; ordinarily
     *                       {@code base -> new StaticBuffer(commands, stream, base)}
     * @return the constructed buffer
     */
    public static DynamicBuffer coherentRapid(MemoryAllocator allocator, FrameIndex frames, int capacity,
            Flag<Role> roles, Function<GpuBuffer, GpuBuffer> staticFallback) {
        return new DynamicBuffer(allocator, frames, capacity, roles, true, staticFallback);
    }

    /**
     * Builds a buffer with one independent copy per frame-in-flight slot (see
     * {@link RapidBuffer}), or falls back to {@code staticFallback} if per-frame
     * host-visible copies aren't possible.
     *
     * @param allocator allocates the per-frame {@link BufferType#Dynamic} buffers
     * @param frames reports which frame-in-flight slot is current
     * @param capacity size in bytes of each per-frame copy
     * @param roles usage roles each per-frame copy must support
     * @param staticFallback wraps the single device-local-only buffer for the case where
     *                       per-frame host-visible copies are not possible; ordinarily
     *                       {@code base -> new StaticBuffer(commands, stream, base)}
     * @return the constructed buffer
     */
    public static DynamicBuffer rapid(MemoryAllocator allocator, FrameIndex frames, int capacity,
            Flag<Role> roles, Function<GpuBuffer, GpuBuffer> staticFallback) {
        return new DynamicBuffer(allocator, frames, capacity, roles, false, staticFallback);
    }

    private DynamicBuffer(MemoryAllocator allocator, FrameIndex frames, int capacity, Flag<Role> roles,
            boolean coherent, Function<GpuBuffer, GpuBuffer> staticFallback) {
        GpuBuffer[] buffers = new GpuBuffer[frames.count()];
        for (int i = 0; i < frames.count(); i++) {
            GpuBuffer base = allocator.createBuffer(BufferType.Dynamic, capacity, roles);
            if (!base.isHostAccessible()) {
                for (int j = 0; j < i; j++) {
                    buffers[j].destroy();
                }
                buffer = staticFallback.apply(base);
                return;
            }
            buffers[i] = base;
        }
        buffer = coherent ? new CoherentRapidBuffer(frames, buffers) : new RapidBuffer(frames, buffers);
    }

    @Override
    public void flushCache() {
        buffer.flushCache();
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
    public int offset() {
        return buffer.offset();
    }

    @Override
    public long getHandle() {
        return buffer.getHandle();
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
    public DataBuffer cache() {
        return buffer.cache();
    }

    @Override
    public ResourceLifecycle getLifecycle() {
        return buffer.getLifecycle();
    }

}
