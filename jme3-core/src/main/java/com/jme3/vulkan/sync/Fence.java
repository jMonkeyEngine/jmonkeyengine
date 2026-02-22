package com.jme3.vulkan.sync;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import java.util.concurrent.TimeUnit;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.*;

public class Fence extends AbstractNative<Long> {

    private final LogicalDevice<?> device;

    public Fence(LogicalDevice<?> device) {
        this(device, false);
    }

    public Fence(LogicalDevice<?> device, boolean signal) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkFenceCreateInfo create = VkFenceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(signal ? VK_FENCE_CREATE_SIGNALED_BIT : 0);
            object = getLong(stack, ptr -> check(vkCreateFence(device.getNativeObject(), create, null, ptr),
                    "Failed to create fence."));
            ref = DisposableManager.reference(this);
            device.getReference().addDependent(ref);
        }
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vkDestroyFence(device.getNativeObject(), nonNull(object), null);
    }

    public String toString() {
        return "Fence[" + !isBlocking() + "]";
    }

    public void block(long timeoutMillis) {
        check(vkWaitForFences(device.getNativeObject(), object, true, TimeUnit.MILLISECONDS.toNanos(timeoutMillis)),
                "Fence wait expired.");
    }

    public void blockThenReset(long timeoutMillis) {
        block(timeoutMillis);
        reset();
    }

    public void reset() {
        vkResetFences(device.getNativeObject(), object);
    }

    public SyncGroup toGroup() {
        return new SyncGroup(this);
    }

    public boolean isBlocking() {
        return vkGetFenceStatus(device.getNativeObject(), object) != VK_SUCCESS;
    }

    public long getId() {
        return object;
    }

}
