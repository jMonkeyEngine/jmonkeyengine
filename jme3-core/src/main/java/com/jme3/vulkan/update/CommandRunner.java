package com.jme3.vulkan.update;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.frames.UpdateFrameManager;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.SyncGroup;

import java.util.Objects;

public class CommandRunner {

    private final LogicalDevice<?> device;
    private final UpdateFrameManager frames;
    private final CommandBuffer buffer;
    private CommandBatch commands;
    private Fence prevRunFence;

    public CommandRunner(LogicalDevice<?> device, UpdateFrameManager frames, CommandBuffer buffer) {
        this(device, frames, buffer, null);
    }

    public CommandRunner(LogicalDevice<?> device, UpdateFrameManager frames, CommandBuffer buffer, CommandBatch commands) {
        this.device = device;
        this.frames = frames;
        this.buffer = buffer;
    }

    public void run(SyncGroup sync) {
        if (prevRunFence != null) {
            prevRunFence.block(5000);
        }
        buffer.resetAndBegin();
        if (Objects.requireNonNull(commands).run(buffer, frames.getCurrentFrame())) {
            prevRunFence = sync.getOrCreateFence(device);
        } else {
            prevRunFence = null;
        }
        buffer.endAndSubmit(sync);
    }

    public void setCommands(CommandBatch commands) {
        this.commands = commands;
    }

    public LogicalDevice<?> getDevice() {
        return device;
    }

    public UpdateFrameManager getFrames() {
        return frames;
    }

    public CommandBuffer getBuffer() {
        return buffer;
    }

    public CommandBatch getCommands() {
        return commands;
    }

}
