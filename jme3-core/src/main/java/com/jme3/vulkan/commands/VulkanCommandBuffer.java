package com.jme3.vulkan.commands;

import com.jme3.vulkan.buffer.*;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.*;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK14.*;

public class VulkanCommandBuffer implements CommandBuffer {

    private final CommandPool pool;
    private final VkCommandBuffer buffer;
    private final Collection<Sync> signals = new ArrayList<>();
    private final Collection<WaitSync> waits = new ArrayList<>();
    private final Collection<Commandable> resourcesInUse = new ArrayList<>();
    private final Collection<CommandCycleListener> cycleListeners = new ArrayList<>();
    private final TimelineSemaphore completionSignal;
    private final Executor listenerThread = Executors.newFixedThreadPool(1);
    private TimelineSemaphore.SignalEvent completionEvent;
    private BufferStream stream;
    private boolean recording = false;
    private boolean executing = false;

    public VulkanCommandBuffer(CommandPool pool, VkCommandBuffer buffer) {
        this.pool = pool;
        this.buffer = buffer;
        this.completionSignal = new TimelineSemaphore(pool.getDevice());
    }

    /*-----------------*\
    | GRAPHICS COMMANDS |
    \*-----------------*/

    private boolean isPerformHostBufferCopy(EngineBuffer src, EngineBuffer dst, OpLocation location) {
        boolean hostAble = src.isHostAccessible() && dst.isHostAccessible();
        boolean deviceAble = src.isDeviceAccessible() && dst.isDeviceAccessible();
        if (!hostAble && !deviceAble) {
            throw new IllegalArgumentException("Buffers are not accessible from any one location.");
        }
        boolean hostOp = hostAble && (!deviceAble || location.isHostBiased() || (location == OpLocation.DontCare
                && src.getMemoryProperties().contains(MemoryProp.HostCached)
                && !dst.getMemoryProperties().contains(MemoryProp.HostCached)));
        if (hostOp && location == OpLocation.Device) {
            throw new IllegalArgumentException("Unable to perform operation on device.");
        }
        if (!hostOp && location == OpLocation.Host) {
            throw new IllegalArgumentException("Unable to perform operation on host.");
        }
        return hostOp;
    }

    @Override
    public OpLocation cmdCopy(EngineBuffer src, int srcOffset, EngineBuffer dst, int dstOffset, BufferTracker regions, OpLocation location) {
        if (srcOffset < 0 || dstOffset < 0) {
            throw new BufferUnderflowException();
        }
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("No regions to copy.");
        }
        srcOffset += src.getInternalOffset();
        dstOffset += dst.getInternalOffset();
        if (isPerformHostBufferCopy(src, dst, location)) {
            DataBuffer srcCache = src.cache();
            DataBuffer dstCache = dst.cache();
            for (BufferTracker.Island i : regions) {
                srcCache.range(srcOffset + i.getStart(), i.getEnd());
                dstCache.range(dstOffset + i.getStart(), i.getEnd());
                srcCache.copyTo(dstCache);
            }
            return OpLocation.Host;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCopy.Buffer copy = VkBufferCopy.malloc(regions.getNumIslands(), stack);
            for (BufferTracker.Island i : regions) {
                copy.get().set(i.getStart() + srcOffset, i.getStart() + dstOffset, i.getSize());
            }
            vkCmdCopyBuffer(buffer, src.getHandle(), dst.getHandle(), copy.flip());
        }
        resourcesInUse.add(src);
        resourcesInUse.add(dst);
        return OpLocation.Device;
    }

    @Override
    public OpLocation cmdFlatCopy(EngineBuffer src, int srcOffset, EngineBuffer dst, int dstOffset, BufferTracker tracker, boolean flatten, OpLocation location) {
        if (tracker.isEmpty()) {
            throw new IllegalArgumentException("No regions to copy.");
        }
        srcOffset += src.getInternalOffset();
        dstOffset += dst.getInternalOffset();
        if (isPerformHostBufferCopy(src, dst, location)) {
            DataBuffer srcCache = src.cache();
            DataBuffer dstCache = dst.cache();
            int flatOffset = 0;
            for (BufferTracker.Island i : tracker) {
                srcCache.region((flatten ? i.getStart() : flatOffset) + srcOffset, i.getSize());
                dstCache.region((flatten ? flatOffset : i.getStart()) + dstOffset, i.getSize());
                srcCache.copyTo(dstCache);
                flatOffset += i.getSize();
            }
            return OpLocation.Host;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCopy.Buffer copy = VkBufferCopy.malloc(tracker.getNumIslands(), stack);
            int flatOffset = 0;
            for (BufferTracker.Island i : tracker) {
                copy.get().set((flatten ? i.getStart() : flatOffset) + srcOffset,
                        (flatten ? flatOffset : i.getStart()) + dstOffset, i.getSize());
                flatOffset += i.getSize();
            }
            vkCmdCopyBuffer(buffer, src.getHandle(), dst.getHandle(), copy.flip());
        }
        resourcesInUse.add(src);
        resourcesInUse.add(dst);
        return OpLocation.Device;
    }

    @Override
    public void cmdStreamToRemote(ByteBuffer src, EngineBuffer dst, BufferTracker regions) {
        stream.streamToRemote(this, src, dst, regions);
    }

    @Override
    public void cmdStreamFromRemote(EngineBuffer src, ByteBuffer dst, Runnable callback) {
        stream.streamFromRemote(this, src, dst, callback);
    }

    /*-------------------------*\
    | COMMAND BUFFER MANAGEMENT |
    \*-------------------------*/

    @Override
    public void beginRecording() {
        if (executing) {
            throw new IllegalStateException("Buffer is executing.");
        }
        if (recording) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo begin = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            check(vkBeginCommandBuffer(buffer, begin), "Failed to begin command buffer");
            recording = true;
        }
    }

    @Override
    public void endRecording() {
        if (executing) {
            throw new IllegalStateException("Buffer is executing.");
        }
        if (!recording) {
            return;
        }
        check(vkEndCommandBuffer(buffer), "Failed to end command buffer");
        recording = false;
    }

    @Override
    public void addResource(Commandable resource) {
        if (executing) {
            throw new IllegalStateException("Buffer is executing.");
        }
        resource.acquireControl();
        resourcesInUse.add(resource);
    }

    @Override
    public void addListener(CommandCycleListener listener) {
        if (executing) {
            throw new IllegalStateException("Buffer is executing.");
        }
        cycleListeners.add(listener);
    }

    @Override
    public void submit() {
        submit(null);
    }

    @Override
    public void submit(Fence fence) {
        if (executing) {
            throw new IllegalStateException("Buffer is executing.");
        }
        if (recording) {
            throw new IllegalStateException("Buffer is recording.");
        }
        cycleListeners.forEach(CommandCycleListener::onCmdSubmit);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo.Buffer submit = VkSubmitInfo.calloc(1, stack);
            populateSubmitInfo(stack, submit.get());
            pool.getQueue().submit(submit.flip(), fence);
            executing = true;
            completionEvent = completionSignal.createEvent();
            listenerThread.execute(this::listenForCompletion);
        }
    }

    protected VkSubmitInfo populateSubmitInfo(MemoryStack stack, VkSubmitInfo info) {
        VkTimelineSemaphoreSubmitInfo timelineSubmit = VkTimelineSemaphoreSubmitInfo.calloc(stack).sType$Default();
        info.sType$Default().pCommandBuffers(stack.pointers(buffer)).pNext(timelineSubmit);
        signal(completionSignal.incrementTargetPayload());
        if (!signals.isEmpty()) { // technically always true
            LongBuffer signalBuf = stack.mallocLong(signals.size());
            LongBuffer payloadBuf = stack.mallocLong(signals.size());
            for (Sync s : signals) {
                signalBuf.put(s.semaphore.getSemaphoreObject());
                payloadBuf.put(s.payload);
            }
            info.pSignalSemaphores(signalBuf.flip());
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
            info.waitSemaphoreCount(waits.size())
                    .pWaitSemaphores(waitBuf.flip())
                    .pWaitDstStageMask(dstStages.flip());
            timelineSubmit.pWaitSemaphoreValues(payloadBuf.flip());
        }
        return info;
    }

    protected void listenForCompletion() {
        completionEvent.awaitSignal(TimeUnit.SECONDS.toMillis(5));
        cycleListeners.forEach(CommandCycleListener::onCmdComplete);
        cycleListeners.clear();
        resourcesInUse.forEach(Commandable::releaseControl);
        resourcesInUse.clear();
        signals.clear();
        waits.clear();
        executing = false;
    }

    @Override
    public void reset() {
        if (executing) {
            throw new IllegalStateException("Buffer is executing.");
        }
        if (recording) {
            throw new IllegalStateException("Buffer is recording.");
        }
        if (!pool.getFlags().contains(CommandPool.Create.ResetCommandBuffer)) {
            throw new UnsupportedOperationException("Reseting is not supported by the allocating pool.");
        }
        vkResetCommandBuffer(buffer, 0);
    }

    @Override
    public void signal(Semaphore s) {
        signals.add(new Sync(s));
    }

    @Override
    public TimelineSemaphore.SignalEvent signalEvent(TimelineSemaphore s) {
        s.incrementTargetPayload();
        signals.add(new Sync(s));
        return s.createEvent();
    }

    @Override
    public void await(Semaphore s, Flag<PipelineStage> stageMask) {
        waits.add(new WaitSync(s, stageMask));
    }

    @Override
    public void await(Semaphore s) {
        await(s, PipelineStage.TopOfPipe);
    }

    /**
     * Blocks until this command buffer's {@link CommandQueue} is {@link CommandQueue#waitIdle() idle}.
     * It is recommended to use {@link Fence fences} or {@link TimelineSemaphore timeline
     * semaphores} instead, as this method is usually results in pipeline stalls.
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

    @Override
    public TimelineSemaphore getCompletionSignal() {
        return completionSignal;
    }

    @Override
    public boolean isRecording() {
        return recording;
    }

    @Override
    public boolean isExecuting() {
        return executing;
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

