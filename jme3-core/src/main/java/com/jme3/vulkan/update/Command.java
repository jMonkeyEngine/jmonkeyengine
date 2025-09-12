package com.jme3.vulkan.update;

import com.jme3.vulkan.commands.CommandBuffer;

public interface Command {

    boolean run(CommandBuffer cmd, int frame);

}
