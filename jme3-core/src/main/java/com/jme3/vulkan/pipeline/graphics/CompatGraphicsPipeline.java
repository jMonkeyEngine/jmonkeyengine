package com.jme3.vulkan.pipeline.graphics;

import com.jme3.material.RenderState;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.mesh.MeshLayout;
import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pipeline.PipelineLayout;
import com.jme3.vulkan.pipeline.PolygonMode;
import com.jme3.vulkan.util.RenderStateToVulkan;

import java.util.function.Consumer;

public class CompatGraphicsPipeline extends GraphicsPipeline {

    private final RenderState state;

    protected CompatGraphicsPipeline(LogicalDevice<?> device, Subpass subpass, PipelineLayout layout,
                                     MeshLayout mesh, RenderState state) {
        super(device, subpass, layout, mesh);
        this.state = state.clone();
    }

    public static CompatGraphicsPipeline build(LogicalDevice<?> device, Subpass subpass, PipelineLayout layout,
                                               MeshLayout mesh, RenderState state, Consumer<Builder> config) {
        Builder b = new CompatGraphicsPipeline(device, subpass, layout, mesh, state).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends GraphicsPipeline.Builder {

        private int numBlendAttachments = 1;

        protected Builder() {}

        @Override
        public CompatGraphicsPipeline build() {
            super.build();
            return CompatGraphicsPipeline.this;
        }

        @Override
        public CompatGraphicsPipeline construct() {
            ColorBlendAttachment blendAtt = new ColorBlendAttachment(state);
            for (int i = 0; i < numBlendAttachments; i++) {
                super.addBlendAttachment(blendAtt);
            }
            setPolygonMode(state.isWireframe() ? PolygonMode.Line : PolygonMode.Fill);
            setCullMode(RenderStateToVulkan.faceCull(state.getFaceCullMode()));
            setDepthCompare(RenderStateToVulkan.depthFunc(state.getDepthFunc()));
            setStencilTest(state.isStencilTest());
            setLineWidth(state.getLineWidth());
            setDepthTest(state.isDepthTest());
            setDepthWrite(state.isDepthWrite());
            super.construct();
            return CompatGraphicsPipeline.this;
        }

        public void setNumBlendAttachments(int blendAttachments) {
            this.numBlendAttachments = blendAttachments;
        }

        @Override
        public void addBlendAttachment(ColorBlendAttachment attachment) {
            throw new UnsupportedOperationException("Blend attachments cannot be added manually.");
        }

    }

}
