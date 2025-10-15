package com.jme3.vulkan.update;

import com.jme3.vulkan.commands.CommandBuffer;

public class FrameLocalCommand implements Command {

    private final int frame;
    private final Command delegate;

    public FrameLocalCommand(int frame, Command delegate) {
        this.frame = frame;
        this.delegate = delegate;
    }

    @Override
    public boolean requiresCommandBuffer(int frame) {
        return this.frame == frame && delegate.requiresCommandBuffer(frame);
    }

    @Override
    public void run(CommandBuffer cmd, int frame) {
        if (this.frame == frame) {
            delegate.run(cmd, frame);
        }
    }

    @Override
    public boolean removeByCommand(Command command) {
        return this == command || delegate.removeByCommand(command);
    }

}
