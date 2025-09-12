package com.jme3.vulkan.update;

public interface CommandBatch extends Command {

    <T extends Command> T add(T command);

    void remove(Command command);

}
