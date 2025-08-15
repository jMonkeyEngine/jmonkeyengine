package com.jme3.vulkan.sync;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class Semaphore implements Native<Long> {

    private final LogicalDevice<?> device;
    private final NativeReference ref;
    private long id;
    private int dstStageMask;

    public Semaphore(LogicalDevice<?> device) {
        this(device, 0);
    }

    public Semaphore(LogicalDevice<?> device, int dstStageMask) {
        this.device = device;
        this.dstStageMask = dstStageMask;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo create = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            id = getLong(stack, ptr -> check(vkCreateSemaphore(device.getNativeObject(), create, null, ptr),
                    "Failed to create semaphore."));
            ref = Native.get().register(this);
            device.getNativeReference().addDependent(ref);
        }
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroySemaphore(device.getNativeObject(), nonNull(id), null);
    }

    @Override
    public void prematureNativeDestruction() {
        id = MemoryUtil.NULL;
    }

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public void setDstStageMask(int dstStageMask) {
        this.dstStageMask = dstStageMask;
    }

    public void addDstStageBit(int stageBit) {
        this.dstStageMask |= stageBit;
    }

    public void removeDstStageBit(int stageBit) {
        this.dstStageMask &= ~stageBit;
    }

    public int getDstStageMask() {
        return dstStageMask;
    }

    @Deprecated
    public long getId() {
        return id;
    }

    @Deprecated
    public LongBuffer toBuffer(MemoryStack stack) {
        return stack.longs(id);
    }

}
