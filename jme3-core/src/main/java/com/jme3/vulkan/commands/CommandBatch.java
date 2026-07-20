package com.jme3.vulkan.commands;

import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.HashMap;
import java.util.Map;

public class CommandBatch {

    private final CommandPool pool;
    private final Map<Thread, CommandBuffer> buffers = new HashMap<>();

    public CommandBatch(CommandQueue queue) {
        this.pool = new CommandPool(queue);
    }

    public CommandBuffer allocate() {

    }

    private static class ThreadLocalBuffer extends CommandBuffer {

        public ThreadLocalBuffer(CommandPool pool, VkCommandBuffer buffer) {
            super(pool, buffer);
        }

    }

}
