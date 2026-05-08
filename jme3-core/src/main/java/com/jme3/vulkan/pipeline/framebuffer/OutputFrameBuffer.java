package com.jme3.vulkan.pipeline.framebuffer;

import com.jme3.math.ColorRGBA;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.images.VulkanImageView;
import com.jme3.vulkan.pass.RenderPass;
import com.jme3.vulkan.surface.Swapchain;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

import java.util.*;

@Deprecated
public class OutputFrameBuffer implements VulkanFrameBuffer<VulkanRenderTarget> {

    private final Swapchain swapchain;
    private final Map<Swapchain.PresentImage, GeneralFrameBuffer> frames = new IdentityHashMap<>();
    private final List<VulkanRenderTarget> color = new ArrayList<>();
    private VulkanRenderTarget depth;
    private Swapchain.PresentImage currentImage;
    private boolean readyToRender = false;

    public OutputFrameBuffer(Swapchain swapchain, RenderPass compat) {
        this.swapchain = swapchain;
        this.currentImage = swapchain.getImages().getFirst();
        for (Swapchain.PresentImage i : swapchain.getImages()) {
            frames.put(i, new GeneralFrameBuffer(compat));
        }
        color.add(new MultiTarget(swapchain.getImages()));
    }

    public boolean acquireNextTarget(Semaphore signal, Fence fence, long timeoutMillis) {
        currentImage = swapchain.acquireNextImage(signal, fence, timeoutMillis);
        return readyToRender = currentImage != null;
    }

    public Swapchain.PresentImage getCurrentImage() {
        return currentImage;
    }

    @Override
    public void beginDynamicRender(CommandBuffer cmd, VulkanImage.Load colorLoad, VulkanImage.Store colorStore, VulkanImage.Load depthLoad, VulkanImage.Store depthStore, Flag<Render> flags) {
        assert readyToRender : "Next render target has not been acquired.";
        frames.get(currentImage).beginDynamicRender(cmd, colorLoad, colorStore, depthLoad, depthStore, flags);
        readyToRender = false;
    }

    @Override
    public long getBufferId(LogicalDevice<?> device) {
        return frames.get(currentImage).getBufferId(device);
    }

    @Override
    public void addColorTarget(VulkanRenderTarget target) {
        color.add(target);
        for (GeneralFrameBuffer fbo : frames.values()) {
            fbo.addColorTarget(target);
        }
    }

    @Override
    public void addColorTarget(int i, VulkanRenderTarget target) {
        color.add(i, target);
    }

    @Override
    public void setColorTarget(int i, VulkanRenderTarget target) {
        color.set(i, target);
    }

    @Override
    public boolean removeColorTarget(VulkanRenderTarget target) {
        if (target instanceof MultiTarget) {
            throw new IllegalArgumentException("Cannot remove present color target.");
        }
        if (color.remove(target)) {
            for (GeneralFrameBuffer fbo : frames.values()) {
                fbo.removeColorTarget(target);
            }
            return true;
        }
        return false;
    }

    @Override
    public void clearColorTargets() {
        color.clear();
        for (GeneralFrameBuffer fbo : frames.values()) {
            fbo.clearColorTargets();
        }
        color.add(new MultiTarget(swapchain.getImages()));
    }

    @Override
    public void setDepthTarget(VulkanRenderTarget target) {
        depth = target;
        for (GeneralFrameBuffer fbo : frames.values()) {
            fbo.setDepthTarget(target);
        }
    }

    @Override
    public List<VulkanRenderTarget> getColorTargets() {
        return color;
    }

    @Override
    public VulkanRenderTarget getDepthTarget() {
        return depth;
    }

    @Override
    public boolean isUsingStencil() {
        return depth != null && depth.getView().getAspect().contains(VulkanImage.Aspect.Stencil);
    }

    private class MultiTarget extends VulkanRenderTarget {

        private final List<VulkanRenderTarget> targets = new ArrayList<>();

        public MultiTarget(List<Swapchain.PresentImage> images) {
            super(VulkanImage.Aspect.Color, images.getFirst().getColorView(), VulkanImage.Layout.ColorAttachmentOptimal);
            for (Swapchain.PresentImage i : images) {
                VulkanRenderTarget t = VulkanRenderTarget.createColorTarget(i.getColorView());
                frames.get(i).addColorTarget(t);
                targets.add(t);
            }
        }

        @Override
        public VulkanRenderTarget setView(VulkanImageView view) {
            throw new UnsupportedOperationException("Cannot replace present image as target.");
        }

        @Override
        public VulkanImageView getView() {
            return currentImage.getColorView();
        }

        @Override
        public VulkanRenderTarget setClearStencil(int clearStencil) {
            for (VulkanRenderTarget t : targets) {
                t.setClearStencil(clearStencil);
            }
            return super.setClearStencil(clearStencil);
        }

        @Override
        public VulkanRenderTarget setClearDepth(float clearDepth) {
            for (VulkanRenderTarget t : targets) {
                t.setClearDepth(clearDepth);
            }
            return super.setClearDepth(clearDepth);
        }

        @Override
        public VulkanRenderTarget setClearColor(ColorRGBA clearColor) {
            for (VulkanRenderTarget t : targets) {
                t.setClearColor(clearColor);
            }
            return super.setClearColor(clearColor);
        }

        @Override
        public VulkanRenderTarget setLayout(VulkanImage.Layout layout) {
            for (VulkanRenderTarget t : targets) {
                t.setLayout(layout);
            }
            return super.setLayout(layout);
        }

    }

}
