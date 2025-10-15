package com.jme3.vulkan.update;

import com.jme3.vulkan.frames.VersionedResource;

public interface CommandBatch extends Command {

    <T extends Command> T add(T command);

    void remove(Command command);

    default <T extends Command> T add(int frame, T command) {
        add(new FrameLocalCommand(frame, command));
        return command;
    }

    default <T extends VersionedResource<? extends Command>> T addAll(T resource) {
        int i = 0;
        for (Command c : resource) {
            add(i++, c);
        }
        return resource;
    }

    default void removeAll(VersionedResource<? extends Command> resource) {
        for (Command c : resource) {
            remove(c);
        }
    }

}
