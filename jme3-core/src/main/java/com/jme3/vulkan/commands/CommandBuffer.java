package com.jme3.vulkan.commands;

import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.FutureTask;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK14.*;

public class CommandBuffer {

    protected final CommandPool pool;
    protected final VkCommandBuffer buffer;
    protected boolean recording = false;

    private final Collection<Sync> signals = new ArrayList<>();
    private final Collection<Sync> waits = new ArrayList<>();
    private final Collection<Runnable> executionComplete = new ArrayList<>();
    private TimelineSemaphore executionCompleteSemaphore;
    private long submitCount = 0;

    public CommandBuffer(CommandPool pool) {
        this.pool = pool;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocate = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(pool.getNativeObject())
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);
            PointerBuffer ptr = stack.mallocPointer(1);
            check(vkAllocateCommandBuffers(pool.getDevice().getNativeObject(), allocate, ptr),
                    "Failed to allocate command buffer");
            buffer = new VkCommandBuffer(ptr.get(0), pool.getDevice().getNativeObject());
        }
    }

    private CommandBuffer(CommandPool pool, VkCommandBuffer buffer) {
        this.pool = pool;
        this.buffer = buffer;
    }

    public void begin() {
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

    public void resetAndBegin() {
        reset();
        begin();
    }

    public void end() {
        if (!recording) {
            throw new IllegalStateException("Command buffer has not begun recording.");
        }
        check(vkEndCommandBuffer(buffer), "Failed to end command buffer");
        recording = false;
    }

    public void submit() {
        submit(null);
    }

    public void submit(Fence fence) {
        if (recording) {
            throw new IllegalStateException("Command buffer is still recording.");
        }
        if (!executionComplete.isEmpty()) {
            if (executionCompleteSemaphore == null) {
                executionCompleteSemaphore = new TimelineSemaphore(pool.getDevice(), 0L);
            }
            signal(executionCompleteSemaphore, ++submitCount);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo.Buffer submit = VkSubmitInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(buffer));
            VkTimelineSemaphoreSubmitInfo values = VkTimelineSemaphoreSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_TIMELINE_SEMAPHORE_SUBMIT_INFO);
            submit.pNext(values);
            if (!signals.isEmpty()) {
                LongBuffer signalBuf = stack.mallocLong(signals.size());
                LongBuffer valueBuf = stack.mallocLong(signals.size());
                for (Sync s : signals) {
                    signalBuf.put(s.semaphore.getNativeObject());
                    valueBuf.put(s.value);
                }
                submit.pSignalSemaphores(signalBuf.flip());
                values.pSignalSemaphoreValues(valueBuf.flip());
            }
            if (!waits.isEmpty()) {
                LongBuffer waitBuf = stack.mallocLong(waits.size());
                IntBuffer dstStages = stack.mallocInt(waits.size());
                LongBuffer valueBuf = stack.mallocLong(waits.size());
                for (Sync w : waits) {
                    waitBuf.put(w.semaphore.getNativeObject());
                    dstStages.put(w.dstStageMask.bits());
                    valueBuf.put(w.value);
                }
                submit.waitSemaphoreCount(waits.size())
                        .pWaitSemaphores(waitBuf.flip())
                        .pWaitDstStageMask(dstStages.flip());
                values.pWaitSemaphoreValues(valueBuf.flip());
            }
            pool.getQueue().submit(submit, fence);
            if (!executionComplete.isEmpty()) {
                Collection<Runnable> listeners = new ArrayList<>(executionComplete);
                pool.getQueue().getExecutor().execute(() -> {
                    executionCompleteSemaphore.block(submitCount, 5000);
                    for (Runnable r : listeners) {
                        r.run();
                    }
                });
            }
        }
    }

    public void endAndSubmit() {
        end();
        submit();
    }

    public void endAndSubmit(Fence fence) {
        end();
        submit(fence);
    }

    public void reset() {
        if (!pool.getFlags().contains(CommandPool.Create.ResetCommandBuffer)) {
            throw new UnsupportedOperationException("Resetting is not supported by the allocating pool.");
        }
        vkResetCommandBuffer(buffer, 0);
        signals.clear();
        waits.clear();
        executionComplete.clear();
    }

    public void signal(Semaphore s) {
        signals.add(new Sync(s, null));
    }

    public void signal(Semaphore s, long signalValue) {
        signals.add(new Sync(s, null, signalValue));
    }

    public void await(Semaphore s, Flag<PipelineStage> dstStageMask) {
        signals.add(new Sync(s, Objects.requireNonNull(dstStageMask, "Pipeline stage mask cannot be null.")));
    }

    public void await(Semaphore s, Flag<PipelineStage> dstStageMask, long waitValue) {
        signals.add(new Sync(s, Objects.requireNonNull(dstStageMask, "Pipeline stage mask cannot be null."), waitValue));
    }

    public void onExecutionComplete(Runnable listener) {
        executionComplete.add(listener);
    }

    public void queueWaitIdle() {
        pool.getQueue().waitIdle();
    }

    public CommandPool getPool() {
        return pool;
    }

    public VkCommandBuffer getBuffer() {
        return buffer;
    }

    public boolean isRecording() {
        return recording;
    }

    private static class Sync {

        public final Semaphore semaphore;
        public final Flag<PipelineStage> dstStageMask;
        public final long value;

        public Sync(Semaphore semaphore, Flag<PipelineStage> dstStageMask) {
            this(semaphore, dstStageMask, 0L);
        }

        public Sync(Semaphore semaphore, Flag<PipelineStage> dstStageMask, long value) {
            this.semaphore = semaphore;
            this.dstStageMask = dstStageMask;
            this.value = value;
        }

    }

}
