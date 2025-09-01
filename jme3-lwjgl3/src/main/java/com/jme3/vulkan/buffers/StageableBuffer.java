package com.jme3.vulkan.buffers;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.CommandPool;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.sync.SyncGroup;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public class StageableBuffer extends BasicVulkanBuffer {

    private final VulkanBuffer stage;

    public StageableBuffer(LogicalDevice device, MemorySize size,
                           Flag<BufferUsage> usage, Flag<MemoryProp> mem, boolean concurrent) {
        super(device, size, usage.add(BufferUsage.TransferDst), mem, concurrent);
        this.stage = new BasicVulkanBuffer(device, size, BufferUsage.TransferSrc,
                Flag.of(MemoryProp.HostVisible, MemoryProp.HostCoherent), concurrent);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return stage.map(offset, size);
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
