package com.jme3.vulkan;

import com.jme3.material.RenderState;
import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

public class GraphicsPipeline implements Native<Long> {

    private final LogicalDevice device;
    private final NativeReference ref;
    private long id;

    public GraphicsPipeline(LogicalDevice device, PipelineLayout layout, RenderPass compat, RenderState state, ShaderModule vert, ShaderModule frag, MeshDescription mesh) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
            stages.get(0).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_VERTEX_BIT)
                    .module(vert.getNativeObject())
                    .pName(stack.UTF8(vert.getEntryPoint())); // function initially called in the shader
            stages.get(1).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(frag.getNativeObject())
                    .pName(stack.UTF8(frag.getEntryPoint()));
            VkPipelineDynamicStateCreateInfo dynamic = VkPipelineDynamicStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                    .pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));
            VkPipelineVertexInputStateCreateInfo vertInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                    .pVertexBindingDescriptions(mesh.getBindings())
                    .pVertexAttributeDescriptions(mesh.getAttributes());
            VkPipelineInputAssemblyStateCreateInfo assembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                    .primitiveRestartEnable(false);
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                    .x(0f).y(0f)
                    .width(1024).height(1024) // todo: ensure passing random values here is acceptable
                    .minDepth(0f).maxDepth(1f);
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset().set(0, 0);
            scissor.extent().width(1024).height(1024); // todo: ensure passing random values here is acceptable
            VkPipelineViewportStateCreateInfo vpState = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .pViewports(viewport)
                    .pScissors(scissor);
            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(state.isDepthTest())
                    .depthWriteEnable(state.isDepthWrite())
                    .depthCompareOp(RenderStateToVulkan.depthFunc(state.getDepthFunc()))
                    .depthBoundsTestEnable(false)
                    .stencilTestEnable(false);
            VkPipelineRasterizationStateCreateInfo raster = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(false)
                    .rasterizerDiscardEnable(false)
                    .polygonMode(VK_POLYGON_MODE_FILL)
                    .lineWidth(1f)
                    .cullMode(RenderStateToVulkan.faceCull(state.getFaceCullMode()))
                    .frontFace(VK_FRONT_FACE_CLOCKWISE)
                    .cullMode(VK_CULL_MODE_NONE)
                    .depthBiasEnable(false);
            VkPipelineMultisampleStateCreateInfo multisample = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .sampleShadingEnable(false)
                    .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);
            // todo: configure depth and stencil buffers
            VkPipelineColorBlendAttachmentState.Buffer blendAtt = VkPipelineColorBlendAttachmentState.calloc(1, stack)
                    .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                    .blendEnable(state.getBlendMode() != RenderState.BlendMode.Off)
                    .srcColorBlendFactor(VK_BLEND_FACTOR_ONE) // todo: control with render state
                    .dstColorBlendFactor(VK_BLEND_FACTOR_ZERO)
                    .colorBlendOp(RenderStateToVulkan.blendEquation(state.getBlendEquation()))
                    .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                    .srcAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
                    .alphaBlendOp(RenderStateToVulkan.blendEquationAlpha(state.getBlendEquationAlpha(), state.getBlendEquation()));
            VkPipelineColorBlendStateCreateInfo blend = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(false)
                    .logicOp(VK_LOGIC_OP_COPY)
                    .pAttachments(blendAtt);
            VkGraphicsPipelineCreateInfo.Buffer pipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .stageCount(2)
                    .pStages(stages)
                    .pVertexInputState(vertInput)
                    .pInputAssemblyState(assembly)
                    .pViewportState(vpState)
                    .pDepthStencilState(depthStencil)
                    .pRasterizationState(raster)
                    .pMultisampleState(multisample)
                    .pColorBlendState(blend)
                    .pDynamicState(dynamic)
                    .layout(layout.getNativeObject())
                    .renderPass(compat.getNativeObject())
                    .subpass(0)
                    .basePipelineHandle(VK_NULL_HANDLE)
                    .basePipelineIndex(-1);
            System.out.println("render pass: " + compat.getNativeObject());
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateGraphicsPipelines(device.getNativeObject(), VK_NULL_HANDLE, pipeline, null, idBuf),
                    "Failed to create graphics pipeline");
            id = idBuf.get(0);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            vkDestroyPipeline(device.getNativeObject(), id, null);
        };
    }

    @Override
    public void prematureNativeDestruction() {
        id = VK_NULL_HANDLE;
    }

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public void bind(CommandBuffer cmd) {
        vkCmdBindPipeline(cmd.getBuffer(), VK_PIPELINE_BIND_POINT_GRAPHICS, id);
    }

}
