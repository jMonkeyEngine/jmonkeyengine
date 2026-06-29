package com.jme3.vulkan.pipeline.graphics;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.ScissorArea;
import com.jme3.renderer.ViewPortArea;
import com.jme3.scene.Geometry;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.util.natives.CacheableNativeBuilder;
import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.VulkanEnums;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.DescriptorSet;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.VulkanImageView;
import com.jme3.vulkan.pipeline.state.GraphicsState;
import com.jme3.vulkan.material.VulkanMaterial;
import com.jme3.vulkan.material.shader.ShaderModule;
import com.jme3.vulkan.material.technique.VulkanTechnique;
import com.jme3.vulkan.mesh.VulkanMesh;
import com.jme3.vulkan.mesh.VertexInput;
import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pipeline.*;
import com.jme3.util.cache.InlineTimedCache;
import com.jme3.vulkan.pipeline.framebuffer.FrameBuffer;
import com.jme3.vulkan.pipeline.framebuffer.RenderTarget;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import com.jme3.vulkan.util.PNextChain;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK13.*;

public class DynamicGraphicsPipeline extends AbstractVulkanPipeline implements VertexPipeline {

    private final GraphicsState state = new GraphicsState();

    // dynamic render attachments
    private int[] colorFormats;
    private int depthFormat = -1;
    private boolean usingStencilAtt = false;

    protected DynamicGraphicsPipeline(LogicalDevice<?> device) {
        super(device, PipelineBindPoint.Graphics);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DynamicGraphicsPipeline that = (DynamicGraphicsPipeline) o;
        return state.dynamicEquals(that.state)
                && depthFormat == that.depthFormat
                && usingStencilAtt == that.usingStencilAtt
                && Arrays.equals(colorFormats, that.colorFormats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state.dynamicHashCode(), Arrays.hashCode(colorFormats), depthFormat, usingStencilAtt);
    }

    public void setDynamicLineWidth(CommandBuffer cmd, float lineWidth) {
        if (!isDynamic(DynamicState.LineWidth)) {
            throw new IllegalStateException("Line width cannot be set dynamically.");
        }
        vkCmdSetLineWidth(cmd.getBuffer(), lineWidth);
    }

    public void setDynamicViewport(MemoryStack stack, CommandBuffer cmd, int index, float x, float y, float w, float h, float minDepth, float maxDepth) {
        if (!isDynamic(DynamicState.ViewPort)) {
            throw new IllegalStateException("Viewport cannot be set dynamically.");
        }
        VkViewport.Buffer vp = VkViewport.calloc(1, stack)
                .x(x).y(y)
                .width(w).height(h)
                .minDepth(minDepth).maxDepth(maxDepth);
        vkCmdSetViewport(cmd.getBuffer(), index, vp);
    }

    public void setDynamicViewport(MemoryStack stack, CommandBuffer cmd, int index, float x, float y, float w, float h) {
        setDynamicViewport(stack, cmd, index, x, y, w, h, 0f, 1f);
    }

    public void setDynamicScissor(MemoryStack stack, CommandBuffer cmd, int index, int x, int y, int w, int h) {
        if (!isDynamic(DynamicState.Scissor)) {
            throw new IllegalStateException("Scissor cannot be set dynamically.");
        }
        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
        scissor.offset().set(x, y);
        scissor.extent().set(w, h);
        vkCmdSetScissor(cmd.getBuffer(), index, scissor);
    }

    public Subpass getSubpass() {
        return subpass;
    }

    public DynamicGraphicsPipeline getParent() {
        return parent;
    }

    public Flag<Create> getCreateFlags() {
        return createFlags;
    }

    public Collection<ShaderModule> getShaders() {
        return shaders;
    }

    public VertexInput getVertexInput() {
        return vertexInput;
    }

    public Topology getTopology() {
        return topology;
    }

    public boolean isPrimitiveRestart() {
        return primitiveRestart;
    }

    public boolean isDepthTest() {
        return depthTest;
    }

    public boolean isDepthWrite() {
        return depthWrite;
    }

    public boolean isDepthBoundsTest() {
        return depthBoundsTest;
    }

    public boolean isStencilTest() {
        return stencilTest;
    }

    public IntEnum<CompareOp> getDepthCompare() {
        return depthCompare;
    }

    public IntEnum<PolygonMode> getPolygonMode() {
        return polygonMode;
    }

    public Flag<CullMode> getCullMode() {
        return cullMode;
    }

    public IntEnum<FaceWinding> getFaceWinding() {
        return faceWinding;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public boolean isDepthClamp() {
        return depthClamp;
    }

    public boolean isRasterizerDiscard() {
        return rasterizerDiscard;
    }

    public boolean isDepthBias() {
        return depthBias;
    }

    public int getRasterizationSamples() {
        return rasterizationSamples;
    }

    public boolean isSampleShading() {
        return sampleShading;
    }

    public List<ColorBlendAttachment> getBlendAttachments() {
        return Collections.unmodifiableList(blendAttachments);
    }

    public boolean isBlendLogicEnabled() {
        return blendLogicEnabled;
    }

    public IntEnum<LogicOp> getBlendLogic() {
        return blendLogic;
    }

    public List<ViewPortArea> getViewports() {
        return Collections.unmodifiableList(viewports);
    }

    public List<ScissorArea> getScissors() {
        return Collections.unmodifiableList(scissors);
    }

    public Set<Integer> getDynamicStates() {
        return Collections.unmodifiableSet(dynamicStates);
    }

    @Override
    public boolean isDynamic(IntEnum<DynamicState> state) {
        return dynamicStates.contains(state.getEnum());
    }

    @Override
    public Integer getAttributeLocation(String attributeName) {
        return state.getAttributeMappings().get(attributeName);
    }

    @Override
    public PipelineLayout getLayout() {
        return layout;
    }

    public static DynamicGraphicsPipeline build(LogicalDevice<?> device, Consumer<Builder> config) {
        Builder b = new DynamicGraphicsPipeline(device).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Handle implements Disposable {

        private final long object;
        private final DisposableReference ref;

        public Handle(LogicalDevice<?> device) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkGraphicsPipelineCreateInfo.Buffer create = createPipelineInfo(stack);
                LongBuffer id = stack.mallocLong(1);
                vkCreateGraphicsPipelines(device.getNativeObject(), VK_NULL_HANDLE, create, null, id);
                object = id.get(0);
                ref = DisposableManager.reference(DynamicGraphicsPipeline.this);
                device.getReference().addDependent(ref);
            }
        }

        @Override
        public Runnable createDestroyer() {
            return () -> vkDestroyPipeline(device.getNativeObject(), object, null);
        }

        @Override
        public DisposableReference getReference() {
            return ref;
        }

        protected VkGraphicsPipelineCreateInfo.Buffer createPipelineInfo(MemoryStack stack) {
            PNextChain chain = new PNextChain();
            VkGraphicsPipelineCreateInfo.Buffer create = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .flags(createFlags.addIf(parent != null, Create.Derivative).bits())
                    .layout(layout.getNativeObject())
                    .pVertexInputState(createVertexInput(stack))
                    .pInputAssemblyState(createInputAssembly(stack))
                    .pDepthStencilState(createDepthStencil(stack))
                    .pRasterizationState(createRasterization(stack))
                    .pMultisampleState(createMultisample(stack))
                    .pColorBlendState(createColorBlend(stack))
                    .pViewportState(createViewport(stack))
                    .pDynamicState(createDynamic(stack));
            if (subpass != null) {
                create.get(0).renderPass(subpass.getPass().getNativeObject())
                        .subpass(subpass.getPosition());
            } else {
                chain.add(id -> createDynamicRenderingInfo(stack).pNext(id));
            }
            if (!state.getShaders().isEmpty()) {
                fillShaderStages(stack, create.get(0));
            }
            if (parent != null) {
                fillParent(create.get(0));
            }
            chain.add(id -> create.get(0).pNext(id));
            return create;
        }

        protected void fillParent(VkGraphicsPipelineCreateInfo struct) {
            struct.basePipelineHandle(parent.getNativeObject());
        }

        protected void fillShaderStages(MemoryStack stack, VkGraphicsPipelineCreateInfo struct) {
            VkPipelineShaderStageCreateInfo.Buffer stageBuf = VkPipelineShaderStageCreateInfo
                    .calloc(state.getShaders().size(), stack);
            for (ShaderModule s : state.getShaders().values()) {
                stageBuf.get().sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                        .module();
            }
            struct.stageCount(stageBuf.limit()).pStages(stageBuf.flip());
        }

        protected VkPipelineRenderingCreateInfo createDynamicRenderingInfo(MemoryStack stack) {
            assert colorFormats != null : "Attachment formats not initialized.";
            VkPipelineRenderingCreateInfo info = VkPipelineRenderingCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO);
            IntBuffer colorFmtBuf = stack.mallocInt(colorFormats.length);
            for (int f : colorFormats) {
                colorFmtBuf.put(f);
            }
            info.colorAttachmentCount(colorFormats.length)
                    .pColorAttachmentFormats(colorFmtBuf.flip());
            if (depthFormat >= 0) {
                info.depthAttachmentFormat(depthFormat);
                if (usingStencilAtt) {
                    info.stencilAttachmentFormat(depthFormat);
                }
            }
            return info;
        }

        protected VkPipelineVertexInputStateCreateInfo createVertexInput(MemoryStack stack) {
            return VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                    .pVertexBindingDescriptions(vertexInput.getBindings(stack))
                    .pVertexAttributeDescriptions(vertexInput.getAttributes(stack));
        }

        protected VkPipelineInputAssemblyStateCreateInfo createInputAssembly(MemoryStack stack) {
            return VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(topology.getEnum(VulkanEnums.instance))
                    .primitiveRestartEnable(primitiveRestart);
        }

        protected VkPipelineDepthStencilStateCreateInfo createDepthStencil(MemoryStack stack) {
            return VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(state.isDepthTest())
                    .depthWriteEnable(state.isDepthWrite())
                    .depthBoundsTestEnable(state.isDepthBoundsTest())
                    .stencilTestEnable(state.isStencilTest())
                    .depthCompareOp(state.getDepthCompare().getEnum());
        }

        protected VkPipelineRasterizationStateCreateInfo createRasterization(MemoryStack stack) {
            VkPipelineRasterizationStateCreateInfo raster = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(state.isDepthClamp())
                    .rasterizerDiscardEnable(state.isRasterizerDiscard())
                    .polygonMode(state.getPolygonMode().getEnum())
                    .lineWidth(state.getLineWidth())
                    .cullMode(state.getCullMode().bits())
                    .frontFace(faceWinding.getEnum())
                    .depthBiasEnable(state.isDepthBiasEnabled());
            if (state.isDepthBiasEnabled()) {
                raster.depthBiasConstantFactor(state.getDepthBias().getConstant())
                        .depthBiasSlopeFactor(state.getDepthBias().getSlope())
                        .depthBiasClamp(state.getDepthBias().getClamp());
            }
            return raster;
        }

        protected VkPipelineMultisampleStateCreateInfo createMultisample(MemoryStack stack) {
            return VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .sampleShadingEnable(sampleShading)
                    .rasterizationSamples(rasterizationSamples);
        }

        protected VkPipelineColorBlendStateCreateInfo createColorBlend(MemoryStack stack) {
            VkPipelineColorBlendAttachmentState.Buffer attBuf = VkPipelineColorBlendAttachmentState
                    .calloc(blendAttachments.size(), stack);
            for (int i = 0; i < blendAttachments.size(); i++) {
                ColorBlendAttachment a = blendAttachments.get(i);
                if (a == null) {
                    throw new NullPointerException("Blend attachment " + i + " is not defined.");
                }
                a.fill(attBuf.get());
            }
            attBuf.flip();
            return VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(blendLogicEnabled)
                    .logicOp(blendLogic.getEnum())
                    .pAttachments(attBuf);
        }

        @SuppressWarnings("resource")
        protected VkPipelineViewportStateCreateInfo createViewport(MemoryStack stack) {
            VkViewport.Buffer vpBuf = VkViewport.malloc(1, stack);
            VkRect2D.Buffer scissorBuf = VkRect2D.malloc(1, stack);
            return VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .pViewports(vpBuf)
                    .pScissors(scissorBuf);
        }

        protected VkPipelineDynamicStateCreateInfo createDynamic(MemoryStack stack) {
            IntBuffer stateBuf = stack.mallocInt(dynamicStates.size());
            for (Integer t : dynamicStates) {
                stateBuf.put(t);
            }
            stateBuf.flip();
            return VkPipelineDynamicStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                    .pDynamicStates(stateBuf);
        }

    }

    private static class State extends GraphicsState {

        private DynamicGraphicsPipeline parent;
        private Flag<Create> createFlags = Flag.empty();
        private PipelineLayout layout;
        private int[] colorFormats;
        private int depthFormat = -1;
        private boolean usingStencilAtt = false;

        public void applyFrameBuffer(FrameBuffer fbo) {
            colorFormats = new int[fbo.getColorTargets().size()];
            for (ListIterator<RenderTarget> it = fbo.getColorTargets().listIterator(); it.hasNext();) {
                RenderTarget t = it.next();
                colorFormats[it.previousIndex()] = t.getView().getImage().getFormat().getEnum(VulkanEnums.instance);
            }
            if (fbo.isUsingDepth()) {
                depthFormat = fbo.getDepthStencilTarget().getView().getImage().getFormat().getEnum(VulkanEnums.instance);
                usingStencilAtt = fbo.isUsingStencil();
            } else {
                depthFormat = -1;
                usingStencilAtt = false;
            }
        }

        public void createLayoutFromShaderSets(LogicalDevice<?> device, InlineTimedCache<> cache, CommandBuffer cmd) {
            layout = PipelineLayout.build(device, l -> {
                l.setCache(cache);
                cmd.fillPipelineLayoutSets(l);
            });
        }

        public void setParent(DynamicGraphicsPipeline parent) {
            this.parent = parent;
        }

        public void setCreateFlags(Flag<Create> createFlags) {
            this.createFlags = createFlags;
        }

        public void setLayout(PipelineLayout layout) {
            this.layout = layout;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            State that = (State)o;
            return Flag.equals(createFlags, that.createFlags)
                    && layout == that.layout
                    && depthFormat == that.depthFormat
                    && usingStencilAtt == that.usingStencilAtt
                    && Arrays.equals(colorFormats, that.colorFormats)
                    && equals(that);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(),
                    createFlags.bits(),
                    System.identityHashCode(layout),
                    Arrays.hashCode(colorFormats),
                    depthFormat, usingStencilAtt);
        }

        @Override
        public boolean dynamicEquals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            State that = (State)o;
            return Flag.equals(createFlags, that.createFlags)
                    && layout == that.layout
                    && depthFormat == that.depthFormat
                    && usingStencilAtt == that.usingStencilAtt
                    && Arrays.equals(colorFormats, that.colorFormats)
                    && super.dynamicEquals(that);
        }

        @Override
        public int dynamicHashCode() {
            return Objects.hash(super.dynamicHashCode(),
                    createFlags.bits(),
                    System.identityHashCode(layout),
                    Arrays.hashCode(colorFormats),
                    depthFormat, usingStencilAtt);
        }

        @Override
        public State clone() {
            return (State)super.clone();
        }

    }

    public class Builder extends CacheableNativeBuilder<Pipeline, DynamicGraphicsPipeline> {

        private InlineTimedCache<PipelineLayout> layoutCache;
        private InlineTimedCache<ShaderModule> shaderCache;
        private InlineTimedCache<DescriptorSetLayout> setCache;
        private FrameBuffer<VulkanImageView> frameBuffer;

        @Override
        public DynamicGraphicsPipeline build() {
            Objects.requireNonNull(layout, "Pipeline layout not specified.");
            Objects.requireNonNull(vertexInput, "Vertex input not specified.");
            if (viewports.isEmpty()) setNextViewPort(new ViewPortArea(128, 128));
            if (scissors.isEmpty()) setNextScissor(new ScissorArea());
            if (subpass == null) {
                assert frameBuffer != null : "Frame buffer required for dynamic rendering.";
                colorFormats = new int[frameBuffer.getColorTargets().size() + 2];
                int i = 0;
                for (RenderTarget t : frameBuffer.getColorTargets()) {
                    colorFormats[i++] = t.getView().getImage().getFormat().getEnum(VulkanEnums.instance);
                }
                if (frameBuffer.getDepthStencilTarget() != null) {
                    depthFormat = frameBuffer.getDepthStencilTarget().getView().getImage().getFormat().getEnum(VulkanEnums.instance);
                    usingStencilAtt = frameBuffer.isUsingStencil();
                }
            }
            return super.build();
        }

        @Override
        protected void construct() {
            VkGraphicsPipelineCreateInfo.Buffer create = createPipelineInfo();
            LongBuffer id = stack.mallocLong(1);
            vkCreateGraphicsPipelines(device.getNativeObject(), VK_NULL_HANDLE, create, null, id);
            object = id.get(0);
            ref = DisposableManager.reference(DynamicGraphicsPipeline.this);
            device.getReference().addDependent(ref);
        }

        @Override
        protected DynamicGraphicsPipeline getBuildTarget() {
            return DynamicGraphicsPipeline.this;
        }

        protected VkGraphicsPipelineCreateInfo.Buffer createPipelineInfo() {
            PNextChain chain = new PNextChain();
            VkGraphicsPipelineCreateInfo.Buffer create = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .flags(createFlags.addIf(parent != null, Create.Derivative).bits())
                    .layout(layout.getNativeObject())
                    .pVertexInputState(createVertexInput())
                    .pInputAssemblyState(createInputAssembly())
                    .pDepthStencilState(createDepthStencil())
                    .pRasterizationState(createRasterization())
                    .pMultisampleState(createMultisample())
                    .pColorBlendState(createColorBlend())
                    .pViewportState(createViewport())
                    .pDynamicState(createDynamic());
            if (subpass != null) {
                create.get(0).renderPass(subpass.getPass().getNativeObject())
                        .subpass(subpass.getPosition());
            } else {
                chain.add(id -> createDynamicRenderingInfo().pNext(id));
            }
            if (!state.getShaders().isEmpty()) {
                fillShaderStages(create.get(0));
            }
            if (parent != null) {
                fillParent(create.get(0));
            }
            chain.add(id -> create.get(0).pNext(id));
            return create;
        }

        protected void fillParent(VkGraphicsPipelineCreateInfo struct) {
            struct.basePipelineHandle(parent.getNativeObject());
        }

        protected void fillShaderStages(VkGraphicsPipelineCreateInfo struct) {
            VkPipelineShaderStageCreateInfo.Buffer stageBuf = VkPipelineShaderStageCreateInfo.calloc(shaders.size(), stack);
            for (ShaderModule s : shaders) {
                s.fill(stack, stageBuf.get().sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO));
            }
            stageBuf.flip();
            struct.stageCount(stageBuf.limit()).pStages(stageBuf);
        }

        protected VkPipelineRenderingCreateInfo createDynamicRenderingInfo() {
            assert colorFormats != null : "Attachment formats not initialized.";
            VkPipelineRenderingCreateInfo info = VkPipelineRenderingCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO);
            IntBuffer colorFmtBuf = stack.mallocInt(colorFormats.length);
            for (int f : colorFormats) {
                colorFmtBuf.put(f);
            }
            info.colorAttachmentCount(colorFormats.length)
                .pColorAttachmentFormats(colorFmtBuf.flip());
            if (depthFormat >= 0) {
                info.depthAttachmentFormat(depthFormat);
                if (usingStencilAtt) {
                    info.stencilAttachmentFormat(depthFormat);
                }
            }
            return info;
        }

        protected VkPipelineVertexInputStateCreateInfo createVertexInput() {
            return VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                    .pVertexBindingDescriptions(vertexInput.getBindings(stack))
                    .pVertexAttributeDescriptions(vertexInput.getAttributes(stack));
        }

        protected VkPipelineInputAssemblyStateCreateInfo createInputAssembly() {
            return VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(topology.getEnum(VulkanEnums.instance))
                    .primitiveRestartEnable(primitiveRestart);
        }

        protected VkPipelineDepthStencilStateCreateInfo createDepthStencil() {
            return VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(depthTest)
                    .depthWriteEnable(depthWrite)
                    .depthBoundsTestEnable(depthBoundsTest)
                    .stencilTestEnable(stencilTest)
                    .depthCompareOp(depthCompare.getEnum());
        }

        protected VkPipelineRasterizationStateCreateInfo createRasterization() {
            return VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(depthClamp)
                    .rasterizerDiscardEnable(rasterizerDiscard)
                    .polygonMode(polygonMode.getEnum())
                    .lineWidth(lineWidth)
                    .cullMode(cullMode.bits())
                    .frontFace(faceWinding.getEnum())
                    .depthBiasEnable(depthBias);
        }

        protected VkPipelineMultisampleStateCreateInfo createMultisample() {
            return VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .sampleShadingEnable(sampleShading)
                    .rasterizationSamples(rasterizationSamples);
        }

        protected VkPipelineColorBlendStateCreateInfo createColorBlend() {
            VkPipelineColorBlendAttachmentState.Buffer attBuf = VkPipelineColorBlendAttachmentState
                    .calloc(blendAttachments.size(), stack);
            for (int i = 0; i < blendAttachments.size(); i++) {
                ColorBlendAttachment a = blendAttachments.get(i);
                if (a == null) {
                    throw new NullPointerException("Blend attachment " + i + " is not defined.");
                }
                a.fill(attBuf.get());
            }
            attBuf.flip();
            return VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(blendLogicEnabled)
                    .logicOp(blendLogic.getEnum())
                    .pAttachments(attBuf);
        }

        @SuppressWarnings("resource")
        protected VkPipelineViewportStateCreateInfo createViewport() {
            VkViewport.Buffer vpBuf = VkViewport.calloc(viewports.size(), stack);
            for (ViewPortArea v : viewports) {
                float x = v.getWidth() * v.getLeft();
                float y = v.getHeight() * v.getTop();
                float w = v.getWidth() * v.getRight() - x;
                float h = v.getHeight() * v.getBottom() - y;
                vpBuf.get().x(x).y(y).width(w).height(h).minDepth(v.getMaxDepth()).maxDepth(v.getMaxDepth());
            }
            VkRect2D.Buffer scissorBuf = VkRect2D.calloc(scissors.size(), stack);
            for (ScissorArea s : scissors) {
                VkRect2D e = scissorBuf.get();
                e.offset().set(s.getX(), s.getY());
                e.extent().set(s.getWidth(), s.getHeight());
            }
            return VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .pViewports(vpBuf.flip())
                    .pScissors(scissorBuf.flip());
        }

        protected VkPipelineDynamicStateCreateInfo createDynamic() {
            IntBuffer stateBuf = stack.mallocInt(dynamicStates.size());
            for (Integer t : dynamicStates) {
                stateBuf.put(t);
            }
            stateBuf.flip();
            return VkPipelineDynamicStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                    .pDynamicStates(stateBuf);
        }

        public void applyGeometry(AssetManager assetManager, Geometry geometry, String technique) {
            VulkanMaterial mat = (VulkanMaterial)geometry.getMaterial();
            applyGeometry(assetManager,
                    (VulkanMesh)geometry.getMesh(),
                    (VulkanMaterial)geometry.getMaterial(),
                    (VulkanTechnique)mat.getTechnique(technique));
        }

        public void applyGeometry(AssetManager assetManager, VulkanMesh mesh, VulkanMaterial material, VulkanTechnique tech) {
            setLayout(tech.getLayout(device, layoutCache, setCache));
            addShaders(tech.getShaders(device, assetManager, shaderCache, material));
            setVertexInput(mesh.declareVertexInput(DynamicGraphicsPipeline.this));
            setTopology(mesh.getTopology());
            attributeLocations.putAll(tech.getAttributeLocations());
        }

        public void applyPipelineState(GraphicsState state) {
            setDepthTest(state.isDepthTest());
        }

        public void createLayoutFromSets(DescriptorSet[] sets) {
            setLayout(PipelineLayout.build(device, l -> {
                l.setCache(layoutCache);
                for (DescriptorSet s : sets) {
                    l.addSetLayout(s.getTargetLocation(), s.getLayout());
                }
            }));
        }

        public void setLayout(PipelineLayout layout) {
            DynamicGraphicsPipeline.this.layout = layout;
        }

        public void setSubpass(Subpass subpass) {
            DynamicGraphicsPipeline.this.subpass = subpass;
        }

        /**
         * Sets the vertex input that describes how vertex attributes are laid
         * out in the pipeline. Only meshes that match the described vertex input
         * can be rendered by this pipeline.
         *
         * @param vertexInput vertex input description
         */
        public void setVertexInput(VertexInput vertexInput) {
            DynamicGraphicsPipeline.this.vertexInput = vertexInput;
        }

        public void addShader(ShaderModule shader) {
            shaders.add(shader);
        }

        public void addShaders(Collection<ShaderModule> shaders) {
            DynamicGraphicsPipeline.this.shaders.addAll(shaders);
        }

        /**
         * Adds a blend attachment to this pipeline that dictates how the corresponding
         * color attachment is blended during the blend stage.
         *
         * @param attachment blend attachment
         * @return index of the blend attachment, which is also the index of the
         * affected color attachment
         */
        public int addBlendAttachment(ColorBlendAttachment attachment) {
            blendAttachments.add(attachment);
            return blendAttachments.size() - 1;
        }

        public void setBlendAttachment(int i, ColorBlendAttachment attachment) {
            blendAttachments.set(i, attachment);
        }

        /**
         * Sets the pipeline layout cache. When generating a pipeline from a geometry,
         * the layout cache ensures existing layouts are used instead of creating
         * new ones when possible. Otherwise a new layout is created for each geometry.
         *
         * @param layout layout cache (can be null)
         */
        public void setLayoutCache(InlineTimedCache<PipelineLayout> layout) {
            this.layoutCache = layout;
        }

        /**
         * Sets the shader cache. When generating a pipeline from a geometry, the shader cache
         * ensures existing shaders are used instead of creating new ones when possible.
         * Otherwise a new shader is created for each geometry.
         *
         * @param shaderCache shader cache (can be null)
         */
        public void setShaderCache(InlineTimedCache<ShaderModule> shaderCache) {
            this.shaderCache = shaderCache;
        }

        /**
         * Sets the descriptor set layout cache. When generating a pipeline from a geometry,
         * the set layout cache ensures existing layouts are used when possible. Otherwise
         * new layouts are created for each geometry.
         *
         * @param setCache descriptor set layout cache
         */
        public void setSetLayoutCache(InlineTimedCache<DescriptorSetLayout> setCache) {
            this.setCache = setCache;
        }

        /**
         * Enables or disables the specified dynamic state. If a dynamic state is enabled,
         * the corresponding render property must be set in the command buffer before the
         * pipeline is used for rendering.
         *
         * @param state dynamic state
         * @param enable enable or disable the state
         */
        public void setDynamic(IntEnum<DynamicState> state, boolean enable) {
            if (enable) dynamicStates.add(state.getEnum());
            else dynamicStates.remove(state.getEnum());
        }

        public void setNextViewPort(ViewPortArea vp) {
            viewports.add(vp);
        }

        public void setNextScissor(ScissorArea scissor) {
            scissors.add(scissor);
        }

        /**
         * Sets the location of the named attribute in the vertex shader.
         *
         * @param attribute attribute name
         * @param location attribute location (as specified in the shader source code)
         */
        public void setAttributeLocation(String attribute, int location) {
            attributeLocations.put(attribute, location);
        }

        @Deprecated
        public void setAttributeLocation(GlVertexBuffer.Type attribute, int location) {
            setAttributeLocation(attribute.name(), location);
        }

        public void setAttributeLocations(Map<String, Integer> locations) {
            attributeLocations.putAll(locations);
        }

        /**
         * Sets the parent pipeline of this pipeline. Using a parent pipeline can have better
         * performance if they share similar properties.
         *
         * @param parent parent pipeline (can be null)
         */
        public void setParent(DynamicGraphicsPipeline parent) {
            if (parent != null && !parent.getCreateFlags().contains(Create.AllowDerivatives)) {
                throw new IllegalArgumentException("Parent pipeline must allow derivatives.");
            }
            DynamicGraphicsPipeline.this.parent = parent;
        }

        public void setCreateFlags(Flag<Create> flags) {
            DynamicGraphicsPipeline.this.createFlags = flags;
        }

        /**
         * Sets the mesh topology to render by. When generating a pipeline from geometry, this
         * is automatically assigned from the geometry's mesh.
         *
         * @param topology mesh topology
         */
        public void setTopology(Topology topology) {
            DynamicGraphicsPipeline.this.topology = topology;
        }

        public void setPrimitiveRestart(boolean primitiveRestart) {
            DynamicGraphicsPipeline.this.primitiveRestart = primitiveRestart;
        }

        /**
         * Enables testing against the depth attachment.
         *
         * @param depthTest true to enable depth testing
         */
        public void setDepthTest(boolean depthTest) {
            DynamicGraphicsPipeline.this.depthTest = depthTest;
        }

        /**
         * Enables writing to the depth attachment.
         *
         * @param depthWrite true to enable depth writing
         */
        public void setDepthWrite(boolean depthWrite) {
            DynamicGraphicsPipeline.this.depthWrite = depthWrite;
        }

        public void setDepthBoundsTest(boolean depthBoundsTest) {
            DynamicGraphicsPipeline.this.depthBoundsTest = depthBoundsTest;
        }

        public void setStencilTest(boolean stencilTest) {
            DynamicGraphicsPipeline.this.stencilTest = stencilTest;
        }

        public void setDepthCompare(IntEnum<CompareOp> depthCompare) {
            DynamicGraphicsPipeline.this.depthCompare = depthCompare;
        }

        public void setPolygonMode(IntEnum<PolygonMode> polygonMode) {
            DynamicGraphicsPipeline.this.polygonMode = polygonMode;
        }

        public void setCullMode(Flag<CullMode> cullMode) {
            DynamicGraphicsPipeline.this.cullMode = cullMode;
        }

        public void setFaceWinding(IntEnum<FaceWinding> faceWinding) {
            DynamicGraphicsPipeline.this.faceWinding = faceWinding;
        }

        public void setLineWidth(float lineWidth) {
            DynamicGraphicsPipeline.this.lineWidth = lineWidth;
        }

        public void setDepthClamp(boolean depthClamp) {
            DynamicGraphicsPipeline.this.depthClamp = depthClamp;
        }

        public void setRasterizerDiscard(boolean rasterizerDiscard) {
            DynamicGraphicsPipeline.this.rasterizerDiscard = rasterizerDiscard;
        }

        public void setDepthBias(boolean depthBias) {
            DynamicGraphicsPipeline.this.depthBias = depthBias;
        }

        public void setRasterizationSamples(int rasterizationSamples) {
            DynamicGraphicsPipeline.this.rasterizationSamples = rasterizationSamples;
        }

        public void setSampleShading(boolean sampleShading) {
            DynamicGraphicsPipeline.this.sampleShading = sampleShading;
        }

        public void setBlendLogicEnabled(boolean blendLogicEnabled) {
            DynamicGraphicsPipeline.this.blendLogicEnabled = blendLogicEnabled;
        }

        public void setBlendLogic(IntEnum<LogicOp> blendLogic) {
            DynamicGraphicsPipeline.this.blendLogic = blendLogic;
        }

        public void setFrameBuffer(FrameBuffer<VulkanImageView> frameBuffer) {
            this.frameBuffer = frameBuffer;
        }

    }

    public static class ViewportInfo {

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

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ViewportInfo that = (ViewportInfo) o;
            return Float.compare(x, that.x) == 0
                    && Float.compare(y, that.y) == 0
                    && Float.compare(w, that.w) == 0
                    && Float.compare(h, that.h) == 0
                    && Float.compare(min, that.min) == 0
                    && Float.compare(max, that.max) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, w, h, min, max);
        }

    }

    public static class ScissorInfo {

        public final int x, y, w, h;

        public ScissorInfo(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ScissorInfo that = (ScissorInfo) o;
            return x == that.x && y == that.y && w == that.w && h == that.h;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, w, h);
        }

    }

}
