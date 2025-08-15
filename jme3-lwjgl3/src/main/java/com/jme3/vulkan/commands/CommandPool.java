package com.jme3.vulkan.commands;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK11.*;

public class CommandPool implements Native<Long> {

    private final Queue queue;
    private final NativeReference ref;
    private final boolean shortLived, reusable, protect;
    private long id;

    public CommandPool(Queue queue, boolean shortLived, boolean reusable, boolean protect) {
        this.queue = queue;
        this.shortLived = shortLived;
        this.reusable = reusable;
        this.protect = protect;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo create = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags((shortLived ? VK_COMMAND_POOL_CREATE_TRANSIENT_BIT : 0)
                        | (reusable ? VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT : 0)
                        | (protect ? VK_COMMAND_POOL_CREATE_PROTECTED_BIT : 0))
                    .queueFamilyIndex(queue.getFamilyIndex());
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateCommandPool(queue.getDevice().getNativeObject(), create, null, idBuf),
                    "Failed to create command pool.");
            id = idBuf.get(0);
        }
        ref = Native.get().register(this);
        queue.getDevice().getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            vkDestroyCommandPool(queue.getDevice().getNativeObject(), id, null);
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
        return queue.getDevice();
    }

    public Queue getQueue() {
        return queue;
    }

    public boolean isShortLived() {
        return shortLived;
    }

    public boolean isReusable() {
        return reusable;
    }

    public boolean isProtected() {
        return protect;
    }

}
