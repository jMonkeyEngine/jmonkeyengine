package com.jme3.vulkan.sync;

import com.jme3.util.natives.AbstractNative;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK14.*;

public abstract class Semaphore extends AbstractNative<Long> {

    public static final Semaphore[] EMPTY = new Semaphore[0];

    protected final LogicalDevice<?> device;

    protected Semaphore(LogicalDevice<?> device) {
        this.device = device;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroySemaphore(device.getNativeObject(), nonNull(object), null);
    }

    public SyncGroup toGroupWait() {
        return new SyncGroup(this, EMPTY);
    }

    public SyncGroup toGroupSignal() {
        return new SyncGroup(EMPTY, this);
    }

    @Deprecated
    public long getId() {
        return object;
    }

    @Deprecated
    public LongBuffer toBuffer(MemoryStack stack) {
        return stack.longs(object);
    }



}
