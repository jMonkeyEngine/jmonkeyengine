package com.jme3.vulkan.commands;

public interface CommandAllocator {

    CommandBuffer[] allocate(CommandBuffer.Level level, int count);

    default CommandBuffer allocate(CommandBuffer.Level level) {
        return allocate(level, 1)[0];
    }

}
