package com.jme3.vulkan.pipeline.framebuffer;

import com.jme3.texture.ImageView;
import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.images.VulkanImageView;
import com.jme3.vulkan.pass.RenderPass;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK14.*;

public class GeneralFrameBuffer implements VulkanFrameBuffer<VulkanRenderTarget> {

    public enum Create implements Flag<Create> {

        Imageless(VK_FRAMEBUFFER_CREATE_IMAGELESS_BIT);

        private final int bits;

        Create(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

    }

    private final RenderPass compat;
    private final float width, height;
    private final List<VulkanRenderTarget> colorTargets = new ArrayList<>();
    private VulkanRenderTarget depthTarget;
    private Flag<Create> flags = Flag.empty();
    private RenderPassHandle handle;

    public GeneralFrameBuffer(RenderPass compat, float width, float height) {
        this.compat = compat;
        this.width = width;
        this.height = height;
    }

    @Override
    public long getBufferId(LogicalDevice<?> device) {
        if (handle == null || !handle.upToDate()) {
            handle = new RenderPassHandle(device);
        }
        return handle.id;
    }

    @Override
    public VulkanRenderTarget createColorTarget(ImageView view) {
        return VulkanRenderTarget.createColorTarget((VulkanImageView)view);
    }

    @Override
    public VulkanRenderTarget createDepthTarget(ImageView view) {
        return VulkanRenderTarget.createDepthTarget((VulkanImageView)view);
    }

    @Override
    public void addColorTarget(VulkanRenderTarget target) {
        colorTargets.add(target);
        handle = null;
    }

    @Override
    public void addColorTarget(int i, VulkanRenderTarget target) {
        colorTargets.add(i, target);
        handle = null;
    }

    @Override
    public void setColorTarget(int i, VulkanRenderTarget target) {
        if (colorTargets.set(i, target) != target) {
            handle = null;
        }
    }

    @Override
    public boolean removeColorTarget(VulkanRenderTarget target) {
        if (colorTargets.remove(target)) {
            handle = null;
            return true;
        }
        return false;
    }

    @Override
    public void clearColorTargets() {
        if (!colorTargets.isEmpty()) {
            colorTargets.clear();
            handle = null;
        }
    }

    @Override
    public void setDepthTarget(VulkanRenderTarget target) {
        if (target != depthTarget) {
            depthTarget = target;
            handle = null;
        }
    }

    @Override
    public List<VulkanRenderTarget> getColorTargets() {
        return Collections.unmodifiableList(colorTargets);
    }

    @Override
    public VulkanRenderTarget getDepthTarget() {
        return depthTarget;
    }

    @Override
    public void beginDynamicRender(CommandBuffer cmd, VulkanImage.Load colorLoad, VulkanImage.Store colorStore, VulkanImage.Load depthLoad, VulkanImage.Store depthStore, Flag<Render> flags) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderingInfo render = VkRenderingInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDERING_INFO)
                    .flags(flags.bits());
            int w = Integer.MAX_VALUE;
            int h = Integer.MAX_VALUE;
            if (!colorTargets.isEmpty()) {
                VkRenderingAttachmentInfo.Buffer colors = VkRenderingAttachmentInfo.calloc(colorTargets.size(), stack);
                for (VulkanRenderTarget t : colorTargets) {
                    t.fill(colors.get().sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO), colorLoad, colorStore);
                    t.transition(cmd);
                    w = Math.min(w, t.getView().getImage().getWidth());
                    h = Math.min(h, t.getView().getImage().getHeight());
                }
                render.pColorAttachments(colors.flip());
            }
            if (depthTarget != null) {
                VkRenderingAttachmentInfo att = depthTarget.fill(VkRenderingAttachmentInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO), colorLoad, colorStore);
                render.pDepthAttachment(att);
                if (depthTarget.getView().getAspect().contains(VulkanImage.Aspect.Stencil)) {
                    render.pStencilAttachment(att);
                }
                depthTarget.transition(cmd);
                w = Math.min(w, depthTarget.getView().getImage().getWidth());
                h = Math.min(h, depthTarget.getView().getImage().getHeight());
            }
            render.renderArea().offset().set(0, 0);
            render.renderArea().extent().set(w, h);
            vkCmdBeginRendering(cmd.getBuffer(), render);
        }
    }

    @Override
    public boolean isUsingStencil() {
        return depthTarget != null && depthTarget.getView().getAspect().contains(VulkanImage.Aspect.Stencil);
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
    }

    public void setFlags(Flag<Create> flags) {
        if (!Flag.is(this.flags, Objects.requireNonNull(flags))) {
            this.flags = flags;
            handle = null;
        }
    }

    public Flag<Create> getFlags() {
        return flags;
    }

    protected class RenderPassHandle implements Disposable {

        private final LogicalDevice<?> device;
        private final DisposableReference ref;
        private final long id;
        private final long[] targetVersions;

        public RenderPassHandle(LogicalDevice<?> device) {
            this.device = device;
            this.targetVersions = new long[colorTargets.size() + 2];
            int versionIndex = 0;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                LongBuffer att = stack.mallocLong(colorTargets.size() + 2);
                int w = Integer.MAX_VALUE;
                int h = Integer.MAX_VALUE;
                for (VulkanRenderTarget c : colorTargets) {
                    att.put(c.getView().getId());
                    targetVersions[versionIndex++] = c.getVersion();
                    w = Math.min(w, c.getView().getImage().getWidth());
                    h = Math.min(h, c.getView().getImage().getHeight());
                }
                if (depthTarget != null) {
                    att.put(depthTarget.getView().getId());
                    targetVersions[versionIndex] = depthTarget.getVersion();
                    w = Math.min(w, depthTarget.getView().getImage().getWidth());
                    h = Math.min(h, depthTarget.getView().getImage().getHeight());
                } else {
                    targetVersions[versionIndex] = -1;
                }
                att.flip();
                VkFramebufferCreateInfo create = VkFramebufferCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                        .flags(flags.bits())
                        .renderPass(compat.getNativeObject())
                        .pAttachments(att)
                        .width(w).height(h);
                LongBuffer idBuf = stack.mallocLong(1);
                check(vkCreateFramebuffer(device.getNativeObject(), create, null, idBuf));
                id = idBuf.get(0);
            }
            ref = DisposableManager.reference(this);
            device.getReference().addDependent(ref);
        }

        @Override
        public Runnable createDestroyer() {
            return () -> vkDestroyFramebuffer(device.getNativeObject(), id, null);
        }

        @Override
        public DisposableReference getReference() {
            return ref;
        }

        public boolean upToDate() {
            int i = 0;
            for (VulkanRenderTarget t : colorTargets) {
                if (t.getVersion() != targetVersions[i++]) return false;
            }
            return (depthTarget == null && targetVersions[i] < 0) || depthTarget.getVersion() == targetVersions[i];
        }

        public LogicalDevice<?> getDevice() {
            return device;
        }

        public long getId() {
            return id;
        }

    }

}
