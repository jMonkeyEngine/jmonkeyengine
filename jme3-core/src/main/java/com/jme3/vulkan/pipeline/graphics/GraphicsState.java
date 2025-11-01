package com.jme3.vulkan.pipeline.graphics;

import com.jme3.util.Versionable;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.mesh.MeshDescription;
import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.pipeline.cache.PipelineCache;
import com.jme3.vulkan.pipeline.states.*;
import com.jme3.vulkan.shader.ShaderModule;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Supplier;

import static org.lwjgl.vulkan.VK10.*;

public class GraphicsState implements BasePipelineState<GraphicsState, VkGraphicsPipelineCreateInfo>, Versionable {

    protected static final int BASE_INDEX = 0;

    // graphics
    protected static final int CREATE_FLAGS = BASE_INDEX;
    private Subpass subpass;
    private PipelineLayout layout;
    private Flag<AbstractVulkanPipeline.Create> flags = Flag.empty();

    // shaders
    private final Map<Integer, IShaderState> shaders = new HashMap<>();

    // vertex input
    private MeshDescription mesh; // not mutated the normal way

    // input assembly
    protected static final int TOPOLOGY = BASE_INDEX + 1;
    protected static final int PRIMITIVE_RESTART = BASE_INDEX + 2;
    private IntEnum<Topology> topology = Topology.TriangleList;
    private Boolean primitiveRestart = false;

    // depth stencil
    protected static final int DEPTH_TEST = BASE_INDEX + 3;
    protected static final int DEPTH_WRITE = BASE_INDEX + 4;
    protected static final int DEPTH_BOUNDS_TEST = BASE_INDEX + 5;
    protected static final int STENCIL_TEST = BASE_INDEX + 6;
    protected static final int DEPTH_COMPARE = BASE_INDEX + 7;
    private boolean depthTest = true;
    private boolean depthWrite = true;
    private boolean depthBoundsTest = false;
    private boolean stencilTest = false;
    private IntEnum<CompareOp> depthCompare = CompareOp.LessOrEqual;

    // rasterization
    protected static final int POLYGON_MODE = BASE_INDEX + 8;
    protected static final int CULL_MODE = BASE_INDEX + 9;
    protected static final int FACE_WINDING = BASE_INDEX + 10;
    protected static final int LINE_WIDTH = BASE_INDEX + 11;
    protected static final int DEPTH_CLAMP = BASE_INDEX + 12;
    protected static final int RASTERIZER_DISCARD = BASE_INDEX + 13;
    protected static final int DEPTH_BIAS = BASE_INDEX + 14;
    private IntEnum<PolygonMode> polygonMode = PolygonMode.Fill;
    private Flag<CullMode> cullMode = CullMode.Back;
    private IntEnum<FaceWinding> faceWinding = FaceWinding.Clockwise;
    private float lineWidth = 1f;
    private boolean depthClamp = false;
    private boolean rasterizerDiscard = false;
    private boolean depthBias = false;

    // multisample
    protected static final int RASTERIZATION_SAMPLES = BASE_INDEX + 15;
    protected static final int SAMPLE_SHADING = BASE_INDEX + 16;
    private int rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;
    private boolean sampleShading = false;

    // color blend
    protected static final int BLEND_LOGIC_ENABLED = 17;
    protected static final int BLEND_LOGIC = 18;
    private final List<ColorBlendAttachment> blendAttachments = new ArrayList<>();
    private boolean blendLogicEnabled = false;
    private IntEnum<LogicOp> blendLogic = LogicOp.Copy;

    // viewport
    private final List<ViewportInfo> viewports = new ArrayList<>();
    private final List<ScissorInfo> scissors = new ArrayList<>();

    // dynamic
    private final Set<Integer> dynamicStates = new HashSet<>();

    // management
    protected static final int LAST_USED_INDEX = BLEND_LOGIC;
    protected final Map<MeshDescription, CachedPipeline> supportedPipelines = new HashMap<>();
    protected final BitSet applied = new BitSet();
    protected long version = 0L;

    public GraphicsState() {}

    @Override
    public VkGraphicsPipelineCreateInfo create(MemoryStack stack) {
        return VkGraphicsPipelineCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
    }

    @Override
    public VkGraphicsPipelineCreateInfo fill(MemoryStack stack, VkGraphicsPipelineCreateInfo struct, Collection<ShaderModule> shaders) {
        struct.flags(getFlags().bits())
                .layout(layout.getNativeObject())
                .renderPass(subpass.getPass().getNativeObject())
                .subpass(subpass.getPosition());
        if (!shaders.isEmpty()) {
            VkPipelineShaderStageCreateInfo.Buffer stageBuf = VkPipelineShaderStageCreateInfo.calloc(shaders.size(), stack);
            for (ShaderModule s : shaders) {
                fillShaderInfo(stack, s, stageBuf.get().sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO));
            }
            stageBuf.flip();
            struct.stageCount(stageBuf.limit()).pStages(stageBuf);
        }
        return struct.pVertexInputState(createVertexInput(stack))
                .pInputAssemblyState(createInputAssembly(stack))
                .pDepthStencilState(createDepthStencil(stack))
                .pRasterizationState(createRasterization(stack))
                .pMultisampleState(createMultisample(stack))
                .pColorBlendState(createColorBlend(stack))
                .pViewportState(createViewport(stack))
                .pDynamicState(createDynamic(stack));
    }

    @Override
    public GraphicsState copy(GraphicsState store) {
        if (store == null) {
            store = new GraphicsState();
        }
        store.subpass = subpass;
        store.layout = layout;
        store.mesh = mesh;
        store.topology = topology;
        store.primitiveRestart = primitiveRestart;
        store.depthTest = depthTest;
        store.depthWrite = depthWrite;
        store.depthBoundsTest = depthBoundsTest;
        store.stencilTest = stencilTest;
        store.depthCompare = depthCompare;
        store.polygonMode = polygonMode;
        store.cullMode = cullMode;
        store.faceWinding = faceWinding;
        store.lineWidth = lineWidth;
        store.depthClamp = depthClamp;
        store.rasterizerDiscard = rasterizerDiscard;
        store.rasterizationSamples = rasterizationSamples;
        store.sampleShading = sampleShading;
        store.blendLogicEnabled = blendLogicEnabled;
        store.blendLogic = blendLogic;
        store.viewports.addAll(viewports);
        store.scissors.addAll(scissors);
        store.dynamicStates.addAll(dynamicStates);
        store.shaders.putAll(shaders);
        store.blendAttachments.addAll(blendAttachments);
        return store;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphicsState that = (GraphicsState) o;
        return depthTest == that.depthTest
                && depthWrite == that.depthWrite
                && depthBoundsTest == that.depthBoundsTest
                && stencilTest == that.stencilTest
                && Float.compare(lineWidth, that.lineWidth) == 0
                && depthClamp == that.depthClamp
                && rasterizerDiscard == that.rasterizerDiscard
                && depthBias == that.depthBias
                && rasterizationSamples == that.rasterizationSamples
                && sampleShading == that.sampleShading
                && blendLogicEnabled == that.blendLogicEnabled
                && Objects.equals(subpass, that.subpass)
                && Objects.equals(layout, that.layout)
                && Objects.equals(flags, that.flags)
                && Objects.equals(mesh, that.mesh)
                && Objects.equals(topology, that.topology)
                && Objects.equals(primitiveRestart, that.primitiveRestart)
                && Objects.equals(depthCompare, that.depthCompare)
                && Objects.equals(polygonMode, that.polygonMode)
                && Objects.equals(cullMode, that.cullMode)
                && Objects.equals(faceWinding, that.faceWinding)
                && Objects.equals(blendAttachments, that.blendAttachments)
                && Objects.equals(blendLogic, that.blendLogic)
                && Objects.equals(viewports, that.viewports)
                && Objects.equals(scissors, that.scissors)
                && Objects.equals(dynamicStates, that.dynamicStates)
                && Objects.equals(shaders, that.shaders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subpass, layout, flags, mesh, topology, primitiveRestart, depthTest, depthWrite,
                depthBoundsTest, stencilTest, depthCompare, polygonMode, cullMode, faceWinding, lineWidth, depthClamp,
                rasterizerDiscard, depthBias, rasterizationSamples, sampleShading, blendAttachments, blendLogicEnabled,
                blendLogic, viewports, scissors, dynamicStates, shaders);
    }

    @Override
    public Pipeline selectPipeline(PipelineCache cache, MeshDescription mesh) {
        this.mesh = mesh;
        CachedPipeline result = supportedPipelines.get(mesh);
        if (result == null || result.version != version) {
            // find an up-to-date pipeline for a different mesh to be the parent,
            // otherwise let the cache take care of it
            Pipeline parent = supportedPipelines.values().stream()
                    .filter(p -> p.version == version)
                    .findAny()
                    .map(CachedPipeline::getPipeline).orElse(null);
            result = new CachedPipeline(cache.acquirePipeline(this, parent), version);
            supportedPipelines.put(mesh, result);
        }
        return result.getPipeline();
    }

    @Override
    public GraphicsPipeline createPipeline(LogicalDevice<?> device, Pipeline parent, Collection<ShaderModule> shaders) {
        if (layout == null) {
            throw new NullPointerException("Pipeline layout is not defined.");
        }
        return new GraphicsPipeline(device, layout, parent, this, shaders);
    }

    @Override
    public Collection<IShaderState> getPipelineShaderStates() {
        return Collections.unmodifiableCollection(shaders.values());
    }

    @Override
    public long getVersionNumber() {
        return version;
    }

    @Override
    public GraphicsState override(GraphicsState state, GraphicsState store) {
        if (store == null) {
            store = new GraphicsState();
        }
        store.subpass = subpass != null ? subpass : state.subpass;
        store.layout = layout != null ? layout : state.layout;
        store.flags = override(state.flags, flags, CREATE_FLAGS);
        store.topology = override(state.topology, topology, TOPOLOGY);
        store.primitiveRestart = override(state.primitiveRestart, primitiveRestart, PRIMITIVE_RESTART);
        store.depthTest = override(state.depthTest, depthTest, DEPTH_TEST);
        store.depthWrite = override(state.depthWrite, depthWrite, DEPTH_WRITE);
        store.depthBoundsTest = override(state.depthBoundsTest, depthBoundsTest, DEPTH_BOUNDS_TEST);
        store.stencilTest = override(state.stencilTest, stencilTest, STENCIL_TEST);
        store.depthCompare = override(state.depthCompare, depthCompare, DEPTH_COMPARE);
        store.polygonMode = override(state.polygonMode, polygonMode, POLYGON_MODE);
        store.cullMode = override(state.cullMode, cullMode, CULL_MODE);
        store.faceWinding = override(state.faceWinding, faceWinding, FACE_WINDING);
        store.lineWidth = override(state.lineWidth, lineWidth, LINE_WIDTH);
        store.depthClamp = override(state.depthClamp, depthClamp, DEPTH_CLAMP);
        store.rasterizerDiscard = override(state.rasterizerDiscard, rasterizerDiscard, RASTERIZER_DISCARD);
        store.depthBias = override(state.depthBias, depthBias, DEPTH_BIAS);
        store.rasterizationSamples = override(state.rasterizationSamples, rasterizationSamples, RASTERIZATION_SAMPLES);
        store.sampleShading = override(state.sampleShading, sampleShading, SAMPLE_SHADING);
        store.blendLogicEnabled = override(state.blendLogicEnabled, blendLogicEnabled, BLEND_LOGIC_ENABLED);
        store.blendLogic = override(state.blendLogic, blendLogic, BLEND_LOGIC);
        store.blendAttachments.clear();
        override(state.blendAttachments, blendAttachments, store.blendAttachments);
        store.dynamicStates.clear();
        store.dynamicStates.addAll(state.dynamicStates);
        store.dynamicStates.addAll(dynamicStates);
        store.shaders.clear();
        store.shaders.putAll(state.shaders);
        store.shaders.putAll(shaders);
        override(state.viewports, viewports, store.viewports);
        override(state.scissors, scissors, store.scissors);
        store.applied.clear();
        store.applied.or(state.applied);
        store.applied.or(applied);
        return store;
    }

    @Override
    public final Class<GraphicsState> getBaseStateClass() {
        return GraphicsState.class;
    }

    protected void fillShaderInfo(MemoryStack stack, ShaderModule shader, VkPipelineShaderStageCreateInfo struct) {
        shader.fill(stack, struct);
    }

    protected VkPipelineVertexInputStateCreateInfo createVertexInput(MemoryStack stack) {
        return VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(mesh.getBindingInfo(stack))
                .pVertexAttributeDescriptions(mesh.getAttributeInfo(stack));
    }

    protected VkPipelineInputAssemblyStateCreateInfo createInputAssembly(MemoryStack stack) {
        return VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(topology.getEnum())
                .primitiveRestartEnable(primitiveRestart);
    }

    protected VkPipelineDepthStencilStateCreateInfo createDepthStencil(MemoryStack stack) {
        return VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                .depthTestEnable(depthTest)
                .depthWriteEnable(depthWrite)
                .depthBoundsTestEnable(depthBoundsTest)
                .stencilTestEnable(stencilTest)
                .depthCompareOp(depthCompare.getEnum());
    }

    protected VkPipelineRasterizationStateCreateInfo createRasterization(MemoryStack stack) {
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
            a.writeToStruct(attBuf.get());
        }
        attBuf.flip();
        return VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .logicOpEnable(blendLogicEnabled)
                .logicOp(blendLogic.getEnum())
                .pAttachments(attBuf);
    }

    protected VkPipelineViewportStateCreateInfo createViewport(MemoryStack stack) {
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

    protected boolean stateEquals(GraphicsState that) {
        return depthTest == that.depthTest
                && depthWrite == that.depthWrite
                && depthBoundsTest == that.depthBoundsTest
                && stencilTest == that.stencilTest
                && Float.compare(lineWidth, that.lineWidth) == 0
                && depthClamp == that.depthClamp
                && rasterizerDiscard == that.rasterizerDiscard
                && depthBias == that.depthBias
                && rasterizationSamples == that.rasterizationSamples
                && sampleShading == that.sampleShading
                && blendLogicEnabled == that.blendLogicEnabled
                && Objects.equals(subpass, that.subpass)
                && Objects.equals(layout, that.layout)
                && Objects.equals(flags, that.flags)
                && Objects.equals(mesh, that.mesh)
                && Objects.equals(topology, that.topology)
                && Objects.equals(primitiveRestart, that.primitiveRestart)
                && Objects.equals(depthCompare, that.depthCompare)
                && Objects.equals(polygonMode, that.polygonMode)
                && Objects.equals(cullMode, that.cullMode)
                && Objects.equals(faceWinding, that.faceWinding)
                && Objects.equals(blendAttachments, that.blendAttachments)
                && Objects.equals(blendLogic, that.blendLogic)
                && Objects.equals(viewports, that.viewports)
                && Objects.equals(scissors, that.scissors) && Objects.equals(dynamicStates, that.dynamicStates);
    }

    protected <T> T override(T val, T override, int i) {
        return applied.get(i) ? override : val;
    }

    protected <T> void override(List<T> state, List<T> overrides, List<T> store) {
        store.clear();
        for (int i = 0, l = Math.max(state.size(), overrides.size()); i < l; i++) {
            if (i < overrides.size() && overrides.get(i) != null) {
                store.add(overrides.get(i));
            } else if (i < state.size()) {
                store.add(state.get(i));
            } else {
                store.add(null);
            }
        }
    }

    protected void incrementVersion(PipelineState<?> oldState, PipelineState<?> newState) {
        if (oldState != newState) {
            // must make sure the new state's version doesn't make this state "travel back in time"
            version += 1 + Math.abs(PipelineState.versionOf(oldState) - PipelineState.versionOf(newState));
        }
    }

    public void setSubpass(Subpass subpass) {
        if (this.subpass != subpass) {
            this.subpass = subpass;
            version++;
        }
    }

    public void setLayout(PipelineLayout layout) {
        if (this.layout != layout) {
            this.layout = layout;
            version++;
        }
    }

    public void setFlags(Flag<AbstractVulkanPipeline.Create> flags) {
        this.flags = set(this.flags, flags, CREATE_FLAGS);
    }

    public void addShader(IShaderState shader) {
        if (!shader.equals(shaders.put(shader.getStage().getVk(), shader))) {
            version++;
        }
    }

    public IShaderState removeShader(ShaderStage stage) {
        return shaders.remove(stage.getVk());
    }

    public void setTopology(IntEnum<Topology> topology) {
        this.topology = set(this.topology, topology, TOPOLOGY);
    }

    public void setPrimitiveRestart(Boolean primitiveRestart) {
        this.primitiveRestart = set(this.primitiveRestart, primitiveRestart, PRIMITIVE_RESTART);
    }

    public void setDepthTest(Boolean depthTest) {
        this.depthTest = set(this.depthTest, depthTest, DEPTH_TEST);
    }

    public void setDepthWrite(Boolean depthWrite) {
        this.depthWrite = set(this.depthWrite, depthWrite, DEPTH_WRITE);
    }

    public void setDepthBoundsTest(Boolean depthBoundsTest) {
        this.depthBoundsTest = set(this.depthBoundsTest, depthBoundsTest, DEPTH_BOUNDS_TEST);
    }

    public void setStencilTest(Boolean stencilTest) {
        this.stencilTest = set(this.stencilTest, stencilTest, STENCIL_TEST);
    }

    public void setDepthCompare(IntEnum<CompareOp> depthCompare) {
        this.depthCompare = set(this.depthCompare, depthCompare, DEPTH_COMPARE);
    }

    public void setPolygonMode(IntEnum<PolygonMode> polygonMode) {
        this.polygonMode = set(this.polygonMode, polygonMode, POLYGON_MODE);
    }

    public void setCullMode(Flag<CullMode> cullMode) {
        this.cullMode = set(this.cullMode, cullMode, CULL_MODE);
    }

    public void setFaceWinding(IntEnum<FaceWinding> faceWinding) {
        this.faceWinding = set(this.faceWinding, faceWinding, FACE_WINDING);
    }

    public void setLineWidth(Float lineWidth) {
        this.lineWidth = set(this.lineWidth, lineWidth, LINE_WIDTH, DynamicState.LineWidth);
    }

    public void setDepthClamp(Boolean depthClamp) {
        this.depthClamp = set(this.depthClamp, depthClamp, DEPTH_CLAMP);
    }

    public void setRasterizerDiscard(Boolean rasterizerDiscard) {
        this.rasterizerDiscard = set(this.rasterizerDiscard, rasterizerDiscard, RASTERIZER_DISCARD);
    }

    public void setDepthBias(Boolean depthBias) {
        this.depthBias = set(this.depthBias, depthBias, DEPTH_BIAS, DynamicState.DepthBias);
    }

    public void setRasterizationSamples(Integer rasterizationSamples) {
        this.rasterizationSamples = set(this.rasterizationSamples, rasterizationSamples, RASTERIZATION_SAMPLES);
    }

    public void setSampleShading(Boolean sampleShading) {
        this.sampleShading = set(this.sampleShading, sampleShading, SAMPLE_SHADING);
    }

    public void setBlendLogicEnabled(Boolean blendLogicEnabled) {
        this.blendLogicEnabled = set(this.blendLogicEnabled, blendLogicEnabled, BLEND_LOGIC_ENABLED);
    }

    public void setBlendLogic(IntEnum<LogicOp> blendLogic) {
        this.blendLogic = set(this.blendLogic, blendLogic, BLEND_LOGIC);
    }

    public void setBlendAttachment(int i, ColorBlendAttachment attachment) {
        while (blendAttachments.size() <= i) {
            blendAttachments.add(null);
        }
        if (!attachment.equals(blendAttachments.set(i, attachment))) {
            version++;
        }
    }

    public ColorBlendAttachment clearBlendAttachment(int i) {
        if (i >= blendAttachments.size()) return null;
        return blendAttachments.set(i, null);
    }

    public void setViewPort(int i, float x, float y, float w, float h, float minDepth, float maxDepth) {
        while (viewports.size() <= i) {
            viewports.add(null);
        }
        ViewportInfo vp = new ViewportInfo(x, y, w, h, minDepth, maxDepth);
        if (!vp.equals(viewports.set(i, vp)) && !isDynamic(DynamicState.ViewPort)) {
            version++;
        }
    }

    public void setViewPort(int i, float x, float y, float w, float h) {
        setViewPort(i, x, y, w, h, 0f, 1f);
    }

    public void setViewPort(int i) {
        setViewPort(i, 0f, 0f, 128f, 128f, 0f, 1f);
    }

    public void setScissor(int i, int x, int y, int w, int h) {
        while (scissors.size() <= i) {
            scissors.add(null);
        }
        ScissorInfo scissor = new ScissorInfo(x, y, w, h);
        if (!scissor.equals(scissors.set(i, scissor)) && !isDynamic(DynamicState.Scissor)) {
            version++;
        }
    }

    public void setScissor(int i) {
        setScissor(i, 0, 0, 128, 128);
    }

    public void addDynamic(IntEnum<DynamicState> dynamic) {
        if (dynamicStates.add(dynamic.getEnum())) {
            version++;
        }
    }

    public void removeDynamic(IntEnum<DynamicState> dynamic) {
        if (dynamicStates.remove(dynamic.getEnum())) {
            version++;
        }
    }

    public Subpass getSubpass() {
        return subpass;
    }

    public PipelineLayout getLayout() {
        return layout;
    }

    public Flag<AbstractVulkanPipeline.Create> getFlags() {
        return flags;
    }

    public Map<Integer, IShaderState> getShaders() {
        return Collections.unmodifiableMap(shaders);
    }

    public IShaderState getShader(ShaderStage stage) {
        return shaders.get(stage.getVk());
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

    public Set<Integer> getDynamicStates() {
        return Collections.unmodifiableSet(dynamicStates);
    }

    public boolean isDynamic(IntEnum<DynamicState> dynamic) {
        return dynamicStates.contains(dynamic.getEnum());
    }

    protected <T> T get(T val, T defVal) {
        return val != null ? val : defVal;
    }

    protected <T> T set(T oldVal, T newVal, int i) {
        applied.set(i);
        if (oldVal != newVal) version++;
        return newVal;
    }

    protected <T> T set(T oldVal, T newVal, int i, IntEnum<DynamicState> dynamic) {
        applied.set(i);
        if (oldVal != newVal && !isDynamic(dynamic)) version++;
        return newVal;
    }

    protected <T> T setEq(T oldVal, T newVal, int i) {
        applied.set(i);
        if (!Objects.equals(oldVal, newVal)) version++;
        return newVal;
    }

    protected <T extends Flag> Flag<T> set(Flag<T> oldVal, Flag<T> newVal, int i) {
        applied.set(i);
        if (!Flag.is(oldVal, newVal)) version++;
        return newVal;
    }

    protected <T extends IntEnum> IntEnum<T> set(IntEnum<T> oldVal, IntEnum<T> newVal, int i) {
        applied.set(i);
        if (!IntEnum.is(oldVal, newVal)) version++;
        return newVal;
    }

    protected boolean changed(Object oldVal, Object newVal, Object defVal) {
        return get(oldVal, defVal) != get(newVal, defVal);
    }

    protected boolean objectChanged(Object oldVal, Object newVal, Object defVal) {
        return !get(oldVal, defVal).equals(get(newVal, defVal));
    }

    protected boolean flagChanged(Flag oldVal, Flag newVal, Flag defVal) {
        return !get(oldVal, defVal).is(get(newVal, defVal));
    }

    protected boolean enumChanged(IntEnum oldVal, IntEnum newVal, IntEnum defVal) {
        return !get(oldVal, defVal).is(get(newVal, defVal));
    }

    protected static class CachedPipeline {

        public final Supplier<Pipeline> pipeline;
        public final long version;

        public CachedPipeline(Supplier<Pipeline> pipeline, long version) {
            this.pipeline = pipeline;
            this.version = version;
        }

        public Pipeline getPipeline() {
            return pipeline.get();
        }

        public long getVersion() {
            return version;
        }

    }

    /**
     * Immutable viewport information.
     */
    protected static class ViewportInfo {

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

    /**
     * Immutable scissor information.
     */
    protected static class ScissorInfo {

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
