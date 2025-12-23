package com.jme3.vulkan.pipeline.framebuffer;

import com.jme3.vulkan.images.VulkanImageView;
import com.jme3.vulkan.pass.RenderPass;
import com.jme3.vulkan.surface.Swapchain;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;

import java.util.List;
import java.util.Objects;

public class OutputFrameBuffer implements FrameBuffer<VulkanImageView> {

    private final Swapchain swapchain;
    private final RenderPass compat;
    private Swapchain.PresentImage current;
    private VulkanImageView depthStencil;

    public OutputFrameBuffer(Swapchain swapchain, RenderPass compat) {
        this.swapchain = swapchain;
        this.compat = compat;
    }

    @Override
    public void addColorTarget(VulkanImageView image) {
        throw new UnsupportedOperationException("Cannot add color target to output framebuffer.");
    }

    @Override
    public void setColorTarget(int i, VulkanImageView image) {
        throw new UnsupportedOperationException("Cannot set color target of output framebuffer.");
    }

    @Override
    public void removeColorTarget(int i) {
        throw new UnsupportedOperationException("Cannot remove color target from output framebuffer.");
    }

    @Override
    public void removeColorTarget(VulkanImageView image) {
        throw new UnsupportedOperationException("Cannot remove color target from output framebuffer.");
    }

    @Override
    public void clearColorTargets() {
        throw new UnsupportedOperationException("Cannot clear color targets from output framebuffer.");
    }

    @Override
    public void setDepthTarget(VulkanImageView image) {
        if (depthStencil != Objects.requireNonNull(image, "Depth target cannot be null.")) {
            swapchain.createFrameBuffers(compat, image);
        }
        this.depthStencil = image;
    }

    @Override
    public List<VulkanImageView> getColorTargets() {
        verifyCurrent();
        return current.getFrameBuffer().getColorTargets();
    }

    @Override
    public VulkanImageView getColorTarget(int i) {
        verifyCurrent();
        return current.getFrameBuffer().getColorTarget(i);
    }

    @Override
    public VulkanImageView getDepthTarget() {
        return depthStencil;
    }

    @Override
    public long getId() {
        verifyCurrent();
        return current.getFrameBuffer().getId();
    }

    @Override
    public int getWidth() {
        return swapchain.getExtent().x;
    }

    @Override
    public int getHeight() {
        return swapchain.getExtent().y;
    }

    private void verifyCurrent() {
        if (current == null) {
            throw new NullPointerException("Swapchain target not acquired.");
        }
    }

    public boolean acquireNextImage(Semaphore signal, Fence fence, long timeoutMillis) {
        current = swapchain.acquireNextImage(signal, fence, timeoutMillis);
        return current != null;
    }

    public Swapchain.PresentImage getCurrentImage() {
        return current;
    }

}
