package com.jme3.vulkan.pipelines.states;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;

public class ViewportState implements PipelineState<VkPipelineViewportStateCreateInfo> {

    private final List<ViewportInfo> viewports = new ArrayList<>();
    private final List<ScissorInfo> scissors = new ArrayList<>();

    @Override
    public VkPipelineViewportStateCreateInfo toStruct(MemoryStack stack) {
        VkViewport.Buffer vpBuf = VkViewport.calloc(viewports.size(), stack);
        for (ViewportInfo v : viewports) {
            vpBuf.get().x(v.x).y(v.y).width(v.w).height(v.h).minDepth(v.min).maxDepth(v.max);
        }
        vpBuf.flip();
        VkRect2D.Buffer scissorBuf = VkRect2D.calloc(scissors.size(), stack);
        for (ScissorInfo s : scissors) {
            VkRect2D e = scissorBuf.get();
            e.offset().set(s.x, s.y);
            e.extent().set(s.w, s.h);
        }
        scissorBuf.flip();
        return VkPipelineViewportStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .pViewports(vpBuf)
                .pScissors(scissorBuf);
    }

    public void addViewport() {
        addViewport(0f, 0f, 128f, 128f);
    }

    public void addViewport(float x, float y, float w, float h) {
        addViewport(x, y, w, h, 0f, 1f);
    }

    public void addViewport(float x, float y, float w, float h, float minDepth, float maxDepth) {
        viewports.add(new ViewportInfo(x, y, w, h, minDepth, maxDepth));
    }

    public void addScissor() {
        addScissor(0, 0, 128, 128);
    }

    public void addScissor(int x, int y, int w, int h) {
        scissors.add(new ScissorInfo(x, y, w, h));
    }

    private static class ViewportInfo {

        public final float x, y, w, h;
        public final float min, max;

        public ViewportInfo(float x, float y, float w, float h, float min, float max) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.min = min;
            this.max = max;
        }

    }

    private static class ScissorInfo {

        public final int x, y, w, h;

        public ScissorInfo(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

    }

}
