package com.jme3.vulkan.allocation;

import com.jme3.export.*;
import com.jme3.vulkan.mesh.MeshDescription;
import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.pipeline.graphics.ColorBlendAttachment;
import com.jme3.vulkan.shader.ShaderModule;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;

import java.io.IOException;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

public class GraphicsState implements Savable {

    // pipeline
    private Flag<Pipeline.Create> pipelineFlags = Flag.empty();
    private PipelineLayout layout;
    private Subpass subpass;

    // shaders
    private final Map<ShaderStage, ShaderStageInfo> shaders = new HashMap<>();

    // vertex input
    private MeshDescription mesh;

    // input assembly
    private IntEnum<Topology> topology = Topology.TriangleList;
    private boolean primitiveRestart = false;

    // viewport
    private final List<ViewportInfo> viewports = new ArrayList<>();
    private final List<ScissorInfo> scissors = new ArrayList<>();

    // depth-stencil
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

    // dynamic
    private final Set<IntEnum<DynamicState.Type>> dynamicStates = new HashSet<>();

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        // todo: implement
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        // todo: implement
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphicsState that = (GraphicsState) o;
        return primitiveRestart == that.primitiveRestart
                && depthTest == that.depthTest
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
                && Objects.equals(shaders, that.shaders)
                && Objects.equals(mesh, that.mesh)
                && topology.is(that.topology)
                && Objects.equals(viewports, that.viewports)
                && Objects.equals(scissors, that.scissors)
                && depthCompare.is(that.depthCompare)
                && polygonMode.is(that.polygonMode)
                && cullMode.is(that.cullMode)
                && faceWinding.is(that.faceWinding)
                && Objects.equals(blendAttachments, that.blendAttachments)
                && blendLogic.is(that.blendLogic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shaders, mesh, topology, primitiveRestart, viewports, scissors, depthTest, depthWrite,
                depthBoundsTest, stencilTest, depthCompare, polygonMode, cullMode, faceWinding, lineWidth, depthClamp,
                rasterizerDiscard, depthBias, rasterizationSamples, sampleShading, blendAttachments, blendLogicEnabled, blendLogic);
    }

    public void apply(MemoryStack stack, VkGraphicsPipelineCreateInfo pipeline) {
        pipeline.pRasterizationState();
    }

    public GraphicsState copy() {
        GraphicsState copy = new GraphicsState();
        copy.mesh = mesh; // mesh description is immutable
        copy.topology = topology;
        copy.primitiveRestart = primitiveRestart;
        copy.depthTest = depthTest;
        copy.depthWrite = depthWrite;
        copy.depthBoundsTest = depthBoundsTest;
        copy.stencilTest = stencilTest;
        copy.depthCompare = depthCompare;
        copy.polygonMode = polygonMode;
        copy.cullMode = cullMode;
        copy.faceWinding = faceWinding;
        copy.lineWidth = lineWidth;
        copy.depthClamp = depthClamp;
        copy.rasterizerDiscard = rasterizerDiscard;
        copy.depthBias = depthBias;
        copy.rasterizationSamples = rasterizationSamples;
        copy.sampleShading = sampleShading;
        copy.blendLogicEnabled = blendLogicEnabled;
        copy.blendLogic = blendLogic;
        copy.blendAttachments.addAll(blendAttachments);
        copy.viewports.addAll(viewports);
        copy.scissors.addAll(scissors);
        copy.dynamicStates.addAll(dynamicStates);
        for (ColorBlendAttachment a : blendAttachments) {
            copy.blendAttachments.add(a.copy());
        }
        for (ShaderStageInfo stage : shaders.values()) {
            copy.shaders.put(stage.getStage(), stage.copy());
        }
        return copy;
    }

    public int difference(GraphicsState state, int threshold) {
        if (this == state) return 0;

        int d = 0;
        if (!topology.is(state.topology)) d++;
        if (primitiveRestart != state.primitiveRestart) d++;
        if (depthTest != state.depthTest) d++;
        if (depthWrite != state.depthWrite) d++;
        if (depthBoundsTest != state.depthBoundsTest) d++;
        if (stencilTest != state.stencilTest) d++;
        if (!depthCompare.is(state.depthCompare)) d++;
        if (!polygonMode.is(state.polygonMode)) d++;
        if (!cullMode.is(state.cullMode)) d++;
        if (!faceWinding.is(state.faceWinding)) d++;
        if (lineWidth != state.lineWidth) d++;
        if (depthClamp != state.depthClamp) d++;
        if (rasterizerDiscard != state.rasterizerDiscard) d++;
        if (depthBias != state.depthBias) d++;
        if (rasterizationSamples != state.rasterizationSamples) d++;
        if (sampleShading != state.sampleShading) d++;
        if (blendLogicEnabled != state.blendLogicEnabled) d++;
        if (!blendLogic.is(state.blendLogic)) d++;
        if (!dynamicStates.containsAll(state.dynamicStates)) d++;
        if (blendAttachments.size() != state.blendAttachments.size()) d++;

        if (d > threshold) return d;
        if (blendAttachments.size() == state.blendAttachments.size())
            for (Iterator<ColorBlendAttachment> it1 = blendAttachments.iterator(), it2 = state.blendAttachments.iterator();
                 it1.hasNext() && it2.hasNext();) if (!it1.next().equals(it2.next())) { d++; break; }
        if (d > threshold) return d;
        if (!Objects.equals(mesh, state.mesh)) d++;
        if (d > threshold) return d;
        for (Map.Entry<ShaderStage, ShaderStageInfo> e : shaders.entrySet()) {
            if (!e.getValue().equals(state.shaders.get(e.getKey()))) d++;
        }
        if (d > threshold) return d;
        for (ShaderStage stage : shaders.keySet()) {
            if (!shaders.containsKey(stage)) d++;
        }
        if (d > threshold) return d;
        if (!Objects.equals(viewports, state.viewports)) d++;
        if (d > threshold) return d;
        if (!Objects.equals(scissors, state.scissors)) d++;
        return d;
    }

    public int difference(GraphicsState state) {
        return difference(state, Integer.MAX_VALUE);
    }

    public void addShader(ShaderModule module, ShaderStage stage, String entryPoint) {
        shaders.put(stage, new ShaderStageInfo(module, stage, entryPoint));
    }

    public boolean removeShaderModule(ShaderModule module) {
        return shaders.values().removeIf(s -> s.module == module);
    }

    public int replaceShaderModule(ShaderModule oldModule, ShaderModule newModule) {
        int count = 0;
        for (ShaderStageInfo stage : shaders.values()) {
            if (stage.module == oldModule) {
                stage.setModule(newModule);
                count++;
            }
        }
        return count;
    }

    public int addViewport() {
        return addViewport(0f, 0f, 1024f, 1024f);
    }

    public int addViewport(float x, float y, float w, float h) {
        return addViewport(x, y, w, h, 0f, 1f);
    }

    public int addViewport(float x, float y, float w, float h, float minDepth, float maxDepth) {
        viewports.add(new ViewportInfo(x, y, w, h, minDepth, maxDepth));
        return viewports.size() - 1;
    }

    public int addScissor() {
        return addScissor(0, 0, 1024, 1024);
    }

    public int addScissor(int x, int y, int w, int h) {
        scissors.add(new ScissorInfo(x, y, w, h));
        return scissors.size() - 1;
    }

    public int addBlendAttachment(ColorBlendAttachment attachment) {
        blendAttachments.add(attachment);
        return blendAttachments.size() - 1;
    }

    public void addDynamicState(IntEnum<DynamicState.Type> type) {
        dynamicStates.add(type);
    }

    public boolean removeDynamicState(IntEnum<DynamicState.Type> type) {
        return dynamicStates.remove(type);
    }

    public void setMesh(MeshDescription mesh) {
        this.mesh = mesh;
    }

    public void setTopology(IntEnum<Topology> topology) {
        this.topology = topology;
    }

    public void setPrimitiveRestart(boolean primitiveRestart) {
        this.primitiveRestart = primitiveRestart;
    }

    public void setDepthTest(boolean depthTest) {
        this.depthTest = depthTest;
    }

    public void setDepthWrite(boolean depthWrite) {
        this.depthWrite = depthWrite;
    }

    public void setDepthBoundsTest(boolean depthBoundsTest) {
        this.depthBoundsTest = depthBoundsTest;
    }

    public void setStencilTest(boolean stencilTest) {
        this.stencilTest = stencilTest;
    }

    public void setDepthCompare(IntEnum<CompareOp> depthCompare) {
        this.depthCompare = depthCompare;
    }

    public void setPolygonMode(IntEnum<PolygonMode> polygonMode) {
        this.polygonMode = polygonMode;
    }

    public void setCullMode(Flag<CullMode> cullMode) {
        this.cullMode = cullMode;
    }

    public void setFaceWinding(IntEnum<FaceWinding> faceWinding) {
        this.faceWinding = faceWinding;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public void setDepthClamp(boolean depthClamp) {
        this.depthClamp = depthClamp;
    }

    public void setRasterizerDiscard(boolean rasterizerDiscard) {
        this.rasterizerDiscard = rasterizerDiscard;
    }

    public void setDepthBias(boolean depthBias) {
        this.depthBias = depthBias;
    }

    public void setRasterizationSamples(int rasterizationSamples) {
        this.rasterizationSamples = rasterizationSamples;
    }

    public void setSampleShading(boolean sampleShading) {
        this.sampleShading = sampleShading;
    }

    public void setBlendLogicEnabled(boolean blendLogicEnabled) {
        this.blendLogicEnabled = blendLogicEnabled;
    }

    public void setBlendLogic(IntEnum<LogicOp> blendLogic) {
        this.blendLogic = blendLogic;
    }

    public Map<ShaderStage, ShaderStageInfo> getShaders() {
        return shaders;
    }

    public MeshDescription getMesh() {
        return mesh;
    }

    public IntEnum<Topology> getTopology() {
        return topology;
    }

    public boolean isPrimitiveRestart() {
        return primitiveRestart;
    }

    public List<ViewportInfo> getViewports() {
        return viewports;
    }

    public List<ScissorInfo> getScissors() {
        return scissors;
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
        return blendAttachments;
    }

    public boolean isBlendLogicEnabled() {
        return blendLogicEnabled;
    }

    public IntEnum<LogicOp> getBlendLogic() {
        return blendLogic;
    }

    public Set<IntEnum<DynamicState.Type>> getDynamicStates() {
        return dynamicStates;
    }

    public static class ShaderStageInfo {

        private ShaderModule module;
        private ShaderStage stage;
        private String entryPoint;

        public ShaderStageInfo(ShaderModule module, ShaderStage stage, String entryPoint) {
            this.module = module;
            this.stage = stage;
            this.entryPoint = entryPoint;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ShaderStageInfo that = (ShaderStageInfo) o;
            return Objects.equals(module, that.module) && stage == that.stage && Objects.equals(entryPoint, that.entryPoint);
        }

        @Override
        public int hashCode() {
            return Objects.hash(module, stage, entryPoint);
        }

        public ShaderStageInfo copy() {
            return new ShaderStageInfo(module, stage, entryPoint);
        }

        public ShaderModule getModule() {
            return module;
        }

        public void setModule(ShaderModule module) {
            this.module = module;
        }

        public ShaderStage getStage() {
            return stage;
        }

        public void setStage(ShaderStage stage) {
            this.stage = stage;
        }

        public String getEntryPoint() {
            return entryPoint;
        }

        public void setEntryPoint(String entryPoint) {
            this.entryPoint = entryPoint;
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
