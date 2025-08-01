package com.jme3.vulkan.buffers;

import com.jme3.vulkan.*;
import com.jme3.vulkan.flags.MemoryFlags;
import com.jme3.vulkan.flags.BufferUsageFlags;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public class StageableBuffer extends GpuBuffer {

    private final GpuBuffer stage;

    public StageableBuffer(LogicalDevice device, int size, BufferUsageFlags usage, MemoryFlags mem, boolean concurrent) {
        super(device, size, usage.transferDst(), mem, concurrent);
        stage = new GpuBuffer(device, size, new BufferUsageFlags().transferSrc(),
                new MemoryFlags().hostVisible().hostCoherent(), concurrent);
    }

    @Override
    public PointerBuffer map(MemoryStack stack, int offset, int size, int flags) {
        return stage.map(stack, offset, size, flags);
    }

    @Override
    public void unmap() {
        stage.unmap();
    }

    @Override
    public void freeMemory() {
        super.freeMemory();
        stage.freeMemory();
    }

    public void transfer(CommandPool transferPool) {
        transfer(transferPool, null, null, null);
        transferPool.getQueue().waitIdle();
    }

    public void transfer(CommandPool transferPool, Semaphore wait, Semaphore signal, Fence fence) {
        if (stage.getNativeReference().isDestroyed()) {
            throw new IllegalStateException("Staging buffer has already been freed.");
        }
        CommandBuffer commands = transferPool.allocateOneTimeCommandBuffer();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            recordCopy(stack, commands, stage, 0, 0, size());
        }
        commands.submit(wait, signal, fence);
    }

    public void freeStagingBuffer() {
        stage.freeMemory();
    }

    public GpuBuffer getStagingBuffer() {
        return stage;
    }

}
