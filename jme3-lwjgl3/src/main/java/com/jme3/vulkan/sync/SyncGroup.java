package com.jme3.vulkan.sync;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class SyncGroup {

    private static final Semaphore[] EMPTY = new Semaphore[0];

    private static Semaphore[] toArray(Semaphore s) {
        return s != null ? new Semaphore[] {s} : EMPTY;
    }

    private final Semaphore[] waits;
    private final Semaphore[] signals;
    private final Fence fence;

    public SyncGroup() {
        this(EMPTY, EMPTY, null);
    }

    public SyncGroup(Fence fence) {
        this(EMPTY, EMPTY, fence);
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
        this.waits = waits;
        this.signals = signals;
        this.fence = fence;
    }

    public Semaphore[] getWaits() {
        return waits;
    }

    public Semaphore[] getSignals() {
        return signals;
    }

    public Fence getFence() {
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
        for (Semaphore s : signals) {
            buf.put(s.getDstStageMask());
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

}
