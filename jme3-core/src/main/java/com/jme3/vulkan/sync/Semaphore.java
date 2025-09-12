package com.jme3.vulkan.sync;

import com.jme3.util.natives.Native;
import com.jme3.vulkan.AbstractNative;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pipelines.PipelineStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class Semaphore extends AbstractNative<Long> {

    public static final Semaphore[] EMPTY = new Semaphore[0];

    private final LogicalDevice<?> device;
    private Flag<PipelineStage> dstStageMask;

    public Semaphore(LogicalDevice<?> device) {
        this(device, PipelineStage.None);
    }

    public Semaphore(LogicalDevice<?> device, Flag<PipelineStage> dstStageMask) {
        this.device = device;
        this.dstStageMask = dstStageMask;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo create = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            object = getLong(stack, ptr -> check(vkCreateSemaphore(device.getNativeObject(), create, null, ptr),
                    "Failed to create semaphore."));
            ref = Native.get().register(this);
            device.getNativeReference().addDependent(ref);
        }
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroySemaphore(device.getNativeObject(), nonNull(object), null);
    }

    public void setDstStageMask(Flag<PipelineStage> dstStageMask) {
        this.dstStageMask = dstStageMask;
    }

    public void addDstStage(Flag<PipelineStage> stageBit) {
        this.dstStageMask = this.dstStageMask.add(stageBit);
    }

    public void removeDstStageBit(Flag<PipelineStage> stageBit) {
        this.dstStageMask = this.dstStageMask.remove(stageBit);
    }

    public Flag<PipelineStage> getDstStageMask() {
        return dstStageMask;
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
