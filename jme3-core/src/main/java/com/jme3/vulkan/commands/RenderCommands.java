package com.jme3.vulkan.commands;

import com.jme3.renderer.ViewPortArea;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.pipeline.framebuffer.FrameBuffer;

public interface RenderCommands {

    void cmdBindFrameBuffer(FrameBuffer fbo, VulkanImage.Load colorLoad, VulkanImage.Store colorStore, VulkanImage.Load depthLoad, VulkanImage.Store depthStore);

    void cmdSetViewPort(ViewPortArea area);

    void cmdCopyBuffer(MappableBuffer src, MappableBuffer dst, MemorySize... regions);

}
