package com.jme3.vulkan.commands;

import com.jme3.util.destruction.ResourceLifecycle;
import com.jme3.vulkan.buffer.BufferCopy;
import com.jme3.vulkan.buffer.GpuBuffer;
import com.jme3.vulkan.concurrent.Access;
import com.jme3.vulkan.concurrent.CommandDataAccess;
import com.jme3.vulkan.concurrent.DeviceData;
import com.jme3.vulkan.images.BufferImageCopy;
import com.jme3.vulkan.images.ImageCopy;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_SECONDARY;

/**
 * Records and submits a sequence of GPU commands.
 * <p>
 * A command buffer moves through a simple lifecycle: {@link #beginRecording()} opens it
 * for recording {@code cmd*} commands, {@link #endRecording()} closes it,
 * {@link #submit()} sends it to the device, and once the device finishes executing it,
 * it becomes eligible to be {@link #reset()} and recorded again. {@link #isRecording()}
 * and {@link #isExecuting()} reflect where in this lifecycle the buffer currently is.
 * <p>
 * {@code cmd*} methods that take a {@link GpuBuffer} register it with {@link #useResource}
 * so that its use by this command buffer is ordered against its other uses; {@link
 * EngineImage} parameters are not, since {@link EngineImage} does not yet implement
 * {@link DeviceData}.
 */
public interface CommandBuffer {

    /**
     * Level at which a command buffer is allocated.
     */
    enum Level implements IntEnum<Level> {

        /**
         * Submitted directly to a {@link CommandQueue}.
         */
        Primary(VK_COMMAND_BUFFER_LEVEL_PRIMARY),

        /**
         * Only executable from within a {@link #Primary} command buffer.
         */
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

    /**
     * Copies {@code copy}'s regions from {@code src} to {@code dst}, performed either as
     * a direct host memory copy or as a recorded device copy command, depending on
     * {@code location} and whether the buffers are host- or device-accessible.
     * <p>
     * If performed on the device, both buffers are registered with {@link #useResource}
     * ({@code src} as {@link Access#Read}, {@code dst} as {@link Access#Write}). If
     * performed as a direct host memory copy, no such registration is made, so callers
     * must otherwise ensure no device operation concurrently accesses the same memory.
     *
     * @param src buffer to copy from
     * @param dst buffer to copy to
     * @param copy regions to copy
     * @param location where the copy should, or must, be performed; see {@link OpLocation}
     * @return the location the copy was actually performed at
     */
    OpLocation cmdCopy(GpuBuffer src, GpuBuffer dst, BufferCopy copy, OpLocation location);

    /**
     * Records a pipeline barrier that transitions {@code image} from {@code srcLayout} to
     * {@code dstLayout}. Does nothing if the two layouts are the same.
     *
     * @param image image to transition
     * @param srcLayout layout {@code image} is currently in
     * @param dstLayout layout to transition {@code image} into
     */
    void cmdTransitionLayout(EngineImage image, EngineImage.Layout srcLayout, EngineImage.Layout dstLayout);

    /**
     * Records a device copy of {@code copy}'s regions from image {@code src} to image
     * {@code dst}.
     *
     * @param src image to copy from
     * @param dst image to copy to
     * @param copy regions to copy
     */
    void cmdCopy(EngineImage src, EngineImage dst, ImageCopy copy);

    /**
     * Records a device copy of {@code copy}'s regions from buffer {@code src} into image
     * {@code dst}. {@code src} is registered with {@link #useResource} as
     * {@link Access#Read}.
     *
     * @param src buffer to copy from
     * @param dst image to copy to
     * @param copy regions to copy
     */
    void cmdCopy(GpuBuffer src, EngineImage dst, BufferImageCopy copy);

    /**
     * Records a device copy of {@code copy}'s regions from image {@code src} into buffer
     * {@code dst}. {@code dst} is registered with {@link #useResource} as
     * {@link Access#Write}.
     *
     * @param src image to copy from
     * @param dst buffer to copy to
     * @param copy regions to copy
     */
    void cmdCopy(EngineImage src, GpuBuffer dst, BufferImageCopy copy);

    /**
     * Records a device command that resolves multisampled image {@code src} into
     * single-sampled image {@code dst}.
     *
     * @param src multisampled image to resolve from
     * @param dst image to resolve into
     * @param copy the single region to resolve; must contain exactly one region
     * @throws IllegalArgumentException if {@code copy} does not contain exactly one region
     */
    void cmdResolveMultisampled(EngineImage src, EngineImage dst, ImageCopy copy);

    /**
     * Opens this command buffer for recording. Subsequent {@code cmd*} calls are recorded
     * into it until {@link #endRecording()} is called. Does nothing if already recording.
     *
     * @throws IllegalStateException if this command buffer is currently executing
     */
    void beginRecording();

    /**
     * Closes this command buffer to further recording, after which it may be
     * {@link #submit() submitted}. Does nothing if not currently recording.
     *
     * @throws IllegalStateException if this command buffer is currently executing
     */
    void endRecording();

    /**
     * Registers {@code listener} to be notified when this command buffer is submitted and
     * when it completes on the device.
     *
     * @param listener listener to notify
     */
    void addListener(CommandCycleListener listener);

    /**
     * Registers {@code lifecycle} as about to be accessed by this command buffer with the
     * given {@code access}. Inserts a wait on whatever earlier access it would race with
     * (per {@link ResourceLifecycle}'s read/write rules), and arranges for this command
     * buffer to become the new most recent access once it has actually been submitted, so
     * callers never need to call {@link ResourceLifecycle#awaitPreviousAccesses} or
     * {@link ResourceLifecycle#recordDeviceAccess} themselves.
     * <p>
     * Must be called while recording, before {@link #submit()}.
     *
     * @param lifecycle tracks the resource's accesses and destruction
     * @param access the kind of access this command buffer will perform
     * @param stageMask pipeline stage(s) of this command buffer that perform this access
     */
    void useResource(ResourceLifecycle lifecycle, Access access, Flag<PipelineStage> stageMask);

    /**
     * Submits this command buffer to the device for execution, signaling {@code fence}
     * once it completes.
     *
     * @param fence fence to signal on completion, or null to signal none
     * @throws IllegalStateException if this command buffer is currently executing or recording
     */
    void submit(Fence fence);

    /**
     * Equivalent to {@link #submit(Fence) submit(null)}.
     */
    default void submit() {
        submit(null);
    }

    /**
     * Resets this command buffer so it can be recorded again, discarding anything
     * previously recorded into it.
     *
     * @throws IllegalStateException if this command buffer is currently executing or recording
     * @throws UnsupportedOperationException if the allocating pool does not support
     * resetting individual command buffers
     */
    void reset();

    /**
     * Arranges for {@code s} to be signaled once this command buffer completes on the
     * device. Must be called before {@link #submit()}.
     *
     * @param s semaphore to signal
     */
    void signal(Semaphore s);

    /**
     * Convenience for {@link #signal(Semaphore)} on a {@link TimelineSemaphore}: increments
     * {@code s}'s target payload, signals it, and returns an event for that payload value,
     * which will be reached once this command buffer completes.
     *
     * @param s timeline semaphore to signal
     * @return event signaled once this command buffer completes
     */
    default TimelineSemaphore.SignalEvent signalEvent(TimelineSemaphore s) {
        s.incrementTargetPayload();
        signal(s);
        return s.createEvent();
    }

    /**
     * Arranges for this command buffer to wait on {@code s} before executing the pipeline
     * stage(s) in {@code stageMask}. Must be called before {@link #submit()}.
     *
     * @param s semaphore to wait on
     * @param stageMask pipeline stage(s) that must wait for {@code s}
     */
    void await(Semaphore s, Flag<PipelineStage> stageMask);

    /**
     * Equivalent to {@link #await(Semaphore, Flag) await(s, PipelineStage.TopOfPipe)},
     * waiting before any work in this command buffer begins.
     *
     * @param s semaphore to wait on
     */
    default void await(Semaphore s) {
        await(s, PipelineStage.TopOfPipe);
    }

    /**
     * Gets the {@link TimelineSemaphore} signaled when this command buffer completes on
     * the device. The same instance is reused across however many times this command
     * buffer is recorded, submitted, and reset; its target payload is incremented on
     * every {@link #submit()}.
     *
     * @return this command buffer's completion signal
     */
    TimelineSemaphore getCompletionSignal();

    /**
     * Returns true if this command buffer is currently open for recording, i.e.
     * {@link #beginRecording()} has been called without a matching {@link #endRecording()}.
     *
     * @return true if recording
     */
    boolean isRecording();

    /**
     * Returns true if this command buffer has been submitted and has not yet completed
     * on the device.
     *
     * @return true if executing
     */
    boolean isExecuting();

}