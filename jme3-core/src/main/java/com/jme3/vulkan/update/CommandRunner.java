package com.jme3.vulkan.update;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.sync.SyncGroup;

import java.util.function.Consumer;

public class CommandRunner {

    private int frame;
    private Command command;
    private CommandBuffer buffer;

    public CommandRunner(int frame, CommandBuffer buffer, Command command) {
        this.frame = frame;
        this.buffer = buffer;
        this.command = command;
    }

    public void run(SyncGroup sync, Consumer<CommandBuffer> onCommandBufferUsed) {
        if (command.requiresCommandBuffer(frame)) {
            if (onCommandBufferUsed != null) {
                onCommandBufferUsed.accept(buffer);
            }
            buffer.resetAndBegin();
            command.run(buffer, frame);
            buffer.endAndSubmit(sync != null ? sync : SyncGroup.ASYNC);
        } else {
            command.run(null, frame);
        }
    }

    public void run(SyncGroup sync) {
        run(sync, null);
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

    public int getTargetFrame() {
        return frame;
    }

    public CommandBuffer getCommandBuffer() {
        return buffer;
    }

    public Command getCommand() {
        return command;
    }

}
