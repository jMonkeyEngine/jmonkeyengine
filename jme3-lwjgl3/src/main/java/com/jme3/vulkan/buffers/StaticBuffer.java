package com.jme3.vulkan.buffers;

import com.jme3.vulkan.commands.CommandPool;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.uniforms.BufferUniform;
import com.jme3.vulkan.memory.MemoryFlag;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.SyncGroup;
import com.jme3.vulkan.sync.TaskQueue;
import com.jme3.vulkan.util.Flag;

import java.util.concurrent.*;

public class StaticBuffer extends StageableBuffer {

    private final CommandPool transferPool;

    public StaticBuffer(LogicalDevice device, CommandPool transferPool, MemorySize size, Flag<BufferUsage> usage, Flag<MemoryFlag> mem, boolean concurrent) {
        super(device, size, usage, mem, concurrent);
        this.transferPool = transferPool;
    }

    @Override
    public void unmap() {
        super.unmap();
        SyncGroup sync = new SyncGroup(new Fence(getDevice(), false));
        transfer(transferPool, sync);
        sync.getFence().block(5000);
        freeStagingBuffer();
    }

    public void unmapAsync(TaskQueue queue, SyncGroup sync) {
        if (!sync.containsFence()) {
            throw new IllegalArgumentException("SyncGroup must contain a fence.");
        }
        transfer(transferPool, sync);
        queue.submit(new FutureTask<>(() -> {
            sync.getFence().block(5000);
            freeStagingBuffer();
        }, true));
    }

}
