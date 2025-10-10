package com.jme3.vulkan.update;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.frames.UpdateFrameManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PerFrameCommandBatch implements CommandBatch {

    private final UpdateFrameManager<?> frames;
    private final List<List<WeakReference<Command>>> commands;

    public PerFrameCommandBatch(UpdateFrameManager<?> frames) {
        this.frames = frames;
        this.commands = new ArrayList<>(frames.getTotalFrames());
        for (int i = 0; i < frames.getTotalFrames(); i++) {
            commands.add(new ArrayList<>());
        }
    }

    @Override
    public boolean run(CommandBuffer cmd, int frame) {
        boolean run = false;
        for (Iterator<WeakReference<Command>> it = commands.get(frames.getCurrentFrame()).iterator(); it.hasNext();) {
            Command c = it.next().get();
            if (c == null) {
                it.remove();
            } else {
                run = c.run(cmd, frame) || run;
            }
        }
        return run;
    }

    @Override
    public <T extends Command> T add(int frame, T command) {
        commands.get(frame).add(new WeakReference<>(command));
        return command;
    }

    @Override
    public void remove(Command command) {
        for (List<WeakReference<Command>> cmd : commands) {
            cmd.removeIf(ref -> ref.get() == command);
        }
    }

}
