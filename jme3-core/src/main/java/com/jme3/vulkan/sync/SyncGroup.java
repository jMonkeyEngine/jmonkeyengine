package com.jme3.vulkan.sync;

import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Objects;

public class SyncGroup {
    
    public static final SyncGroup ASYNC = new SyncGroup();

    private static Semaphore[] toArray(Semaphore s) {
        return s != null ? new Semaphore[] {s} : Semaphore.EMPTY;
    }

    private Semaphore[] waits;
    private Semaphore[] signals;
    private Fence fence;

    public SyncGroup() {
        this(Semaphore.EMPTY, Semaphore.EMPTY, null);
    }

    public SyncGroup(Fence fence) {
        this(Semaphore.EMPTY, Semaphore.EMPTY, fence);
    }

    public SyncGroup(Semaphore wait, Semaphore signal) {
        this(toArray(wait), toArray(signal), null);
    }

    public SyncGroup(Semaphore wait, Semaphore[] signals) {
        this(toArray(wait), signals, null);
    }

    public SyncGroup(Semaphore[] waits, Semaphore signal) {
        this(waits, toArray(signal), null);
    }

    public SyncGroup(Semaphore[] waits, Semaphore[] signals) {
        this(waits, signals, null);
    }

    public SyncGroup(Semaphore wait, Semaphore signal, Fence fence) {
        this(toArray(wait), toArray(signal), fence);
    }

    public SyncGroup(Semaphore wait, Semaphore[] signals, Fence fence) {
        this(toArray(wait), signals, fence);
    }

    public SyncGroup(Semaphore[] waits, Semaphore signal, Fence fence) {
        this(waits, toArray(signal), fence);
    }

    public SyncGroup(Semaphore[] waits, Semaphore[] signals, Fence fence) {
        this.waits = Objects.requireNonNull(waits);
        this.signals = Objects.requireNonNull(signals);
        this.fence = fence;
    }

    public void setWaits(Semaphore... waits) {
        this.waits = Objects.requireNonNull(waits);
    }

    public void setWaits(SyncGroup sync) {
        this.waits = sync.waits;
    }

    public Semaphore[] getWaits() {
        return waits;
    }

    public void setSignals(Semaphore... signals) {
        this.signals = Objects.requireNonNull(signals);
    }

    public void setSignals(SyncGroup sync) {
        this.signals = sync.signals;
    }

    public Semaphore[] getSignals() {
        return signals;
    }

    public void setFence(Fence fence) {
        this.fence = fence;
    }

    public void setFence(SyncGroup sync) {
        this.fence = sync.fence;
    }

    public Fence getFence() {
        return fence;
    }

    public Fence getOrCreateFence(LogicalDevice<?> device) {
        return getOrCreateFence(device, false);
    }

    public Fence getOrCreateFence(LogicalDevice<?> device, boolean signal) {
        if (fence == null) {
            fence = new Fence(device, signal);
        }
        return fence;
    }

    public boolean containsWaits() {
        return waits.length > 0;
    }

    public boolean containsSignals() {
        return signals.length > 0;
    }

    public boolean containsFence() {
        return fence != null;
    }

    public LongBuffer toWaitBuffer(MemoryStack stack) {
        LongBuffer buf = stack.mallocLong(waits.length);
        for (Semaphore w : waits) {
            buf.put(w.getNativeObject());
        }
        buf.flip();
        return buf;
    }

    public IntBuffer toDstStageBuffer(MemoryStack stack) {
        IntBuffer buf = stack.mallocInt(waits.length);
        for (Semaphore s : waits) {
            if (s.getDstStageMask().isEmpty()) {
                throw new IllegalStateException("Wait semaphore destination stage mask cannot be empty.");
            }
            buf.put(s.getDstStageMask().bits());
        }
        buf.flip();
        return buf;
    }

    public LongBuffer toSignalBuffer(MemoryStack stack) {
        LongBuffer buf = stack.mallocLong(signals.length);
        for (Semaphore s : signals) {
            buf.put(s.getNativeObject());
        }
        buf.flip();
        return buf;
    }

    public long getFenceHandle() {
        return fence != null ? fence.getNativeObject() : VK10.VK_NULL_HANDLE;
    }

    public static SyncGroup wait(Semaphore... waits) {
        return new SyncGroup(waits, Semaphore.EMPTY);
    }

    public static SyncGroup signal(Semaphore... signals) {
        return new SyncGroup(Semaphore.EMPTY, signals);
    }

}
