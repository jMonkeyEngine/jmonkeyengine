package com.jme3.vulkan.commands;

import com.jme3.vulkan.sync.Fence;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class DataCommands {

    private final CommandPool pool;
    private final Map<Thread, Bucket> buffers = new ConcurrentHashMap<>();
    private int bucketSize = 32;

    public CommandBuffer acquire() {
        return buffers.computeIfAbsent(Thread.currentThread(), t -> {
            synchronized (pool) {
                return pool.allocate(CommandBuffer.Level.Primary);
            }
        });
    }

    private class Bucket {

        private final Queue<CommandBuffer> buffers = new LinkedList<>();
        private int allocBuffers = 0;

        public CommandBuffer acquire() {
            CommandBuffer cmd = buffers.poll();
            if (cmd == null || (cmd.isExecuting() && allocBuffers < bucketSize)) {
                cmd = pool.allocate(CommandBuffer.Level.Primary, (p, b) -> new CmdBuffer(p, b, this));
            }
        }

    }

    private static class CmdBuffer extends CommandBuffer {

        private final Bucket bucket;

        public CmdBuffer(CommandPool pool, VkCommandBuffer buffer, Bucket bucket) {
            super(pool, buffer);
            this.bucket = bucket;
        }

        @Override
        public void submit(Fence fence) {
            super.submit(fence);
        }

    }

}
