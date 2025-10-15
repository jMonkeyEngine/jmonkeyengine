package com.jme3.vulkan.update;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.sync.SyncGroup;

public class CommandRunner implements Runnable {

    private int frame;
    private Command command;
    private CommandBuffer buffer;
    private SyncGroup sync = new SyncGroup();

    public CommandRunner(int frame, CommandBuffer buffer, Command command) {
        this.frame = frame;
        this.buffer = buffer;
        this.command = command;
    }

    @Override
    public void run() {
        if (command.requiresCommandBuffer(frame)) {
            buffer.resetAndBegin();
            command.run(buffer, frame);
            buffer.endAndSubmit(sync);
        } else {
            command.run(null, frame);
        }
    }

    public void setTargetFrame(int frame) {
        this.frame = frame;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public void setCommandBuffer(CommandBuffer buffer) {
        this.buffer = buffer;
    }

    public void setSync(SyncGroup sync) {
        this.sync = sync;
    }

    public int getTargetFrame() {
        return frame;
    }

    public CommandBuffer getCommandBuffer() {
        return buffer;
    }

    public Command getCommand() {
        return command;
    }

    public SyncGroup getSync() {
        return sync;
    }

}
