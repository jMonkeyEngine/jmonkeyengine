package com.jme3.vulkan.buffers;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.CommandPool;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.sync.SyncGroup;
import com.jme3.vulkan.update.Command;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public class StageableBuffer extends BasicVulkanBuffer implements Command {

    protected VulkanBuffer stage;
    protected BufferRegion dirtyRegion;

    public StageableBuffer(LogicalDevice device, MemorySize size) {
        super(device, size);
        usage = usage.add(BufferUsage.TransferDst);
    }

    @Override
    public boolean run(CommandBuffer cmd, int frame) {
        return transfer(cmd);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        updateDirtyRegion(offset, size);
        if (stage == null) {
            stage = createStagingBuffer();
        }
        return stage.map(offset, size);
    }

    @Override
    public void unmap() {
        stage.unmap();
    }

    @Override
    public void resize(int elements) {
        super.resize(elements);
        if (stage != null) {
            stage.resize(elements);
        }
    }

    protected VulkanBuffer createStagingBuffer() {
        BasicVulkanBuffer buf = new BasicVulkanBuffer(getDevice(), size(), padding);
        try (BasicVulkanBuffer.Builder b = buf.build()) {
            b.setUsage(BufferUsage.TransferSrc);
            b.setMemFlags(Flag.of(MemoryProp.HostVisible, MemoryProp.HostCoherent));
        }
        return buf;
    }

    protected void updateDirtyRegion(int offset, int size) {
        if (dirtyRegion != null) {
            dirtyRegion.unionLocal(offset, size);
        } else {
            dirtyRegion = new BufferRegion(offset, size);
        }
    }

    public void transfer(CommandPool transferPool) {
        transfer(transferPool, SyncGroup.ASYNC);
    }

    public void transfer(CommandPool transferPool, SyncGroup sync) {
        if (stage != null && dirtyRegion != null) {
            CommandBuffer cmd = transferPool.allocateTransientCommandBuffer();
            cmd.begin();
            transfer(cmd);
            cmd.endAndSubmit(sync);
        }
    }

    public boolean transfer(CommandBuffer cmd) {
        if (stage != null && dirtyRegion != null) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                recordCopy(stack, cmd, stage, dirtyRegion.getOffset(), dirtyRegion.getOffset(), dirtyRegion.getSize());
            }
            dirtyRegion = null;
            return true;
        }
        return false;
    }

    public void freeStagingBuffer() {
        stage = null;
    }

    public GpuBuffer getStagingBuffer() {
        return stage;
    }

}
