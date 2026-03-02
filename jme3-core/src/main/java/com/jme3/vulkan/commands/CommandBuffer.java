package com.jme3.vulkan.commands;

import com.jme3.renderer.ScissorArea;
import com.jme3.renderer.ViewPortArea;
import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK14.*;

public class CommandBuffer {

    public enum Level implements IntEnum<Level> {

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

    private final CommandPool pool;
    private final VkCommandBuffer buffer;
    private final Collection<Sync> signals = new ArrayList<>();
    private final Collection<WaitSync> waits = new ArrayList<>();
    private final Collection<Runnable> completionListeners = new ArrayList<>();
    private final Queue<VulkanBuffer> stagedUploads = new LinkedList<>();
    private TimelineSemaphore completionSemaphore;
    private boolean recording = false;

    protected CommandBuffer(CommandPool pool, VkCommandBuffer buffer) {
        this.pool = pool;
        this.buffer = buffer;
    }

    public void stageBufferUpload(VulkanBuffer buffer) {
        stagedUploads.add(buffer);
    }

    public void uploadBuffers(BufferStream stream) {
        for (VulkanBuffer buf; (buf = stagedUploads.poll()) != null;) {
            buf.upload(this, stream);
        }
    }

    /**
     * Begins recording commands to this command buffer.
     *
     * @see #endRecording()
     * @throws IllegalStateException if already recording
     */
    public void beginRecording() {
        if (recording) {
            throw new IllegalStateException("Command buffer already recording.");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo begin = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            check(vkBeginCommandBuffer(buffer, begin), "Failed to begin command buffer");
            recording = true;
        }
    }

    /**
     * Ends recording commands to this command buffer.
     *
     * @see #beginRecording()
     * @throws IllegalStateException if not recording
     */
    public void endRecording() {
        if (!recording) {
            throw new IllegalStateException("Command buffer has not begun recording.");
        }
        check(vkEndCommandBuffer(buffer), "Failed to end command buffer");
        recording = false;
    }

    /**
     * Submits this command buffer to the allocating queue for execution.
     * The command buffer must not be recording when submitted. No fence
     * is submitted by this method.
     *
     * @see #submit(Fence)
     */
    public void submit() {
        submit(null);
    }

    /**
     * Submits this command buffer to the allocating queue for execution.
     * The command buffer must not be recording when submitted.
     *
     * @param fence fence to submit with that can be used on the host to await
     *              execution completion (or null to not submit with a fence)
     * @throws IllegalStateException if recording
     */
    public void submit(Fence fence) {
        if (recording) {
            throw new IllegalStateException("Command buffer is still recording.");
        }
        if (!completionListeners.isEmpty()) {
            if (completionSemaphore == null) {
                completionSemaphore = new TimelineSemaphore(pool.getDevice());
            }
            TimelineSemaphore.SignalEvent completionEvent = signalEvent(completionSemaphore);
            Collection<Runnable> listeners = new ArrayList<>(completionListeners);
            pool.getQueue().getAsyncExecutor().execute(() -> {
                completionEvent.awaitSignal(5000);
                for (Runnable r : listeners) r.run();
            });
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkTimelineSemaphoreSubmitInfo timelineSubmit = VkTimelineSemaphoreSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_TIMELINE_SEMAPHORE_SUBMIT_INFO);
            VkSubmitInfo.Buffer submit = VkSubmitInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(buffer))
                    .pNext(timelineSubmit);
            if (!signals.isEmpty()) {
                LongBuffer signalBuf = stack.mallocLong(signals.size());
                LongBuffer payloadBuf = stack.mallocLong(signals.size());
                for (Sync s : signals) {
                    signalBuf.put(s.semaphore.getSemaphoreObject());
                    payloadBuf.put(s.payload);
                }
                submit.pSignalSemaphores(signalBuf.flip());
                timelineSubmit.pSignalSemaphoreValues(payloadBuf.flip());
            }
            if (!waits.isEmpty()) {
                LongBuffer waitBuf = stack.mallocLong(waits.size());
                IntBuffer dstStages = stack.mallocInt(waits.size());
                LongBuffer payloadBuf = stack.mallocLong(waits.size());
                for (WaitSync w : waits) {
                    waitBuf.put(w.semaphore.getSemaphoreObject());
                    dstStages.put(w.stageMask.bits());
                    payloadBuf.put(w.payload);
                }
                submit.waitSemaphoreCount(waits.size())
                        .pWaitSemaphores(waitBuf.flip())
                        .pWaitDstStageMask(dstStages.flip());
                timelineSubmit.pWaitSemaphoreValues(payloadBuf.flip());
            }
            pool.getQueue().submit(submit, fence);
        }
    }

    /**
     * Resets this command buffer so that it contains no previous commands
     * and may be reused with new commands. All The allocating {@link CommandPool}
     * must be created with the {@link CommandPool.Create#ResetCommandBuffer
     * ResetCommandBuffer} flag.
     *
     * @throws IllegalStateException if recording
     * @throws UnsupportedOperationException if the allocating command pool does
     * not support command buffer reseting
     */
    public void reset() {
        if (recording) {
            throw new IllegalStateException("Command buffer cannot be reset while recording.");
        }
        if (!pool.getFlags().contains(CommandPool.Create.ResetCommandBuffer)) {
            throw new UnsupportedOperationException("Resetting is not supported by the allocating pool.");
        }
        vkResetCommandBuffer(buffer, 0);
        signals.clear();
        waits.clear();
        completionListeners.clear();
    }

    /**
     * Registers a semaphore to be signaled when the command buffer finishes
     * execution. All registered signal semaphores are removed when this
     * command buffer is {@link #reset()}.
     *
     * @param s semaphore to signal
     */
    public void signal(Semaphore s) {
        signals.add(new Sync(s));
    }

    /**
     * Registers a {@link TimelineSemaphore} to be signaled when the command
     * buffer finishes execution. The {@link TimelineSemaphore#getTargetPayload()
     * target payload} is automatically incremented and a {@link TimelineSemaphore.SignalEvent
     * signal event} is created. All registered signal semaphores are removed
     * when this command buffer is {@link #reset()}.
     *
     * @param s timeline semaphore to signal
     * @return signal event that listens for the semaphore being signaled by
     * this command buffer.
     */
    public TimelineSemaphore.SignalEvent signalEvent(TimelineSemaphore s) {
        s.incrementTargetPayload();
        signals.add(new Sync(s));
        return s.createEvent();
    }

    /**
     * Registers a semaphore that blocks the command buffer from executing
     * until the semaphore is signaled. All registered wait semaphores are
     * removed when this command buffer is {@link #reset()}.
     *
     * @param s blocking semaphore
     * @param stageMask specifies the stages the wait operation for this
     *                  semaphore occurs
     */
    public void await(Semaphore s, Flag<PipelineStage> stageMask) {
        waits.add(new WaitSync(s, stageMask));
    }

    /**
     * Registers a semaphore that blocks the command buffer from executing
     * until the semaphore is signaled. The semaphore blocks at the
     * {@link PipelineStage#TopOfPipe} pipeline stage.
     *
     * @param s blocking semaphore
     * @see #await(Semaphore, Flag)
     */
    public void await(Semaphore s) {
        await(s, PipelineStage.TopOfPipe);
    }

    /**
     * Registers a {@link Runnable} listener that is triggered when this
     * command buffer finishes execution. All registered listeners are
     * removed when this command buffer is {@link #reset()}.
     *
     * @param listener listener to register
     */
    public void onExecutionComplete(Runnable listener) {
        completionListeners.add(listener);
    }

    /**
     * Blocks until this command buffer's {@link CommandQueue} is {@link CommandQueue#waitIdle() idle}.
     * It is recommended to use {@link Fence fences} or {@link TimelineSemaphore timeline
     * semaphores} instead, as this method is usually very inefficient.
     */
    public void queueWaitIdle() {
        pool.getQueue().waitIdle();
    }

    /**
     * Gets the {@link CommandPool} that created this command buffer.
     *
     * @return command pool
     */
    public CommandPool getPool() {
        return pool;
    }

    /**
     * Gets the native {@link VkCommandBuffer} handle.
     *
     * @return command buffer handle
     */
    public VkCommandBuffer getBuffer() {
        return buffer;
    }

    /**
     * Returns true if this command buffer is {@link #beginRecording() recording}.
     *
     * @return true if recording
     */
    public boolean isRecording() {
        return recording;
    }

    private static class Sync {

        public final Semaphore semaphore;
        public final long payload; // store the current payload in case it changes

        public Sync(Semaphore semaphore) {
            this.semaphore = semaphore;
            this.payload = semaphore.getTargetPayload();
        }

    }

    private static class WaitSync extends Sync {

        public final Flag<PipelineStage> stageMask;

        public WaitSync(Semaphore semaphore, Flag<PipelineStage> stageMask) {
            super(semaphore);
            this.stageMask = stageMask;
        }

    }

}
