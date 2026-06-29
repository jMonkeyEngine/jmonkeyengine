package com.jme3.vulkan.commands;

import com.jme3.renderer.ScissorArea;
import com.jme3.renderer.ViewPortArea;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.pipeline.framebuffer.FrameBuffer;

import java.util.function.Consumer;

public interface RenderCommands {

    void cmdSetViewPort(ViewPortArea area);

    void cmdSetScissor(ScissorArea area);

}
