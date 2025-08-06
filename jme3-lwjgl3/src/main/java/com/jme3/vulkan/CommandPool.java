package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class CommandPool implements Native<Long> {

    private final LogicalDevice<?> device;
    private final Queue queue;
    private final NativeReference ref;
    private long id;

    public CommandPool(LogicalDevice<?> device, Queue queue, boolean isTransient, boolean reset) {
        this.device = device;
        this.queue = queue;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo create = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags((isTransient ? VK_COMMAND_POOL_CREATE_TRANSIENT_BIT : 0)
                            | (reset ? VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT : 0))
                    .queueFamilyIndex(queue.getFamilyIndex());
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateCommandPool(device.getNativeObject(), create, null, idBuf), "Failed to create command pool.");
            id = idBuf.get(0);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            vkDestroyCommandPool(device.getNativeObject(), id, null);
        };
    }

    @Override
    public void prematureNativeDestruction() {
        id = VK_NULL_HANDLE;
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

    public LogicalDevice<?> getDevice() {
        return device;
    }

    public Queue getQueue() {
        return queue;
    }

}
