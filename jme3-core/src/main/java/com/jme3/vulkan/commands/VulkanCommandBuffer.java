package com.jme3.vulkan.commands;

import com.jme3.util.destruction.ResourceLifecycle;
import com.jme3.vulkan.buffer.BufferCopy;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;
import com.jme3.vulkan.concurrent.Access;
import com.jme3.vulkan.images.BufferImageCopy;
import com.jme3.vulkan.images.ImageCopy;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK14.*;

public class VulkanCommandBuffer implements CommandBuffer {

    private final CommandPool pool;
    private final VkCommandBuffer buffer;
    private final Collection<Sync> signals = new ArrayList<>();
    private final Collection<WaitSync> waits = new ArrayList<>();
    private final Collection<PendingAccess> pendingAccess = new ArrayList<>();
    private final Collection<CommandCycleListener> cycleListeners = new ArrayList<>();
    private final TimelineSemaphore completionSignal;
    private final Executor listenerThread = Executors.newFixedThreadPool(1);
    private TimelineSemaphore.SignalEvent completionEvent;
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

    public static boolean isPerformHostBufferCopy(GpuBuffer src, GpuBuffer dst, OpLocation location) {
        boolean hostAble = src.isHostAccessible() && dst.isHostAccessible();
        boolean hostOp = hostAble && (location.isHostBiased() || (location == OpLocation.DontCare
                && src.getMemoryProperties().contains(MemoryProp.HostCached)
                && !dst.getMemoryProperties().contains(MemoryProp.HostCached)));
        if (hostOp && location == OpLocation.Device) {
            throw new IllegalArgumentException("Unable to perform copy operation on device.");
        }
        if (!hostOp && location == OpLocation.Host) {
            throw new IllegalArgumentException("Unable to perform copy operation on host.");
        }
        return hostOp;
    }

    @Override
    public OpLocation cmdCopy(GpuBuffer src, GpuBuffer dst, BufferCopy copy, OpLocation location) {
        if (location == OpLocation.NoOp) {
            return location;
        }
        if (isPerformHostBufferCopy(src, dst, location)) {
            DataBuffer srcCache = src.cache().mark();
            DataBuffer dstCache = dst.cache().mark();
            for (BufferCopy.Region r : copy.getRegions()) {
                srcCache.reset().offset(r.getSrcOffset() + src.offset(), r.getSize());
                dstCache.reset().offset(r.getDstOffset() + dst.offset(), r.getSize());
                srcCache.copyTo(dstCache);
            }
            return OpLocation.Host;
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkBufferCopy.Buffer vkCopy = VkBufferCopy.calloc(copy.getRegions().size(), stack);
                for (BufferCopy.Region r : copy.getRegions()) {
                    vkCopy.get().set(r.getSrcOffset() + src.offset(), r.getDstOffset() + dst.offset(), r.getSize());
                }
                vkCmdCopyBuffer(buffer, src.getHandle(), dst.getHandle(), vkCopy.flip());
            }
            useResource(src.getLifecycle(), Access.Read, PipelineStage.Transfer);
            useResource(dst.getLifecycle(), Access.Write, PipelineStage.Transfer);
            return OpLocation.Device;
        }
    }

    @Override
    public void cmdTransitionLayout(EngineImage image, EngineImage.Layout srcLayout, EngineImage.Layout dstLayout) {
        if (srcLayout == dstLayout) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                .sType$Default()
                .oldLayout(srcLayout.getEnum())
                .newLayout(dstLayout.getEnum())
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(image.getHandle())
                .srcAccessMask(srcLayout.getAccessHint().bits())
                .dstAccessMask(dstLayout.getAccessHint().bits());
            barrier.subresourceRange()
                .baseMipLevel(0)
                .levelCount(image.getMipLevels())
                .baseArrayLayer(0)
                .layerCount(image.getArrayLayers())
                .aspectMask(image.getFormat().getAspects().getImageAspect().bits());
            vkCmdPipelineBarrier(buffer, srcLayout.getStageHint().bits(), dstLayout.getStageHint().bits(),
                    0, null, null, barrier);
        }
        addResource(image);
    }

    @Override
    public void cmdCopy(EngineImage src, EngineImage dst, ImageCopy copy) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCopy.Buffer imgCopy = VkImageCopy.calloc(copy.getRegions().size(), stack);
            for (ImageCopy.Region r : copy.getRegions()) {
                VkImageCopy c = imgCopy.get();
                c.srcOffset().set(r.getSrcOffset().x, r.getSrcOffset().y, r.getSrcOffset().z);
                c.dstOffset().set(r.getDstOffset().x, r.getDstOffset().y, r.getDstOffset().z);
                c.extent().set(r.getSize().x, r.getSize().y, r.getSize().z);
                c.srcSubresource()
                    .mipLevel(r.getSrcMipLevel())
                    .baseArrayLayer(r.getSrcBaseLayer())
                    .layerCount(r.getLayerCount())
                    .aspectMask(r.getAspects().bits());
                c.dstSubresource()
                    .mipLevel(r.getDstMipLevel())
                    .baseArrayLayer(r.getDstBaseLayer())
                    .layerCount(r.getLayerCount())
                    .aspectMask(r.getAspects().bits());
            }
            vkCmdCopyImage(buffer, src.getHandle(), src.getLayout().getEnum(), dst.getHandle(), dst.getLayout().getEnum(), imgCopy.flip());
        }
        addResource(src);
        addResource(dst);
    }

    @Override
    public void cmdResolveMultisampled(EngineImage src, EngineImage dst, ImageCopy copy) {
        if (copy.getRegions().size() != 1) {
            throw new IllegalArgumentException("Copy structure must contain only one copy region.");
        }
        assert false : "not implemented";
    }

    private VkBufferImageCopy.Buffer populateBufferImageCopyInfo(MemoryStack stack, int bufferOffset, BufferImageCopy copy) {
        VkBufferImageCopy.Buffer bufImgCopy = VkBufferImageCopy.calloc(copy.getRegions().size(), stack);
        for (BufferImageCopy.Region r : copy.getRegions()) {
            VkBufferImageCopy c = bufImgCopy.get();
            c.bufferRowLength(r.getBufferTexels().x)
                    .bufferImageHeight(r.getBufferTexels().y)
                    .bufferOffset(bufferOffset + r.getBufferOffset());
            c.imageOffset().set(r.getImageOffset().x, r.getImageOffset().y, r.getImageOffset().z);
            c.imageExtent().set(r.getImageSize().x, r.getImageSize().y, r.getImageSize().z);
            bufImgCopy.imageSubresource()
                .mipLevel(r.getImageMipLevel())
                .baseArrayLayer(r.getImageBaseLayer())
                .layerCount(r.getImageLayerCount())
                .aspectMask(r.getAspects().bits());
        }
        return bufImgCopy.flip();
    }

    @Override
    public void cmdCopy(GpuBuffer src, EngineImage dst, BufferImageCopy copy) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vkCmdCopyBufferToImage(buffer, src.getHandle(), dst.getHandle(), dst.getLayout().getEnum(),
                    populateBufferImageCopyInfo(stack, src.offset(), copy));
        }
        useResource(src.getLifecycle(), Access.Read, PipelineStage.Transfer);
        addResource(dst);
    }

    @Override
    public void cmdCopy(EngineImage src, GpuBuffer dst, BufferImageCopy copy) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vkCmdCopyImageToBuffer(buffer, src.getHandle(), src.getLayout().getEnum(), dst.getHandle(),
                    populateBufferImageCopyInfo(stack, dst.offset(), copy));
        }
        addResource(src);
        useResource(dst.getLifecycle(), Access.Write, PipelineStage.Transfer);
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
    public void useResource(ResourceLifecycle lifecycle, Access access, Flag<PipelineStage> stageMask) {
        if (executing) {
            throw new IllegalStateException("Buffer is executing.");
        }
        lifecycle.awaitPreviousAccesses(this, access, stageMask);
        pendingAccess.add(new PendingAccess(lifecycle, access));
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
            for (PendingAccess p : pendingAccess) {
                p.record(this);
            }
            pendingAccess.clear();
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
        pendingAccess.clear();
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

    private static class PendingAccess {

        public final ResourceLifecycle lifecycle;
        public final Access access;

        public PendingAccess(ResourceLifecycle lifecycle, Access access) {
            this.lifecycle = lifecycle;
            this.access = access;
        }

        public void record(CommandBuffer cmd) {
            lifecycle.recordDeviceAccess(cmd, access);
        }

    }

}

