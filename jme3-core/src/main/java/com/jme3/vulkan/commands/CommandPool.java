package com.jme3.vulkan.commands;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import java.nio.LongBuffer;
import java.util.function.BiFunction;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK11.*;

public class CommandPool extends AbstractNative<Long> {

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

    private final CommandQueue queue;
    private final Flag<Create> flags;

    public CommandPool(CommandQueue queue) {
        this(queue, Create.ResetCommandBuffer);
    }

    public CommandPool(CommandQueue queue, Flag<Create> flags) {
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
            object = idBuf.get(0);
        }
        ref = DisposableManager.reference(this);
        queue.getDevice().getReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return object;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vkDestroyCommandPool(queue.getDevice().getNativeObject(), object, null);
    }

    /**
     * Allocates {@code n} command buffers from this pool.
     *
     * @param level level of the allocated buffers
     * @param n number of buffers to allocate
     * @param factory generates a wrapper for the vulkan command buffer handle
     * @return allocated buffers
     */
    public <T extends CommandBuffer> T[] allocate(IntEnum<CommandBuffer.Level> level, int n, BiFunction<CommandPool, VkCommandBuffer, T> factory) {
        if (n <= 0) {
            throw new IllegalArgumentException("Must allocate at least one command buffer.");
        }
        T[] buffers = (T[])new CommandBuffer[n];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocate = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(object)
                    .level(level.getEnum())
                    .commandBufferCount(buffers.length);
            PointerBuffer ptrs = stack.mallocPointer(buffers.length);
            check(vkAllocateCommandBuffers(queue.getDevice().getNativeObject(), allocate, ptrs),
                    "Failed to allocate command buffers");
            for (int i = 0; i < buffers.length; i++) {
                buffers[i] = factory.apply(this, new VkCommandBuffer(ptrs.get(), queue.getDevice().getNativeObject()));
            }
        }
        return buffers;
    }

    /**
     * Allocates {@code n} command buffers from this pool.
     *
     * @param level level of the allocated buffers
     * @param n number of buffers to allocate
     * @return allocated buffers
     * @see #allocate(IntEnum, int, BiFunction)
     */
    public CommandBuffer[] allocate(IntEnum<CommandBuffer.Level> level, int n) {
        return allocate(level, n, CommandBuffer::new);
    }

    /**
     * Allocates a command buffer from this pool.
     *
     * @param level level of the allocated buffer
     * @param factory generates a wrapper for the vulkan command buffer handle
     * @return allocated buffer
     * @see #allocate(IntEnum, int, BiFunction)
     */
    public <T extends CommandBuffer> T allocate(IntEnum<CommandBuffer.Level> level, BiFunction<CommandPool, VkCommandBuffer, T> factory) {
        return allocate(level, 1, factory)[0];
    }

    /**
     * Allocates a command buffer from this pool.
     *
     * @param level level of the allocated buffer
     * @return allocated buffer
     * @see #allocate(IntEnum, int, BiFunction)
     */
    public CommandBuffer allocate(IntEnum<CommandBuffer.Level> level) {
        return allocate(level, 1, CommandBuffer::new)[0];
    }

    public LogicalDevice<?> getDevice() {
        return queue.getDevice();
    }

    public CommandQueue getQueue() {
        return queue;
    }

    public Flag<Create> getFlags() {
        return flags;
    }

}
