package com.jme3.vulkan.commands;

import com.jme3.renderer.ScissorArea;
import com.jme3.renderer.ViewPortArea;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

import static org.lwjgl.vulkan.VK10.vkCmdSetScissor;
import static org.lwjgl.vulkan.VK10.vkCmdSetViewport;

public class VulkanRenderSettings implements StandardRenderSettings {

    private final CommandBuffer cmd;

    public VulkanRenderSettings(CommandBuffer cmd) {
        this.cmd = cmd;
    }

    private final RenderSetting<ViewPortArea> viewport = new RenderSetting<ViewPortArea>() {
        @Override
        protected void apply(ViewPortArea value) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkViewport.Buffer vp = VkViewport.malloc(1, stack)
                        .x(value.getX())
                        .y(value.getY())
                        .width(value.getWidth())
                        .height(value.getHeight())
                        .minDepth(value.getMinDepth())
                        .maxDepth(value.getMaxDepth());
                vkCmdSetViewport(cmd.getBuffer(), 0, vp);
            }
        }
    };

    private final RenderSetting<ScissorArea> scissor = new RenderSetting<ScissorArea>() {
        @Override
        protected void apply(ScissorArea value) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkRect2D.Buffer scissor = VkRect2D.malloc(1, stack);
                scissor.offset().set(value.getX(), value.getY());
                scissor.extent().set(value.getWidth(), value.getHeight());
                vkCmdSetScissor(cmd.getBuffer(), 0, scissor);
            }
        }
    };

    @Override
    public void applySettings() {
        viewport.apply();
        scissor.apply();
    }

    @Override
    public void pushViewPort(ViewPortArea area) {
        viewport.push(area);
    }

    @Override
    public ViewPortArea popViewPort() {
        return viewport.pop();
    }

    @Override
    public ViewPortArea getViewPort() {
        return viewport.peek();
    }

    @Override
    public void pushScissor(ScissorArea area) {
        scissor.push(area);
    }

    @Override
    public ScissorArea popScissor() {
        return scissor.pop();
    }

    @Override
    public ScissorArea getScissor() {
        return scissor.peek();
    }

}
