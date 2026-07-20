package com.jme3.vulkan.commands;

import com.jme3.vulkan.buffer.BufferTracker;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.buffer.RangeBufferTracker;
import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

import java.nio.*;

import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_SECONDARY;

public interface CommandBuffer {

    enum Level implements IntEnum<Level> {

        Primary(VK_COMMAND_BUFFER_LEVEL_PRIMARY),
        Secondary(VK_COMMAND_BUFFER_LEVEL_SECONDARY);

        private final int vk;

        Level(int vk) {
            this.vk = vk;
        }

        @Override
        public int getEnum() {
            return vk;
        }

    }

    default OpLocation cmdCopy(EngineBuffer src, int srcOffset, EngineBuffer dst, int dstOffset, int size, OpLocation location) {
        if (srcOffset + size > src.capacity() || dstOffset + size > dst.capacity()) {
            throw new BufferOverflowException();
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive.");
        }
        return cmdCopy(src, srcOffset, dst, dstOffset, new RangeBufferTracker(0, size), location);
    }

    OpLocation cmdCopy(EngineBuffer src, int srcOffset, EngineBuffer dst, int dstOffset, BufferTracker regions, OpLocation location);

    OpLocation cmdFlatCopy(EngineBuffer src, int srcOffset, EngineBuffer dst, int dstOffset, BufferTracker regions, boolean flatten, OpLocation location);

    void cmdStreamToRemote(ByteBuffer src, EngineBuffer dst, BufferTracker regions);

    default void cmdStreamToRemote(DataBuffer src, EngineBuffer dst) {
        cmdStreamToRemote(src.getBytes(), dst, src.getTracker());
    }

    void cmdStreamFromRemote(EngineBuffer src, ByteBuffer dst, Runnable callback);

    default void cmdStreamFromRemote(EngineBuffer src, ByteBuffer dst) {
        cmdStreamFromRemote(src, dst, null);
    }

    void beginRecording();

    void endRecording();

    void addResource(Commandable resource);

    void addListener(CommandCycleListener listener);

    void submit(Fence fence);

    default void submit() {
        submit(null);
    }

    void reset();

    void signal(Semaphore s);

    default TimelineSemaphore.SignalEvent signalEvent(TimelineSemaphore s) {
        s.incrementTargetPayload();
        signal(s);
        return s.createEvent();
    }

    void await(Semaphore s, Flag<PipelineStage> stageMask);

    default void await(Semaphore s) {
        await(s, PipelineStage.TopOfPipe);
    }

    TimelineSemaphore getCompletionSignal();

    boolean isRecording();

    boolean isExecuting();

}