package com.jme3.vulkan.update;

import com.jme3.vulkan.commands.CommandBuffer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BasicCommandBatch implements CommandBatch {

    private final List<WeakReference<Command>> commands = new ArrayList<>();

    @Override
    public boolean run(CommandBuffer cmd, int frame) {
        boolean run = false;
        for (Iterator<WeakReference<Command>> it = commands.iterator(); it.hasNext();) {
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
    public <T extends Command> T add(T command) {
        commands.add(new WeakReference<>(command));
        return command;
    }

    @Override
    public void remove(Command command) {
        commands.removeIf(ref -> ref.get() == command);
    }

}
