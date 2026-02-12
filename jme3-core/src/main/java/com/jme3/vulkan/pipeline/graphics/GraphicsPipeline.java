package com.jme3.vulkan.pipeline.graphics;

import com.jme3.asset.AssetManager;
import com.jme3.material.RenderState;
import com.jme3.renderer.ScissorArea;
import com.jme3.renderer.ViewPortArea;
import com.jme3.scene.Geometry;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.util.natives.CacheableNativeBuilder;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.VulkanMaterial;
import com.jme3.vulkan.material.shader.ShaderModule;
import com.jme3.vulkan.material.technique.VulkanTechnique;
import com.jme3.vulkan.mesh.MeshLayout;
import com.jme3.vulkan.mesh.VulkanMesh;
import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.pipeline.cache.Cache;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import com.jme3.vulkan.util.VulkanEnums;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK10.*;

public class GraphicsPipeline extends AbstractVulkanPipeline implements VertexPipeline {

    private PipelineLayout layout;
    private Subpass subpass;
    private GraphicsPipeline parent;
    private Flag<Create> createFlags = Flag.empty();

    // shaders
    private final Collection<ShaderModule> shaders = new ArrayList<>();

    // vertex input
    private MeshLayout mesh;
    private final Map<String, Integer> attributeLocations = new HashMap<>();

    // input assembly
    private IntEnum<Topology> topology = Topology.TriangleList;
    private boolean primitiveRestart = false;

    // depth stencil
    private boolean depthTest = true;
    private boolean depthWrite = true;
    private boolean depthBoundsTest = false;
    private boolean stencilTest = false;
    private IntEnum<CompareOp> depthCompare = CompareOp.LessOrEqual;

    // rasterization
    private IntEnum<PolygonMode> polygonMode = PolygonMode.Fill;
    private Flag<CullMode> cullMode = CullMode.Back;
    private IntEnum<FaceWinding> faceWinding = FaceWinding.Clockwise;
    private float lineWidth = 1f;
    private boolean depthClamp = false;
    private boolean rasterizerDiscard = false;
    private boolean depthBias = false;

    // multisample
    private int rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;
    private boolean sampleShading = false;

    // color blend
    private final List<ColorBlendAttachment> blendAttachments = new ArrayList<>();
    private boolean blendLogicEnabled = false;
    private IntEnum<LogicOp> blendLogic = LogicOp.Copy;

    // viewport
    private final List<ViewPortArea> viewports = new ArrayList<>();
    private final List<ScissorArea> scissors = new ArrayList<>();

    // dynamic
    private final Set<Integer> dynamicStates = new HashSet<>();

    protected GraphicsPipeline(LogicalDevice<?> device) {
        super(device, PipelineBindPoint.Graphics);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphicsPipeline that = (GraphicsPipeline) o;
        return layout == that.layout
                && subpass == that.subpass
                && primitiveRestart == that.primitiveRestart
                && depthTest == that.depthTest
                && depthWrite == that.depthWrite
                && depthBoundsTest == that.depthBoundsTest
                && stencilTest == that.stencilTest
                && depthClamp == that.depthClamp
                && rasterizerDiscard == that.rasterizerDiscard
                && (isDynamic(DynamicState.DepthBias) || depthBias == that.depthBias)
                && rasterizationSamples == that.rasterizationSamples
                && sampleShading == that.sampleShading
                && blendLogicEnabled == that.blendLogicEnabled
                && Flag.is(createFlags, that.createFlags)
                && Flag.is(cullMode, that.cullMode)
                && IntEnum.is(topology, that.topology)
                && IntEnum.is(depthCompare, that.depthCompare)
                && IntEnum.is(polygonMode, that.polygonMode)
                && IntEnum.is(faceWinding, that.faceWinding)
                && IntEnum.is(blendLogic, that.blendLogic)
                && (isDynamic(DynamicState.LineWidth) || Float.compare(lineWidth, that.lineWidth) == 0)
                && Objects.equals(mesh, that.mesh)
                && Objects.equals(shaders, that.shaders)
                && Objects.equals(attributeLocations, that.attributeLocations)
                && Objects.equals(blendAttachments, that.blendAttachments)
                && ((isDynamic(DynamicState.ViewPort) && viewports.size() == that.viewports.size()) || Objects.equals(viewports, that.viewports))
                && ((isDynamic(DynamicState.Scissor) && scissors.size() == that.scissors.size()) || Objects.equals(scissors, that.scissors))
                && Objects.equals(dynamicStates, that.dynamicStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subpass, parent, createFlags, shaders, mesh, attributeLocations, topology, primitiveRestart,
                depthTest, depthWrite, depthBoundsTest, stencilTest, depthCompare, polygonMode, cullMode, faceWinding,
                lineWidth, depthClamp, rasterizerDiscard, rasterizationSamples, sampleShading, blendAttachments,
                blendLogicEnabled, blendLogic, dynamicStates,
                (isDynamic(DynamicState.ViewPort) ? viewports.size() : viewports),
                (isDynamic(DynamicState.Scissor) ? scissors.size() : scissors),
                (isDynamic(DynamicState.DepthBias) ? 0 : depthBias));
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

    public GraphicsPipeline getParent() {
        return parent;
    }

    public Flag<Create> getCreateFlags() {
        return createFlags;
    }

    public Collection<ShaderModule> getShaders() {
        return shaders;
    }

    public MeshLayout getMesh() {
        return mesh;
    }

    public IntEnum<Topology> getTopology() {
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
    public int getNumAttributes() {
        return attributeLocations.size();
    }

    @Override
    public Integer getAttributeLocation(String attributeName) {
        return attributeLocations.get(attributeName);
    }

    @Override
    public PipelineLayout getLayout() {
        return layout;
    }

    public static GraphicsPipeline build(LogicalDevice<?> device, Consumer<Builder> config) {
        Builder b = new GraphicsPipeline(device).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends CacheableNativeBuilder<Pipeline, GraphicsPipeline> {

        private Cache<PipelineLayout> layoutCache;
        private Cache<ShaderModule> shaderCache;
        private Cache<DescriptorSetLayout> setCache;

        @Override
        public GraphicsPipeline build() {
            Objects.requireNonNull(layout, "Pipeline layout not specified.");
            Objects.requireNonNull(mesh, "Mesh layout not specified.");
            if (viewports.isEmpty()) setNextViewPort(new ViewPortArea());
            if (scissors.isEmpty()) setNextScissor(new ScissorArea());
            return super.build();
        }

        @Override
        protected void construct() {
            VkGraphicsPipelineCreateInfo.Buffer create = createPipelineInfo();
            LongBuffer id = stack.mallocLong(1);
            vkCreateGraphicsPipelines(device.getNativeObject(), VK_NULL_HANDLE, create, null, id);
            object = id.get(0);
            ref = DisposableManager.reference(GraphicsPipeline.this);
            device.getReference().addDependent(ref);
        }

        @Override
        protected GraphicsPipeline getBuildTarget() {
            return GraphicsPipeline.this;
        }

        protected VkGraphicsPipelineCreateInfo.Buffer createPipelineInfo() {
            VkGraphicsPipelineCreateInfo.Buffer create = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .flags(createFlags.addIf(parent != null, Create.Derivative).bits())
                    .layout(layout.getNativeObject())
                    .renderPass(subpass.getPass().getNativeObject())
                    .subpass(subpass.getPosition())
                    .pVertexInputState(createVertexInput())
                    .pInputAssemblyState(createInputAssembly())
                    .pDepthStencilState(createDepthStencil())
                    .pRasterizationState(createRasterization())
                    .pMultisampleState(createMultisample())
                    .pColorBlendState(createColorBlend())
                    .pViewportState(createViewport())
                    .pDynamicState(createDynamic());
            if (!shaders.isEmpty()) {
                fillShaderStages(create.get(0));
            }
            if (parent != null) {
                fillParent(create.get(0));
            }
            return create.flip();
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

        protected VkPipelineVertexInputStateCreateInfo createVertexInput() {
            return VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                    .pVertexBindingDescriptions(getBindingInfo(stack, mesh))
                    .pVertexAttributeDescriptions(getAttributeInfo(stack, mesh));
        }

        protected VkPipelineInputAssemblyStateCreateInfo createInputAssembly() {
            return VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(topology.getEnum())
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
                vpBuf.get().x(v.getX()).y(v.getY()).width(v.getWidth()).height(v.getHeight())
                        .minDepth(v.getMaxDepth()).maxDepth(v.getMaxDepth());
            }
            vpBuf.flip();
            VkRect2D.Buffer scissorBuf = VkRect2D.calloc(scissors.size(), stack);
            for (ScissorArea s : scissors) {
                VkRect2D e = scissorBuf.get();
                e.offset().set(s.getX(), s.getY());
                e.extent().set(s.getWidth(), s.getHeight());
            }
            scissorBuf.flip();
            return VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .pViewports(vpBuf)
                    .pScissors(scissorBuf);
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
            applyGeometry(assetManager, (VulkanMesh)geometry.getMesh(), (VulkanMaterial)geometry.getMaterial(), mat.getTechnique(technique));
        }

        public void applyGeometry(AssetManager assetManager, VulkanMesh mesh, VulkanMaterial material, VulkanTechnique tech) {
            setLayout(tech.getLayout(device, layoutCache, setCache));
            addShaders(tech.getShaders(device, assetManager, shaderCache, material));
            setMeshLayout(mesh.getLayout());
            setTopology(mesh.getTopology());
            attributeLocations.putAll(tech.getAttributeLocations());
        }

        public void applyRenderState(RenderState state) {
            addBlendAttachment(new ColorBlendAttachment(state));
            setPolygonMode(state.isWireframe() ? PolygonMode.Line : PolygonMode.Fill);
            setCullMode(VulkanEnums.faceCull(state.getFaceCullMode()));
            setDepthCompare(VulkanEnums.depthFunc(state.getDepthFunc()));
            setStencilTest(state.isStencilTest());
            setLineWidth(state.getLineWidth());
            setDepthTest(state.isDepthTest());
            setDepthWrite(state.isDepthWrite());
        }

        public void setLayout(PipelineLayout layout) {
            GraphicsPipeline.this.layout = layout;
        }

        public void setSubpass(Subpass subpass) {
            GraphicsPipeline.this.subpass = subpass;
        }

        public void setMeshLayout(MeshLayout mesh) {
            GraphicsPipeline.this.mesh = mesh;
        }

        public void addShader(ShaderModule shader) {
            shaders.add(shader);
        }

        public void addShaders(Collection<ShaderModule> shaders) {
            GraphicsPipeline.this.shaders.addAll(shaders);
        }

        public int addBlendAttachment(ColorBlendAttachment attachment) {
            blendAttachments.add(attachment);
            return blendAttachments.size() - 1;
        }

        public void setBlendAttachment(int i, ColorBlendAttachment attachment) {
            blendAttachments.set(i, attachment);
        }

        public void setLayoutCache(Cache<PipelineLayout> layout) {
            this.layoutCache = layoutCache;
        }

        public void setShaderCache(Cache<ShaderModule> shaderCache) {
            this.shaderCache = shaderCache;
        }

        public void setSetLayoutCache(Cache<DescriptorSetLayout> setCache) {
            this.setCache = setCache;
        }

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

        public void setAttributeLocation(String attribute, int location) {
            attributeLocations.put(attribute, location);
        }

        public void setAttributeLocation(GlVertexBuffer.Type attribute, int location) {
            setAttributeLocation(attribute.name(), location);
        }

        public void setAttributeLocations(Map<String, Integer> locations) {
            attributeLocations.putAll(locations);
        }

        public void setParent(GraphicsPipeline parent) {
            if (!parent.getCreateFlags().contains(Create.AllowDerivatives)) {
                throw new IllegalArgumentException("Parent pipeline must allow derivatives.");
            }
            GraphicsPipeline.this.parent = parent;
        }

        public void setCreateFlags(Flag<Create> flags) {
            GraphicsPipeline.this.createFlags = flags;
        }

        public void setTopology(IntEnum<Topology> topology) {
            GraphicsPipeline.this.topology = topology;
        }

        public void setPrimitiveRestart(boolean primitiveRestart) {
            GraphicsPipeline.this.primitiveRestart = primitiveRestart;
        }

        public void setDepthTest(boolean depthTest) {
            GraphicsPipeline.this.depthTest = depthTest;
        }

        public void setDepthWrite(boolean depthWrite) {
            GraphicsPipeline.this.depthWrite = depthWrite;
        }

        public void setDepthBoundsTest(boolean depthBoundsTest) {
            GraphicsPipeline.this.depthBoundsTest = depthBoundsTest;
        }

        public void setStencilTest(boolean stencilTest) {
            GraphicsPipeline.this.stencilTest = stencilTest;
        }

        public void setDepthCompare(IntEnum<CompareOp> depthCompare) {
            GraphicsPipeline.this.depthCompare = depthCompare;
        }

        public void setPolygonMode(IntEnum<PolygonMode> polygonMode) {
            GraphicsPipeline.this.polygonMode = polygonMode;
        }

        public void setCullMode(Flag<CullMode> cullMode) {
            GraphicsPipeline.this.cullMode = cullMode;
        }

        public void setFaceWinding(IntEnum<FaceWinding> faceWinding) {
            GraphicsPipeline.this.faceWinding = faceWinding;
        }

        public void setLineWidth(float lineWidth) {
            GraphicsPipeline.this.lineWidth = lineWidth;
        }

        public void setDepthClamp(boolean depthClamp) {
            GraphicsPipeline.this.depthClamp = depthClamp;
        }

        public void setRasterizerDiscard(boolean rasterizerDiscard) {
            GraphicsPipeline.this.rasterizerDiscard = rasterizerDiscard;
        }

        public void setDepthBias(boolean depthBias) {
            GraphicsPipeline.this.depthBias = depthBias;
        }

        public void setRasterizationSamples(int rasterizationSamples) {
            GraphicsPipeline.this.rasterizationSamples = rasterizationSamples;
        }

        public void setSampleShading(boolean sampleShading) {
            GraphicsPipeline.this.sampleShading = sampleShading;
        }

        public void setBlendLogicEnabled(boolean blendLogicEnabled) {
            GraphicsPipeline.this.blendLogicEnabled = blendLogicEnabled;
        }

        public void setBlendLogic(IntEnum<LogicOp> blendLogic) {
            GraphicsPipeline.this.blendLogic = blendLogic;
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
