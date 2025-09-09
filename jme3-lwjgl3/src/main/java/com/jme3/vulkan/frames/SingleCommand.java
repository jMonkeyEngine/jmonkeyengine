package com.jme3.vulkan.frames;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.update.Command;

public class SingleCommand <T extends Command> extends SingleResource<T> implements Command {

    public SingleCommand() {}

    public SingleCommand(T resource) {
        super(resource);
    }

    @Override
    public boolean run(CommandBuffer cmd, int frame) {
        return get(frame).run(cmd, frame);
    }

}
