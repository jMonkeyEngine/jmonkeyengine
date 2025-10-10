package com.jme3.vulkan.pipelines;

import com.jme3.util.AbstractBuilder;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.allocation.GraphicsState;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pass.Subpass;
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

    private final Subpass subpass;
    private final Collection<ShaderStageInfo> shaders = new ArrayList<>();
    private final VertexInputState vertexInput = new VertexInputState();
    private final InputAssemblyState inputAssembly = new InputAssemblyState();
    private final ViewportState viewport = new ViewportState();
    private final DepthStencilState depthStencil = new DepthStencilState();
    private final RasterizationState rasterization = new RasterizationState();
    private final MultisampleState multisample = new MultisampleState();
    private final ColorBlendState colorBlend = new ColorBlendState();
    private final DynamicState dynamic = new DynamicState();

    public GraphicsPipeline(LogicalDevice<?> device, PipelineLayout layout, Subpass subpass) {
        super(device, PipelineBindPoint.Graphics, layout);
        this.subpass = subpass;
    }

    public Builder build() {
        return new Builder();
    }

    public class Builder extends AbstractBuilder {

        @Override
        protected void build() {
            VkGraphicsPipelineCreateInfo.Buffer pipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .flags(flags.bits())
                    .layout(layout.getNativeObject())
                    .renderPass(subpass.getPass().getNativeObject())
                    .subpass(subpass.getPosition())
                    .basePipelineHandle(parent != null ? parent.getNativeObject() : VK_NULL_HANDLE)
                    .basePipelineIndex(-1);
            if (!shaders.isEmpty()) {
                VkPipelineShaderStageCreateInfo.Buffer stageBuf = VkPipelineShaderStageCreateInfo.calloc(shaders.size(), stack);
                for (ShaderStageInfo s : shaders) {
                    stageBuf.get().sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                            .stage(s.stage.bits())
                            .module(s.module.getNativeObject())
                            .pName(stack.UTF8(s.entryPoint));
                }
                stageBuf.flip();
                pipeline.stageCount(stageBuf.limit())
                        .pStages(stageBuf);
            }
            pipeline.pVertexInputState(vertexInput.toStruct(stack));
            pipeline.pInputAssemblyState(inputAssembly.toStruct(stack));
            pipeline.pViewportState(viewport.toStruct(stack));
            pipeline.pDepthStencilState(depthStencil.toStruct(stack));
            pipeline.pRasterizationState(rasterization.toStruct(stack));
            pipeline.pMultisampleState(multisample.toStruct(stack));
            pipeline.pColorBlendState(colorBlend.toStruct(stack));
            pipeline.pDynamicState(dynamic.toStruct(stack));
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
            shaders.add(new ShaderStageInfo(module, stage, entryPoint));
        }

        public DynamicState getDynamic() {
            return dynamic;
        }

        public VertexInputState getVertexInput() {
            return vertexInput;
        }

        public InputAssemblyState getInputAssembly() {
            return inputAssembly;
        }

        public ViewportState getViewport() {
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
