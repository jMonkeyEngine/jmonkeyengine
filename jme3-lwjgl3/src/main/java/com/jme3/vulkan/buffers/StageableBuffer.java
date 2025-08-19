package com.jme3.vulkan.buffers;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.CommandPool;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryFlag;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.sync.SyncGroup;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public class StageableBuffer extends GpuBuffer {

    private final GpuBuffer stage;

    public StageableBuffer(LogicalDevice device, MemorySize size,
                           Flag<BufferUsage> usage, Flag<MemoryFlag> mem, boolean concurrent) {
        super(device, size, usage.add(BufferUsage.TransferDst), mem, concurrent);
        this.stage = new GpuBuffer(device, size, BufferUsage.TransferSrc,
                Flag.of(MemoryFlag.HostVisible, MemoryFlag.HostCoherent), concurrent);
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
