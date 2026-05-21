package com.jme3.vulkan.pipeline.framebuffer;

import com.jme3.texture.ImageView;
import com.jme3.vulkan.images.VulkanImage;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FrameBuffer <T extends ImageView> {

    private final List<RenderTarget<T>> colorTargets = new ArrayList<>();
    private RenderTarget<T> depthStencilTarget;

    public void setDepthStencilTarget(RenderTarget<T> depthStencilTarget) {
        this.depthStencilTarget = depthStencilTarget;
    }

    public List<RenderTarget<T>> getColorTargets() {
        return colorTargets;
    }

    public RenderTarget<T> getDepthStencilTarget() {
        return depthStencilTarget;
    }

    public boolean isUsingDepth() {
        return depthStencilTarget != null;
    }

    public boolean isUsingStencil() {
        return isUsingDepth() && depthStencilTarget.getAspects().containsAny(VulkanImage.Aspect.Stencil);
    }

    public Point getArea() {
        Point p = new Point(0, 0);
        for (RenderTarget<T> t : colorTargets) {
            p.x = Math.max(p.x, t.getWidth());
            p.y = Math.max(p.y, t.getHeight());
        }
        if (depthStencilTarget != null) {
            p.x = Math.max(p.x, depthStencilTarget.getWidth());
            p.y = Math.max(p.y, depthStencilTarget.getHeight());
        }
        return p;
    }

}
