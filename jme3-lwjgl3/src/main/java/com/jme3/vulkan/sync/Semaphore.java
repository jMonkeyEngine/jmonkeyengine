package com.jme3.vulkan.sync;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pipelines.PipelineStage;
import com.jme3.vulkan.util.Flag;
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
        return new SyncGroup(this, SyncGroup.EMPTY_SEMAPHORE_ARRAY);
    }

    public SyncGroup toGroupSignal() {
        return new SyncGroup(SyncGroup.EMPTY_SEMAPHORE_ARRAY, this);
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
