package com.jme3.vulkan.commands;

import com.jme3.vulkan.buffer.BufferCopy;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.buffer.tracking.BufferTracker;
import com.jme3.vulkan.images.BufferImageCopy;
import com.jme3.vulkan.images.ImageCopy;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

import java.nio.ByteBuffer;

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

    OpLocation cmdCopy(EngineBuffer src, EngineBuffer dst, BufferCopy copy, OpLocation location);

    void cmdStreamToRemote(ByteBuffer src, EngineBuffer dst, BufferTracker regions);

    default void cmdStreamToRemote(DataBuffer src, EngineBuffer dst) {
        cmdStreamToRemote(src.getBytes(), dst, src.getTracker());
    }

    void cmdStreamFromRemote(EngineBuffer src, ByteBuffer dst, Runnable callback);

    default void cmdStreamFromRemote(EngineBuffer src, ByteBuffer dst) {
        cmdStreamFromRemote(src, dst, null);
    }

    void cmdTransitionLayout(EngineImage image, EngineImage.Layout srcLayout, EngineImage.Layout dstLayout);

    void cmdCopy(EngineImage src, EngineImage dst, ImageCopy copy);

    void cmdCopy(EngineBuffer src, EngineImage dst, BufferImageCopy copy);

    void cmdCopy(EngineImage src, EngineBuffer dst, BufferImageCopy copy);

    void cmdResolveMultisampled(EngineImage src, EngineImage dst, ImageCopy copy);

    void beginRecording();

    void endRecording();

    void addResource(Object resource);

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