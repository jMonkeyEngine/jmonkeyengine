package com.jme3.vulkan.commands;

import com.jme3.renderer.ViewPortArea;
import com.jme3.scene.Geometry;
import com.jme3.vulkan.images.GpuImage;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.pipeline.framebuffer.FrameBuffer;
import com.jme3.vulkan.util.Flag;

public interface RenderCommands {

    void bindFrameBuffer(FrameBuffer fbo, VulkanImage.Load colorLoad, VulkanImage.Store colorStore, VulkanImage.Load depthLoad, VulkanImage.Store depthStore);

    void setViewPort(ViewPortArea area);

    void renderGeometry(Geometry geometry);

    void transitionImage(GpuImage image, VulkanImage.Layout layout);

    void blitFrameBuffer(FrameBuffer src, FrameBuffer dst, Flag<VulkanImage.Aspect> aspects);

}
