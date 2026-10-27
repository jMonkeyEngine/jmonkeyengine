package com.jme3.vulkan.material.experimental;

import com.jme3.scene.Mesh;
import com.jme3.vulkan.mesh.VertexInput;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.pipeline.graphics.ColorBlendAttachment;
import com.jme3.vulkan.pipeline.graphics.DepthTest;
import com.jme3.vulkan.pipeline.graphics.StencilTest;
import com.jme3.vulkan.scene.Node;
import com.jme3.vulkan.scene.Scene;
import com.jme3.vulkan.scene.components.Blend;
import com.jme3.vulkan.scene.components.ColorWriteMask;
import com.jme3.vulkan.scene.components.CullModeType;
import com.jme3.vulkan.scene.components.DepthTestType;
import com.jme3.vulkan.scene.components.PolygonModeType;
import com.jme3.vulkan.scene.components.Stencil;
import com.jme3.vulkan.util.Flag;

import java.util.Objects;
import java.util.function.Function;

/**
 * Base identity key for a {@link com.jme3.vulkan.pipeline.graphics.GraphicsPipeline}:
 * every render-state parameter that can actually require a distinct pipeline - the
 * Vulkan-native equivalent of what {@code com.jme3.material.RenderState} covers for the
 * classic renderer (depth testing, blending, face culling, stencil operations,
 * wireframe/polygon mode), expressed with this package's own enums instead - plus a
 * mesh's topology, face winding, and primitive-restart setting, and the
 * {@link VertexInput} layout a technique's shader needs to read that mesh.
 * <p>
 * Deliberately excludes anything Vulkan treats as dynamic state regardless of pipeline
 * identity - line width, depth bias factor/units, and stencil reference/compare/write
 * masks all have a {@code vkCmdSet*} call and never require a different pipeline, only
 * whether a feature is <em>enabled</em> does (hence {@link DepthTest#isBiasEnabled()} is
 * here but no bias value is). Baking dynamic-state values into the key would only
 * fragment the pipeline cache for no benefit.
 * <p>
 * A specific technique's key should extend this with whatever else, beyond render
 * state, actually changes which pipeline one of its nodes needs (e.g. a shader
 * permutation selected by material parameters). A subclass must fold its own fields into
 * {@code equals}/{@code hashCode} alongside {@code super}'s, since two keys with equal
 * render state but different subclass fields still need distinct pipelines.
 */
public class PipelineKey {

    /**
     * Builds a key from {@code node}'s current render-state components, falling back to
     * this class's defaults for any component {@code node} does not have. Leaves
     * {@link #getVertexInput()} {@link VertexInput#empty() empty}, suitable for a
     * technique whose shader reads vertex data itself (vertex pulling) rather than
     * through the fixed-function vertex input stage.
     *
     * @param node node to read render state from
     * @return a key describing the pipeline {@code node} currently needs
     */
    public static PipelineKey create(Node node) {
        PipelineKey key = new PipelineKey();
        key.populate(node);
        return key;
    }

    /**
     * Builds a key from {@code node}'s current render-state components, the same as
     * {@link #create(Node)}, but also builds {@link #getVertexInput()} from {@code node}'s
     * mesh using {@code attributeLocation} to resolve where each attribute the mesh has
     * belongs in the technique's shader.
     *
     * @param node node to read render state from
     * @param attributeLocation maps a vertex attribute name (e.g. one of
     *                          {@link com.jme3.vulkan.mesh.attributes.CommonAttributes})
     *                          to the shader location it occupies, or null if the
     *                          shader doesn't consume that attribute at all
     * @return a key describing the pipeline {@code node} currently needs
     */
    public static PipelineKey create(Node node, Function<String, Integer> attributeLocation) {
        PipelineKey key = new PipelineKey();
        key.populate(node, attributeLocation);
        return key;
    }

    /**
     * Reads every property this key understands off {@code node}, keeping this key's
     * current value for anything {@code node} has no override for. Does not touch
     * {@link #getVertexInput()} - see {@link #populate(Node, Function)} for that. A
     * subclass (e.g. a technique's own key) should call this from its own factory method
     * before reading whatever additional properties it adds.
     *
     * @param node node to read render state from
     */
    protected void populate(Node node) {
        Mesh mesh = node.getProperty(Scene.MeshType.class);
        if (mesh != null) {
            topology = mesh.getTopology();
            winding = mesh.getFaceWinding();
            primitiveRestart = mesh.isPrimitiveRestart();
        }
        polygonMode = node.getProperty(PolygonModeType.class, polygonMode);
        cullMode = node.getProperty(CullModeType.class, cullMode);
        depthTest = node.getProperty(DepthTestType.class, depthTest);
        colorWriteMask = node.getProperty(ColorWriteMask.class, colorWriteMask);
        blend = node.getProperty(Blend.class, blend);
        stencil = node.getProperty(Stencil.class, stencil);
    }

    /**
     * Like {@link #populate(Node)}, but also builds {@link #getVertexInput()} from
     * {@code node}'s mesh, using {@code attributeLocation} to resolve where each
     * attribute the mesh has belongs in the shading technique's shader. Leaves
     * {@link #getVertexInput()} {@link VertexInput#empty() empty} if {@code node} has no
     * mesh.
     *
     * @param node node to read render state from
     * @param attributeLocation maps a vertex attribute name to the shader location it
     *                          occupies, or null if the shader doesn't consume it
     */
    protected void populate(Node node, Function<String, Integer> attributeLocation) {
        populate(node);
        Mesh mesh = node.getProperty(Scene.MeshType.class);
        vertexInput = mesh != null ? new VertexInput(attributeLocation, mesh) : VertexInput.empty();
    }

    private Topology topology = Topology.TriangleList;
    private FaceWinding winding = FaceWinding.Clockwise;
    private boolean primitiveRestart = false;
    private PolygonMode polygonMode = PolygonMode.Fill;
    private CullMode cullMode = CullMode.Back;
    private DepthTest depthTest = new DepthTest();
    private Flag<ColorComponent> colorWriteMask = ColorComponent.All;
    private ColorBlendAttachment blend = new ColorBlendAttachment(false);
    private StencilTest stencil = new StencilTest();
    private VertexInput vertexInput = VertexInput.empty();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PipelineKey k = (PipelineKey)o;
        return topology == k.topology
                && winding == k.winding
                && primitiveRestart == k.primitiveRestart
                && polygonMode == k.polygonMode
                && cullMode == k.cullMode
                && Objects.equals(depthTest, k.depthTest)
                && Flag.equals(colorWriteMask, k.colorWriteMask)
                && Objects.equals(blend, k.blend)
                && Objects.equals(stencil, k.stencil)
                && Objects.equals(vertexInput, k.vertexInput);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topology, winding, primitiveRestart, polygonMode, cullMode, depthTest,
                colorWriteMask, blend, stencil, vertexInput);
    }

    public Topology getTopology() {
        return topology;
    }

    public void setTopology(Topology topology) {
        this.topology = topology;
    }

    public FaceWinding getWinding() {
        return winding;
    }

    public void setWinding(FaceWinding winding) {
        this.winding = winding;
    }

    public boolean isPrimitiveRestart() {
        return primitiveRestart;
    }

    public void setPrimitiveRestart(boolean primitiveRestart) {
        this.primitiveRestart = primitiveRestart;
    }

    public PolygonMode getPolygonMode() {
        return polygonMode;
    }

    public void setPolygonMode(PolygonMode polygonMode) {
        this.polygonMode = polygonMode;
    }

    public CullMode getCullMode() {
        return cullMode;
    }

    public void setCullMode(CullMode cullMode) {
        this.cullMode = cullMode;
    }

    public DepthTest getDepthTest() {
        return depthTest;
    }

    public void setDepthTest(DepthTest depthTest) {
        this.depthTest = depthTest;
    }

    public Flag<ColorComponent> getColorWriteMask() {
        return colorWriteMask;
    }

    public void setColorWriteMask(Flag<ColorComponent> colorWriteMask) {
        this.colorWriteMask = colorWriteMask;
    }

    public ColorBlendAttachment getBlend() {
        return blend;
    }

    public void setBlend(ColorBlendAttachment blend) {
        this.blend = blend;
    }

    public StencilTest getStencil() {
        return stencil;
    }

    public void setStencil(StencilTest stencil) {
        this.stencil = stencil;
    }

    /**
     * Gets the vertex input layout a technique's shader needs to read a node's mesh.
     * Empty (not null) for a technique that reads vertex data itself (vertex pulling)
     * instead of relying on the fixed-function vertex input stage - see
     * {@link VertexInput#empty()}.
     *
     * @return this key's vertex input layout, never null
     */
    public VertexInput getVertexInput() {
        return vertexInput;
    }

    public void setVertexInput(VertexInput vertexInput) {
        this.vertexInput = Objects.requireNonNull(vertexInput, "Use VertexInput.empty() instead of null.");
    }

}
