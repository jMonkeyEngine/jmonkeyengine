package com.jme3.vulkan.commands;

import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK11.*;

public class CommandPool implements Native<Long> {

    public enum Create implements Flag<Create> {

        Transient(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT),
        ResetCommandBuffer(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT),
        Protected(VK_COMMAND_POOL_CREATE_PROTECTED_BIT);

        private final int vkEnum;

        Create(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int bits() {
            return vkEnum;
        }

    }

    private final Queue queue;
    private final DisposableReference ref;
    private final Flag<Create> flags;
    private long id;

    public CommandPool(Queue queue) {
        this(queue, Create.ResetCommandBuffer);
    }

    public CommandPool(Queue queue, Flag<Create> flags) {
        this.queue = queue;
        this.flags = flags;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo create = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(this.flags.bits())
                    .queueFamilyIndex(queue.getFamilyIndex());
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateCommandPool(queue.getDevice().getNativeObject(), create, null, idBuf),
                    "Failed to create command pool.");
            id = idBuf.get(0);
        }
        ref = Native.get().register(this);
        queue.getDevice().getReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> {
            vkDestroyCommandPool(queue.getDevice().getNativeObject(), id, null);
        };
    }

    @Override
    public void prematureDestruction() {
        id = VK_NULL_HANDLE;
    }

    @Override
    public DisposableReference getNativeReference() {
        return null;
    }

    public CommandBuffer allocateCommandBuffer() {
        if (flags.contains(Create.Transient)) {
            return new TransientCommandBuffer(this);
        } else {
            return new CommandBuffer(this);
        }
    }

    public LogicalDevice<?> getDevice() {
        return queue.getDevice();
    }

    public Queue getQueue() {
        return queue;
    }

    public Flag<Create> getFlags() {
        return flags;
    }

}
