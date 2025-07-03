package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class CommandPool implements Native<Long> {

    private final LogicalDevice device;
    private final Queue queue;
    private final NativeReference ref;
    private LongBuffer id = MemoryUtil.memAllocLong(1);

    public CommandPool(LogicalDevice device, Queue queue, boolean isTransient, boolean reset) {
        this.device = device;
        this.queue = queue;
        VkCommandPoolCreateInfo create = VkCommandPoolCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags((isTransient ? VK_COMMAND_POOL_CREATE_TRANSIENT_BIT : 0)
                        | (reset ? VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT : 0))
                .queueFamilyIndex(queue.getFamilyIndex());
        check(vkCreateCommandPool(device.getNativeObject(), create, null, id), "Failed to create command pool.");
        create.close();
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id != null ? id.get(0) : null;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            vkDestroyCommandPool(device.getNativeObject(), id.get(0), null);
            MemoryUtil.memFree(id);
        };
    }

    @Override
    public void prematureNativeDestruction() {
        id = null;
    }

    @Override
    public NativeReference getNativeReference() {
        return null;
    }

    public CommandBuffer allocateCommandBuffer() {
        return new CommandBuffer(this);
    }

    public OneTimeCommandBuffer allocateOneTimeCommandBuffer() {
        return new OneTimeCommandBuffer(this);
    }

    public LogicalDevice getDevice() {
        return device;
    }

    public Queue getQueue() {
        return queue;
    }

}
