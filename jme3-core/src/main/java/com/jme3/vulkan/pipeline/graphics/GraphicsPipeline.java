package com.jme3.vulkan.pipeline.graphics;

import com.jme3.util.destruction.Destructable;
import com.jme3.util.destruction.ResourceLifecycle;
import com.jme3.vulkan.VulkanEnums;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.material.experimental.PipelineKey;
import com.jme3.vulkan.pipeline.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK13.*;

/**
 * A graphics pipeline for dynamic rendering, created from a {@link PipelineLayout}, shader stages,
 * and a {@link PipelineKey} holding all of its fixed-function state and the formats of the
 * attachments it renders to. Immutable once created.
 * <p>
 * The viewport and scissor are always dynamic, as are the values the key deliberately leaves out
 * when the feature using them is enabled (see {@link PipelineKey#getDynamicStates()}). They must be
 * set on the command buffer before drawing with this pipeline.
 * <p>
 * The pipeline depends on its layout: destroying the layout (or the device) destroys the pipeline.
 */
public class GraphicsPipeline implements Pipeline, Destructable {

    private final PipelineLayout layout;
    private final Set<DynamicState> dynamicStates;
    private final long handle;
    private final ResourceLifecycle lifecycle;

    /**
     * @param layout layout to create the pipeline against, whose push constant ranges must equal
     *               {@code key}'s
     * @param shaders shader stages, at most one per stage, including a vertex stage
     * @param key fixed-function state and attachment formats; not referenced after creation
     * @throws IllegalArgumentException if {@code layout}'s push constant ranges don't match {@code key}'s,
     *                                  or {@code shaders} has no vertex stage or two of one stage
     */
    public GraphicsPipeline(PipelineLayout layout, List<PipelineShader> shaders, PipelineKey key) {
        if (!key.getPushConstants().equals(layout.getPushConstantRanges())) {
            throw new IllegalArgumentException("Layout push constants " + layout.getPushConstantRanges()
                    + " do not match the key's " + key.getPushConstants() + ".");
        }
        validateShaders(shaders);
        this.layout = layout;
        this.dynamicStates = Collections.unmodifiableSet(collectDynamicStates(key));
        LogicalDevice<?> device = layout.getDevice();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(shaders.size(), stack);
            for (PipelineShader s : shaders) {
                s.fill(stack, stages.get());
            }
            VkGraphicsPipelineCreateInfo.Buffer create = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType$Default()
                    .pNext(createRendering(stack, key).address())
                    .pStages(stages.flip())
                    .pVertexInputState(createVertexInput(stack, key))
                    .pInputAssemblyState(createInputAssembly(stack, key))
                    .pViewportState(createViewport(stack))
                    .pRasterizationState(createRasterization(stack, key))
                    .pMultisampleState(createMultisample(stack, key))
                    .pDepthStencilState(createDepthStencil(stack, key))
                    .pColorBlendState(createColorBlend(stack, key))
                    .pDynamicState(createDynamic(stack))
                    .layout(layout.getHandle())
                    .renderPass(VK_NULL_HANDLE)
                    .basePipelineIndex(-1);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateGraphicsPipelines(device.getHandle(), VK_NULL_HANDLE, create, null, idBuf),
                    "Failed to create graphics pipeline.");
            handle = idBuf.get(0);
        }
        // the destroyer must not reference this, or the leak backstop could never see it
        VkDevice deviceHandle = device.getHandle();
        long h = handle;
        lifecycle = new ResourceLifecycle(this, () -> vkDestroyPipeline(deviceHandle, h, null));
        layout.getLifecycle().addDependent(lifecycle);
    }

    private static void validateShaders(List<PipelineShader> shaders) {
        int stages = 0;
        for (PipelineShader s : shaders) {
            if ((stages & s.getStage().bits()) != 0) {
                throw new IllegalArgumentException("More than one " + s.getStage() + " shader.");
            }
            stages |= s.getStage().bits();
        }
        if ((stages & VK_SHADER_STAGE_VERTEX_BIT) == 0) {
            throw new IllegalArgumentException("Graphics pipeline requires a vertex shader.");
        }
    }

    private static Set<DynamicState> collectDynamicStates(PipelineKey key) {
        EnumSet<DynamicState> states = EnumSet.of(DynamicState.ViewPort, DynamicState.Scissor);
        states.addAll(key.getDynamicStates());
        if (isLineRasterization(key)) {
            states.add(DynamicState.LineWidth);
        }
        if (key.getDepthTest().isBiasEnabled()) {
            states.add(DynamicState.DepthBias);
        }
        if (key.isDepthBoundsTest()) {
            states.add(DynamicState.DepthBounds);
        }
        if (key.getStencil().isStencilTestEnabled()) {
            states.add(DynamicState.CompareMask);
            states.add(DynamicState.WriteMask);
            states.add(DynamicState.StencilReference);
        }
        return states;
    }

    private static boolean isLineRasterization(PipelineKey key) {
        if (key.getPolygonMode() == PolygonMode.Line) {
            return true;
        }
        switch (key.getTopology()) {
            case LineList:
            case LineStrip:
            case LineLoop:
            case LineListAdjacency:
            case LineStripAdjacency:
                return true;
            default:
                return false;
        }
    }

    private static VkPipelineRenderingCreateInfo createRendering(MemoryStack stack, PipelineKey key) {
        IntBuffer colorFormats = stack.mallocInt(key.getColorFormats().size());
        for (Format f : key.getColorFormats()) {
            colorFormats.put(f.getEnum(VulkanEnums.instance));
        }
        return VkPipelineRenderingCreateInfo.calloc(stack)
                .sType$Default()
                .colorAttachmentCount(colorFormats.capacity())
                .pColorAttachmentFormats(colorFormats.flip())
                .depthAttachmentFormat(formatEnum(key.getDepthFormat()))
                .stencilAttachmentFormat(formatEnum(key.getStencilFormat()));
    }

    private static int formatEnum(Format format) {
        return format != null ? format.getEnum(VulkanEnums.instance) : VK_FORMAT_UNDEFINED;
    }

    private static VkPipelineVertexInputStateCreateInfo createVertexInput(MemoryStack stack, PipelineKey key) {
        return VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType$Default()
                .pVertexBindingDescriptions(key.getVertexInput().getBindings(stack))
                .pVertexAttributeDescriptions(key.getVertexInput().getAttributes(stack));
    }

    private static VkPipelineInputAssemblyStateCreateInfo createInputAssembly(MemoryStack stack, PipelineKey key) {
        return VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType$Default()
                .topology(key.getTopology().getEnum(VulkanEnums.instance))
                .primitiveRestartEnable(key.isPrimitiveRestart());
    }

    private static VkPipelineViewportStateCreateInfo createViewport(MemoryStack stack) {
        // viewport and scissor are dynamic, so only their counts are given
        return VkPipelineViewportStateCreateInfo.calloc(stack)
                .sType$Default()
                .viewportCount(1)
                .scissorCount(1);
    }

    private static VkPipelineRasterizationStateCreateInfo createRasterization(MemoryStack stack, PipelineKey key) {
        return VkPipelineRasterizationStateCreateInfo.calloc(stack)
                .sType$Default()
                .depthClampEnable(key.isDepthClamp())
                .rasterizerDiscardEnable(key.isRasterizerDiscard())
                .polygonMode(key.getPolygonMode().getEnum())
                .cullMode(key.getCullMode().bits())
                .frontFace(key.getWinding().getEnum())
                .depthBiasEnable(key.getDepthTest().isBiasEnabled())
                .lineWidth(1f);
    }

    private static VkPipelineMultisampleStateCreateInfo createMultisample(MemoryStack stack, PipelineKey key) {
        // VkSampleCountFlagBits values equal the sample counts they stand for
        return VkPipelineMultisampleStateCreateInfo.calloc(stack)
                .sType$Default()
                .rasterizationSamples(key.getSamples());
    }

    private static VkPipelineDepthStencilStateCreateInfo createDepthStencil(MemoryStack stack, PipelineKey key) {
        DepthTest depth = key.getDepthTest();
        StencilTest stencil = key.getStencil();
        VkPipelineDepthStencilStateCreateInfo info = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                .sType$Default()
                .depthTestEnable(depth.isTestEnabled())
                .depthWriteEnable(depth.isWriteEnabled())
                .depthCompareOp(depth.getCompareOp().getEnum())
                .depthBoundsTestEnable(key.isDepthBoundsTest())
                .stencilTestEnable(stencil.isStencilTestEnabled())
                .minDepthBounds(0f)
                .maxDepthBounds(1f);
        // masks and reference are dynamic, so they are left zero
        VulkanEnums e = VulkanEnums.instance;
        info.front()
                .failOp(e.getStencilOpEnum(stencil.getFrontStencilFailOp()))
                .passOp(e.getStencilOpEnum(stencil.getFrontStencilPassOp()))
                .depthFailOp(e.getStencilOpEnum(stencil.getFrontStencilDepthFailOp()))
                .compareOp(stencil.getFrontStencilCompareOp().getEnum());
        info.back()
                .failOp(e.getStencilOpEnum(stencil.getBackStencilFailOp()))
                .passOp(e.getStencilOpEnum(stencil.getBackStencilPassOp()))
                .depthFailOp(e.getStencilOpEnum(stencil.getBackStencilDepthFailOp()))
                .compareOp(stencil.getBackStencilCompareOp().getEnum());
        return info;
    }

    private static VkPipelineColorBlendStateCreateInfo createColorBlend(MemoryStack stack, PipelineKey key) {
        int count = key.getColorFormats().size();
        VkPipelineColorBlendAttachmentState.Buffer attachments = VkPipelineColorBlendAttachmentState.calloc(count, stack);
        // the key's color write mask wins over the mask the blend attachment carries
        ColorBlendAttachment blend = key.getBlend().clone();
        blend.setWriteMask(key.getColorWriteMask());
        for (int i = 0; i < count; i++) {
            blend.fill(attachments.get());
        }
        VkPipelineColorBlendStateCreateInfo info = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType$Default()
                .pAttachments(attachments.flip());
        if (key.getLogicOp() != null) {
            info.logicOpEnable(true).logicOp(key.getLogicOp().getEnum());
        }
        return info;
    }

    private VkPipelineDynamicStateCreateInfo createDynamic(MemoryStack stack) {
        IntBuffer states = stack.mallocInt(dynamicStates.size());
        for (DynamicState s : dynamicStates) {
            states.put(s.getEnum());
        }
        return VkPipelineDynamicStateCreateInfo.calloc(stack)
                .sType$Default()
                .pDynamicStates(states.flip());
    }

    @Override
    public ResourceLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public long getHandle() {
        return handle;
    }

    @Override
    public PipelineLayout getLayout() {
        return layout;
    }

    @Override
    public PipelineBindPoint getBindPoint() {
        return PipelineBindPoint.Graphics;
    }

    @Override
    public boolean isDynamic(DynamicState state) {
        return dynamicStates.contains(state);
    }

    /**
     * @return every state of this pipeline that must be set on the command buffer
     */
    public Set<DynamicState> getDynamicStates() {
        return dynamicStates;
    }

}
