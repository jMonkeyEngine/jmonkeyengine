package com.jme3.vulkan.frames;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.update.Command;

import java.util.function.IntFunction;

public class PerFrameCommand<T extends Command> extends PerFrameResource<T> implements Command {

    public PerFrameCommand(UpdateFrameManager frames, IntFunction<T> generator) {
        super(frames, generator);
    }

    @Override
    public boolean run(CommandBuffer cmd, int frame) {
        return get(frame).run(cmd, frame);
    }

}
