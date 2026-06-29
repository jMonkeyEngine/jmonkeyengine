package com.jme3.vulkan.commands;

import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.TimelineSemaphore;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.*;
import java.util.concurrent.*;

public class ImmediateCommandStream {

    private final CommandPool pool;
    private final int maxAlloc;
    private final Deque<CachedCmdBuffer> buffers = new LinkedList<>();
    private int numAllocs = 0;

    public ImmediateCommandStream(CommandPool pool) {
        this(pool, 64);
    }

    /**
     *
     * @param pool allocating command pool
     * @param maxAlloc maximum number of buffers that may be allocated (unbounded if not positive)
     */
    public ImmediateCommandStream(CommandPool pool, int maxAlloc) {
        assert pool.getFlags().contains(CommandPool.Create.ResetCommandBuffer) : "Command pool must allow buffers to be reset.";
        this.pool = pool;
        this.maxAlloc = maxAlloc;
    }

    /**
     * Acquires an available {@link CommandBuffer}. If no existing buffer is available,
     * either a new buffer is created or this method blocks until one becomes available.
     * This method automatically {@link CommandBuffer#reset() resets} the returned command
     * buffer.
     *
     * @return command buffer
     */
    public CommandBuffer acquire() {
        CachedCmdBuffer cmd = buffers.peek();
        if (cmd == null || (!cmd.event.signaled() && (maxAlloc <= 0 || numAllocs < maxAlloc))) {
            cmd = pool.allocate(CommandBuffer.Level.Primary, CachedCmdBuffer::new);
            numAllocs++;
        } else {
            (cmd = buffers.poll()).event.awaitSignal(TimeUnit.SECONDS.toMillis(5));
        }
        cmd.reset();
        return cmd;
    }

    public int getNumAllocatedBuffers() {
        return numAllocs;
    }

    public int getMaxAllocatedBuffers() {
        return maxAlloc;
    }

    private class CachedCmdBuffer extends CommandBuffer {

        private final TimelineSemaphore signal;
        private TimelineSemaphore.SignalEvent event;

        public CachedCmdBuffer(CommandPool pool, VkCommandBuffer handle) {
            super(pool, handle);
            this.signal = new TimelineSemaphore(pool.getDevice(), 0);
        }

        @Override
        public void submit(Fence fence) {
            event = signalEvent(signal);
            if (!buffers.isEmpty()) {
                await(buffers.peekLast().signal, PipelineStage.AllCommands);
            }
            buffers.add(this);
            super.submit(fence);
        }

    }

}
