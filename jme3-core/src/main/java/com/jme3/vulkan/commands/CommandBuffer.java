package com.jme3.vulkan.commands;

import com.jme3.asset.AssetManager;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.ViewPortArea;
import com.jme3.vulkan.VulkanEnums;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.descriptors.DescriptorSet;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.images.VulkanImageView;
import com.jme3.vulkan.material.VulkanMaterial;
import com.jme3.vulkan.material.experimental.SetBindCommand;
import com.jme3.vulkan.material.shader.VulkanShaderModule;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.pipeline.*;
import com.jme3.util.cache.InlineTimedCache;
import com.jme3.vulkan.pipeline.framebuffer.FrameBuffer;
import com.jme3.vulkan.pipeline.framebuffer.RenderTarget;
import com.jme3.vulkan.pipeline.framebuffer.VulkanFrameBuffer;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.awt.*;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK14.*;

public class CommandBuffer implements RenderCommands {

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

    private static final int ACTIVE_SET = 0, STAGED_SET = 1;
    private static final int LOWER = 0, UPPER = 1;

    private final CommandPool pool;
    private final VkCommandBuffer buffer;
    private final Collection<Sync> signals = new ArrayList<>();
    private final Collection<WaitSync> waits = new ArrayList<>();
    private final Collection<Runnable> completionListeners = new ArrayList<>();
    private final Queue<VulkanBuffer> stagedUploads = new LinkedList<>();
    private TimelineSemaphore completionSemaphore;
    private boolean recording = false;

    private VulkanFrameBuffer activeFrameBuffer;
    private Pipeline activePipeline;
    private VulkanMaterial activeMaterial;
    private final ViewPortArea activeViewPort = new ViewPortArea(0f, 0f);
    private RenderState forcedRenderState;

    private AssetManager assetManager;
    private InlineTimedCache<Pipeline> pipelineCache;
    private InlineTimedCache<PipelineLayout> layoutCache;
    private InlineTimedCache<VulkanShaderModule> shaderCache;
    private InlineTimedCache<DescriptorSetLayout> setLayoutCache;

    private final SetBindCommand<DescriptorSet>[][] boundSets;
    private final int[] stagedSetRange;

    protected CommandBuffer(CommandPool pool, VkCommandBuffer buffer) {
        this.pool = pool;
        this.buffer = buffer;
        this.boundSets = new SetBindCommand[pool.getDevice().getPhysicalLimits().maxBoundDescriptorSets()][2];
        this.stagedSetRange = new int[] {boundSets.length, 0};
    }

    @Override
    public void cmdBindFrameBuffer(FrameBuffer fbo, VulkanImage.Load colorLoad, VulkanImage.Store colorStore, VulkanImage.Load depthLoad, VulkanImage.Store depthStore) {
        activeFrameBuffer = (VulkanFrameBuffer)fbo;
        activeFrameBuffer.beginDynamicRender(this, colorLoad, colorStore, depthLoad, depthStore);
    }

    @Override
    public void cmdSetViewPort(ViewPortArea area) {
        if (activeViewPort.equals(area)) {
            return;
        }
        activeViewPort.set(area);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkViewport.Buffer vp = VkViewport.malloc(1, stack);
            float x = area.getLeft() * area.getWidth();
            float y = area.getTop() * area.getHeight();
            float w = area.getRight() * area.getWidth() - x;
            float h = area.getBottom() * area.getHeight() - y;
            vp.get().set(x, y, w, h, area.getMinDepth(), area.getMaxDepth());
            vkCmdSetViewport(buffer, 0, vp.flip());
        }
    }

    @Override
    public void cmdCopyBuffer(MappableBuffer src, MappableBuffer dst, MemorySize... regions) {
        if (regions.length == 0) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCopy.Buffer copy = VkBufferCopy.malloc(regions.length, stack);
            for (MemorySize r : regions) {
                copy.get().set()
            }
        }
    }

    public void cmdBindPipeline(Pipeline pipeline) {
        if (pipeline != activePipeline) {
            if (activePipeline != null && !pipeline.getLayout().equals(activePipeline.getLayout())) {
                for (SetBindCommand<DescriptorSet>[] stack : boundSets) {
                    stack[ACTIVE_SET] = null;
                }
            }
            pipeline.bind(this);
            activePipeline = pipeline;
        }
    }

    public void stageShaderSets(int location, SetBindCommand... sets) {
        for (int i = 0; i < sets.length; i++) {
            boundSets[location + i][STAGED_SET] = sets[i];
        }
        stagedSetRange[LOWER] = Math.min(stagedSetRange[LOWER], location);
        stagedSetRange[UPPER] = Math.max(stagedSetRange[UPPER], location + sets.length);
    }

    public void fillPipelineLayoutSets(PipelineLayout.Builder layout) {
        for (int i = 0; i < boundSets.length; i++) {
            SetBindCommand<DescriptorSet>[] stack = boundSets[i];
            SetBindCommand<DescriptorSet> set = (stack[STAGED_SET] == null ? stack[ACTIVE_SET] : stack[STAGED_SET]);
            if (set != null) layout.addSetLayout(i, set.getSet().getLayout());
        }
    }

    public void cmdBindStagedSets() {
        if (activePipeline == null) {
            throw new IllegalStateException("No pipeline bound.");
        }
        if (stagedSetRange[LOWER] >= stagedSetRange[UPPER]) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer binds = stack.mallocLong(stagedSetRange[UPPER] - stagedSetRange[LOWER]);
            IntBuffer offsets = stack.mallocInt(binds.capacity());
            for (int i = stagedSetRange[LOWER], location = i; i < stagedSetRange[UPPER]; i++) {
                for (; i < boundSets.length && boundSets[i][STAGED_SET] != null && !boundSets[i][STAGED_SET].equals(boundSets[i][ACTIVE_SET]); i++) {
                    SetBindCommand<DescriptorSet>[] bindStack = boundSets[i];
                    bindStack[ACTIVE_SET] = bindStack[STAGED_SET];
                    bindStack[STAGED_SET] = null;
                    binds.put(bindStack[ACTIVE_SET].getSet().getNativeObject());
                    offsets.put(bindStack[ACTIVE_SET].getDynamicOffset());
                }
                if (binds.position() > 0) {
                    vkCmdBindDescriptorSets(buffer, activePipeline.getBindPoint().getEnum(),
                            activePipeline.getLayout().getNativeObject(), location, binds.flip(), offsets.flip());
                }
                location = i + 1;
            }
        }
        stagedSetRange[LOWER] = boundSets.length;
        stagedSetRange[UPPER] = 0;
    }

    public void stageBufferUpload(VulkanBuffer buffer) {
        stagedUploads.add(buffer);
    }

    public void cmdBeginDynamicRender(FrameBuffer<VulkanImageView> fbo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderingInfo render = VkRenderingInfo.calloc(stack).sType$Default();
            if (!fbo.getColorTargets().isEmpty()) {
                VkRenderingAttachmentInfo.Buffer colors = VkRenderingAttachmentInfo.calloc(fbo.getColorTargets().size(), stack);
                for (RenderTarget<VulkanImageView> t : fbo.getColorTargets()) {
                    fillDynamicAttachment(t, colors.get().sType$Default());
                    t.getView().getImage().transitionLayout(stack, this, t.getLayout());
                }
                render.pColorAttachments(colors.flip());
            }
            RenderTarget<VulkanImageView> depth = fbo.getDepthStencilTarget();
            if (depth != null) {
                VkRenderingAttachmentInfo att = fillDynamicAttachment(depth, VkRenderingAttachmentInfo.calloc(stack).sType$Default());
                depth.getView().getImage().transitionLayout(stack, this, depth.getLayout());
                render.pDepthAttachment(att);
                if (fbo.isUsingStencil()) {
                    render.pStencilAttachment(att);
                }
            }
            Point area = fbo.getArea();
            render.renderArea().offset().set(0, 0);
            render.renderArea().extent().set(area.x, area.y);
            vkCmdBeginRendering(buffer, render);
        }
    }

    public void cmdEndDynamicRender() {
        vkCmdEndRendering(buffer);
    }

    private VkRenderingAttachmentInfo fillDynamicAttachment(RenderTarget<VulkanImageView> t, VkRenderingAttachmentInfo att) {
        att.imageView(t.getView().getId()).imageLayout(t.getLayout().getEnum());
        att.loadOp(t.getLoad().getEnum(VulkanEnums.instance)).storeOp(t.getStore().getEnum(VulkanEnums.instance));
        if (t.getAspects().containsAny(VulkanImage.Aspect.Color)) {
            ColorRGBA clear = t.getClearColor();
            att.clearValue().color().float32().put(clear.r).put(clear.g).put(clear.b).put(clear.a).flip();
        }
        if (t.getAspects().containsAny(VulkanImage.Aspect.DepthStencil)) {
            att.clearValue().depthStencil().set(t.getClearDepth(), t.getClearStencil());
        }
        return att;
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
     * and may be reused with new commands. The allocating {@link CommandPool}
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
        for (SetBindCommand<DescriptorSet>[] s : boundSets) {
            Arrays.fill(s, null);
        }
        stagedSetRange[LOWER] = boundSets.length;
        stagedSetRange[UPPER] = 0;
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
