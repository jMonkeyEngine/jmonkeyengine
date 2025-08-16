package com.jme3.vulkan.buffers;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.CommandPool;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.flags.MemoryFlags;
import com.jme3.vulkan.flags.BufferUsageFlags;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.SyncGroup;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public class StageableBuffer extends GpuBuffer {

    private final GpuBuffer stage;

    public StageableBuffer(LogicalDevice device, MemorySize size,
                           BufferUsageFlags usage, MemoryFlags mem, boolean concurrent) {
        super(device, size, usage.transferDst(), mem, concurrent);
        this.stage = new GpuBuffer(device, size, new BufferUsageFlags().transferSrc(),
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
        transfer(transferPool, SyncGroup.ASYNC);
    }

    public void transfer(CommandPool transferPool, SyncGroup sync) {
        if (stage.getNativeReference().isDestroyed()) {
            throw new IllegalStateException("Staging buffer has already been freed.");
        }
        CommandBuffer cmd = transferPool.allocateOneTimeCommandBuffer();
        cmd.begin();
        transfer(cmd);
        cmd.endAndSubmit(sync);
    }

    public void transfer(CommandBuffer cmd) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            recordCopy(stack, cmd, stage, 0, 0, size().getBytes());
        }
    }

    public void freeStagingBuffer() {
        stage.freeMemory();
    }

    public GpuBuffer getStagingBuffer() {
        return stage;
    }

}
