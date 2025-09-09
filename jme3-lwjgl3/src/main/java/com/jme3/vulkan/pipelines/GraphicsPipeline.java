package com.jme3.vulkan.pipelines;

import com.jme3.util.natives.Native;
import com.jme3.vulkan.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.mesh.MeshDescription;
import com.jme3.vulkan.pass.RenderPass;
import com.jme3.vulkan.pipelines.states.*;
import com.jme3.vulkan.shader.ShaderModule;
import com.jme3.vulkan.shader.ShaderStage;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collection;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class GraphicsPipeline extends Pipeline {

    private final RenderPass compat;
    private final int subpassIndex;

    public GraphicsPipeline(LogicalDevice<?> device, PipelineLayout layout, RenderPass compat, int subpassIndex) {
        super(device, PipelineBindPoint.Graphics, layout);
        this.compat = compat;
        this.subpassIndex = subpassIndex;
    }

    public RenderPass getCompat() {
        return compat;
    }

    public int getSubpassIndex() {
        return subpassIndex;
    }

    public Builder build() {
        return new Builder();
    }

    public class Builder extends AbstractNative.Builder<GraphicsPipeline> {

        private final Collection<ShaderStageInfo> stages = new ArrayList<>();
        private final DynamicState dynamic = new DynamicState();
        private final VertexInputState vertexInput = new VertexInputState();
        private final InputAssemblyState inputAssembly = new InputAssemblyState();
        private final ViewportState viewport = new ViewportState();
        private final DepthStencilState depthStencil = new DepthStencilState();
        private final RasterizationState rasterization = new RasterizationState();
        private final MultisampleState multisample = new MultisampleState();
        private final ColorBlendState colorBlend = new ColorBlendState();

        @Override
        protected void build() {
            VkPipelineShaderStageCreateInfo.Buffer stageBuf = VkPipelineShaderStageCreateInfo.calloc(stages.size(), stack);
            for (ShaderStageInfo s : stages) {
                stageBuf.get().sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                        .stage(s.stage.bits())
                        .module(s.module.getNativeObject())
                        .pName(stack.UTF8(s.entryPoint));
            }
            stageBuf.flip();
            VkGraphicsPipelineCreateInfo.Buffer pipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .stageCount(stageBuf.limit())
                    .pStages(stageBuf)
                    .pVertexInputState(vertexInput.toStruct(stack))
                    .pInputAssemblyState(inputAssembly.toStruct(stack))
                    .pViewportState(viewport.toStruct(stack))
                    .pDepthStencilState(depthStencil.toStruct(stack))
                    .pRasterizationState(rasterization.toStruct(stack))
                    .pMultisampleState(multisample.toStruct(stack))
                    .pColorBlendState(colorBlend.toStruct(stack))
                    .pDynamicState(dynamic.toStruct(stack))
                    .layout(layout.getNativeObject())
                    .renderPass(compat.getNativeObject())
                    .subpass(subpassIndex)
                    .basePipelineHandle(VK_NULL_HANDLE)
                    .basePipelineIndex(-1);
            System.out.println("render pass: " + compat.getNativeObject());
            LongBuffer idBuf = stack.mallocLong(1);
            // todo: look into pipeline caching
            check(vkCreateGraphicsPipelines(device.getNativeObject(), VK_NULL_HANDLE, pipeline, null, idBuf),
                    "Failed to create graphics pipeline");
            object = idBuf.get(0);
            ref = Native.get().register(GraphicsPipeline.this);
            device.getNativeReference().addDependent(ref);
        }

        public void addShader(ShaderModule module, ShaderStage stage) {
            addShader(module, stage, DEFAULT_SHADER_ENTRY_POINT);
        }

        public void addShader(ShaderModule module, ShaderStage stage, String entryPoint) {
            this.stages.add(new ShaderStageInfo(module, stage, entryPoint));
        }

        public DynamicState getDynamicState() {
            return dynamic;
        }

        public VertexInputState getVertexInput() {
            return vertexInput;
        }

        public InputAssemblyState getInputAssembly() {
            return inputAssembly;
        }

        public ViewportState getViewportState() {
            return viewport;
        }

        public DepthStencilState getDepthStencil() {
            return depthStencil;
        }

        public RasterizationState getRasterization() {
            return rasterization;
        }

        public MultisampleState getMultisample() {
            return multisample;
        }

        public ColorBlendState getColorBlend() {
            return colorBlend;
        }

    }

    public static class ShaderStageInfo {

        private final ShaderModule module;
        private final ShaderStage stage;
        private final String entryPoint;

        public ShaderStageInfo(ShaderModule module, ShaderStage stage, String entryPoint) {
            this.module = module;
            this.stage = stage;
            this.entryPoint = entryPoint;
        }

    }

}
