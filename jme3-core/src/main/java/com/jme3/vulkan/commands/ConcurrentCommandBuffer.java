package com.jme3.vulkan.commands;

import com.jme3.vulkan.buffer.BufferTracker;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentCommandBuffer implements CommandBuffer {

    private final CommandAllocator allocator;
    private final Map<Thread, CommandBuffer> buffers = new ConcurrentHashMap<>();
    private final Deque<CommandBuffer> submissions = new ArrayDeque<>();

    public ConcurrentCommandBuffer(CommandAllocator allocator) {
        this.allocator = allocator;
    }

    protected CommandBuffer getCurrent() {
        return buffers.computeIfAbsent(Thread.currentThread(), t -> allocator.allocate(Level.Primary));
    }

    @Override
    public OpLocation cmdCopy(EngineBuffer src, int srcOffset, EngineBuffer dst, int dstOffset, BufferTracker regions, OpLocation location) {
        return getCurrent().cmdCopy(src, srcOffset, dst, dstOffset, regions, location);
    }

    @Override
    public OpLocation cmdFlatCopy(EngineBuffer src, int srcOffset, EngineBuffer dst, int dstOffset, BufferTracker regions, boolean flatten, OpLocation location) {
        return getCurrent().cmdFlatCopy(src, srcOffset, dst, dstOffset, regions, flatten, location);
    }

    @Override
    public void cmdStreamToRemote(ByteBuffer src, EngineBuffer dst, BufferTracker regions) {
        getCurrent().cmdStreamToRemote(src, dst, regions);
    }

    @Override
    public void cmdStreamFromRemote(EngineBuffer src, ByteBuffer dst, Runnable callback) {
        getCurrent().cmdStreamFromRemote(src, dst, callback);
    }

    @Override
    public void beginRecording() {
        getCurrent().beginRecording();
    }

    @Override
    public void endRecording() {
        getCurrent().endRecording();
    }

    @Override
    public void addResource(Commandable resource) {
        getCurrent().addResource(resource);
    }

    @Override
    public void addListener(CommandCycleListener listener) {
        getCurrent().addListener(listener);
    }

    @Override
    public void submit(Fence fence) {
        CommandBuffer cur = getCurrent();
        synchronized (submissions) {
            CommandBuffer prev = submissions.peek();
            if (prev != null) {
                cur.await(prev.getCompletionSignal());
            }
            submissions.push(cur);
        }
        cur.submit(fence);
    }

    @Override
    public void reset() {
        getCurrent().reset();
    }

    @Override
    public void signal(Semaphore s) {
        getCurrent().signal(s);
    }

    @Override
    public void await(Semaphore s, Flag<PipelineStage> stageMask) {
        getCurrent().await(s, stageMask);
    }

    @Override
    public TimelineSemaphore getCompletionSignal() {
        return null;
    }

    @Override
    public boolean isRecording() {
        return getCurrent().isRecording();
    }

    @Override
    public boolean isExecuting() {
        return getCurrent().isExecuting();
    }

}
