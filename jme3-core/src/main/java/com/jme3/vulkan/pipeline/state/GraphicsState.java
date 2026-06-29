package com.jme3.vulkan.pipeline.state;

import com.jme3.scene.Mesh;
import com.jme3.vulkan.material.DepthBias;
import com.jme3.vulkan.material.experimental.ShaderBindingLayout;
import com.jme3.vulkan.material.experimental.ShaderBindingSet;
import com.jme3.vulkan.material.shader.ShaderModule;
import com.jme3.vulkan.mesh.VertexInput;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.pipeline.graphics.ColorBlendAttachment;
import com.jme3.vulkan.shaderc.ShaderType;
import com.jme3.vulkan.util.Flag;

import java.util.*;

/**
 * Graphics state variables compatible with all rendering backends.
 */
public class GraphicsState implements Cloneable {
    
    private final Map<ShaderType, ShaderModule> shaders = new EnumMap<>(ShaderType.class);
    private final Map<Integer, ColorBlendAttachment> blendOverrides = new HashMap<>();
    private final Map<String, Integer> attributeMappings = new HashMap<>();
    private final Map<Integer, ShaderBindingSet> bindings = new HashMap<>();
    private final Set<DynamicState> dynamic = EnumSet.noneOf(DynamicState.class);
    private VertexInput vertexInput;
    private boolean depthTest = true;
    private boolean depthWrite = true;
    private boolean depthBoundsTest = true;
    private boolean depthClamp = true;
    private boolean stencilTest = false;
    private boolean rasterizerDiscard = false;
    private boolean primitiveRestart = false;
    private float lineWidth = 1f;
    private CompareOp depthCompare = CompareOp.LessOrEqual;
    private DepthBias depthBias;
    private PolygonMode polygonMode = PolygonMode.Fill;
    private Topology topology = Topology.TriangleList;
    private FaceWinding faceWinding = FaceWinding.CounterClockwise;
    private Flag<CullMode> cullMode = CullMode.Back;
    private LogicOp blendLogic;

    private final DynamicKey key = new DynamicKey(this);

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return equals((GraphicsState)o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blendOverrides, attributeMappings, depthTest, depthWrite, depthBoundsTest, stencilTest,
                depthClamp, blendLogic, rasterizerDiscard, lineWidth, depthCompare, depthBias, polygonMode, cullMode,
                topology, primitiveRestart, faceWinding, vertexInput);
    }

    @Override
    public GraphicsState clone() {
        try {
            GraphicsState state = (GraphicsState)super.clone();
            state.shaders.putAll(shaders);
            blendOverrides.forEach((k, e) -> state.blendOverrides.put(k, e.clone()));
            state.attributeMappings.putAll(attributeMappings);
            state.dynamic.addAll(dynamic);
            if (state.depthBias != null) {
                state.depthBias = state.depthBias.clone();
            }
            return state;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean equals(GraphicsState that) {
        return that != null
            && depthTest == that.depthTest
            && depthWrite == that.depthWrite
            && depthBoundsTest == that.depthBoundsTest
            && depthClamp == that.depthClamp
            && stencilTest == that.stencilTest
            && rasterizerDiscard == that.rasterizerDiscard
            && primitiveRestart == that.primitiveRestart
            && polygonMode == that.polygonMode
            && depthCompare == that.depthCompare
            && blendLogic == that.blendLogic
            && topology == that.topology
            && faceWinding == that.faceWinding
            && Float.compare(lineWidth, that.lineWidth) == 0
            && Flag.equals(cullMode, that.cullMode)
            && Objects.equals(blendOverrides, that.blendOverrides)
            && Objects.equals(attributeMappings, that.attributeMappings)
            && Objects.equals(depthBias, that.depthBias)
            && Objects.equals(dynamic, that.dynamic)
            && Objects.equals(vertexInput, that.vertexInput);
    }

    /**
     * Tests if graphics state {@code o} is equal to this graphics state, accounting for
     * the fact that dynamic fields do not have to be equal between states.
     *
     * @param o state to test equality to
     * @return true if dynamically equal
     */
    public boolean dynamicEquals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphicsState that = (GraphicsState)o;
        return Objects.equals(dynamic, that.dynamic)
                && blendLogic == that.blendLogic
                && depthClamp == that.depthClamp
                && polygonMode == that.polygonMode
                && faceWinding == that.faceWinding
                && (that.dynamic.contains(DynamicState.DepthTest) || depthTest == that.depthTest)
                && (that.dynamic.contains(DynamicState.DepthWrite) || depthWrite == that.depthWrite)
                && (that.dynamic.contains(DynamicState.DepthBoundsTest) || depthBoundsTest == that.depthBoundsTest)
                && (that.dynamic.contains(DynamicState.StencilTest) || stencilTest == that.stencilTest)
                && (that.dynamic.contains(DynamicState.RasterizerDiscard) || rasterizerDiscard == that.rasterizerDiscard)
                && (that.dynamic.contains(DynamicState.DepthCompare) || depthCompare == that.depthCompare)
                && (that.dynamic.contains(DynamicState.LineWidth) || Float.compare(lineWidth, that.lineWidth) == 0)
                && (that.dynamic.contains(DynamicState.CullMode) || Flag.equals(cullMode, that.cullMode))
                && (that.dynamic.contains(DynamicState.DepthBiasEnabled) || (depthBias == null) == (that.depthBias == null))
                && (that.dynamic.contains(DynamicState.DepthBias) || Objects.equals(depthBias, that.depthBias))
                && (that.dynamic.contains(DynamicState.Topology) || Objects.equals(topology, that.topology))
                && (that.dynamic.contains(DynamicState.PrimitiveRestart) || primitiveRestart == that.primitiveRestart)
                && Objects.equals(blendOverrides, that.blendOverrides)
                && Objects.equals(attributeMappings, that.attributeMappings)
                && Objects.equals(vertexInput, that.vertexInput);
    }

    /**
     * Computes a hash code that accounts for the fact that the values of dynamic fields do not matter.
     *
     * @return dynamic hash code
     */
    public int dynamicHashCode() {
        return Objects.hash(dynamic,
                dynamicHash(DynamicState.DepthTest, depthTest),
                dynamicHash(DynamicState.DepthWrite, depthWrite),
                dynamicHash(DynamicState.DepthBoundsTest, depthBoundsTest),
                depthClamp,
                dynamicHash(DynamicState.StencilTest, stencilTest),
                dynamicHash(DynamicState.RasterizerDiscard, rasterizerDiscard),
                polygonMode,
                dynamicHash(DynamicState.DepthCompare, depthCompare),
                blendLogic,
                dynamicHash(DynamicState.LineWidth, lineWidth),
                dynamicHash(DynamicState.CullMode, cullMode),
                blendOverrides,
                attributeMappings,
                dynamicHash(DynamicState.DepthBiasEnabled, depthBias == null),
                dynamicHash(DynamicState.DepthBias, depthBias),
                dynamicHash(DynamicState.Topology, topology),
                dynamicHash(DynamicState.PrimitiveRestart, primitiveRestart),
                faceWinding,
                vertexInput);
    }

    private int dynamicHash(DynamicState state, Object value) {
        return dynamic.contains(state) ? Objects.hashCode(value) : 0;
    }

    public void applyMesh(Mesh mesh) {
        vertexInput = mesh.declareVertexInput(attributeMappings::get);
        topology = mesh.getTopology();
        faceWinding = mesh.getFaceWinding();
        primitiveRestart = mesh.isPrimitiveRestart();
    }

    public void set(GraphicsState state) {
        depthTest = state.depthTest;
        depthWrite = state.depthWrite;
        depthBoundsTest = state.depthBoundsTest;
        depthClamp = state.depthClamp;
        stencilTest = state.stencilTest;
        rasterizerDiscard = state.rasterizerDiscard;
        lineWidth = state.lineWidth;
        depthCompare = state.depthCompare;
        blendLogic = state.blendLogic;
        primitiveRestart = state.primitiveRestart;
        topology = state.topology;
        faceWinding = state.faceWinding;
        depthBias = state.depthBias;
        if (depthBias != null) {
            depthBias = depthBias.clone();
        }
        blendOverrides.clear();
        shaders.clear();
        attributeMappings.clear();
        dynamic.clear();
        state.blendOverrides.forEach((key, value) -> blendOverrides.put(key, value.clone()));
        shaders.putAll(state.shaders);
        attributeMappings.putAll(state.attributeMappings);
        dynamic.addAll(state.dynamic);
    }

    public void setShader(ShaderType type, ShaderModule shader) {
        if (shader != null) {
            shaders.put(type, shader);
        } else {
            shaders.remove(type);
        }
    }

    public void clearShaders() {
        shaders.clear();
    }

    public void setBlendOverride(int colorTarget, ColorBlendAttachment attachment) {
        if (attachment != null) {
            blendOverrides.put(colorTarget, attachment);
        } else {
            blendOverrides.remove(colorTarget);
        }
    }

    public void clearBlendOverrides() {
        blendOverrides.clear();
    }

    public void setAttributeMapping(String name, Integer location) {
        if (location != null) {
            attributeMappings.put(name, location);
        } else {
            attributeMappings.remove(name);
        }
    }

    public void clearAttributeMappings() {
        attributeMappings.clear();
    }

    public void setDynamic(DynamicState state, boolean enable) {
        if (enable) dynamic.add(state);
        else dynamic.remove(state);
    }

    public void clearDynamic() {
        dynamic.clear();
    }

    public Map<ShaderType, ShaderModule> getShaders() {
        return shaders;
    }

    public Map<Integer, ColorBlendAttachment> getBlendOverrides() {
        return blendOverrides;
    }

    public Map<String, Integer> getAttributeMappings() {
        return attributeMappings;
    }

    public Set<DynamicState> getDynamic() {
        return dynamic;
    }

    public VertexInput getVertexInput() {
        return vertexInput;
    }

    public boolean getDepthTest() {
        return depthTest;
    }

    public boolean getDepthWrite() {
        return depthWrite;
    }

    public boolean getDepthBoundsTest() {
        return depthBoundsTest;
    }

    public boolean getDepthClamp() {
        return depthClamp;
    }

    public boolean getStencilTest() {
        return stencilTest;
    }

    public boolean getRasterizerDiscard() {
        return rasterizerDiscard;
    }

    public boolean getPrimitiveRestart() {
        return primitiveRestart;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public CompareOp getDepthCompare() {
        return depthCompare;
    }

    public DepthBias getDepthBias() {
        return depthBias;
    }

    public PolygonMode getPolygonMode() {
        return polygonMode;
    }

    public Topology getTopology() {
        return topology;
    }

    public FaceWinding getFaceWinding() {
        return faceWinding;
    }

    public Flag<CullMode> getCullMode() {
        return cullMode;
    }

    public LogicOp getBlendLogic() {
        return blendLogic;
    }

    public GraphicsState getBase() {
        return base;
    }

    public DynamicKey getDynamicKey() {
        return key;
    }

    public static class DynamicKey {

        private final GraphicsState state;

        protected DynamicKey(GraphicsState state) {
            this.state = state;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (getClass() == o.getClass()) {
                return state.dynamicEquals(((DynamicKey)o).state);
            }
            return state.dynamicEquals(o);
        }

        @Override
        public int hashCode() {
            return state.dynamicHashCode();
        }

    }

}
