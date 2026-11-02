package com.jme3.vulkan.material.experimental;

import com.jme3.scene.Mesh;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.mesh.VertexInput;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.pipeline.framebuffer.FrameBuffer;
import com.jme3.vulkan.pipeline.framebuffer.RenderTarget;
import com.jme3.vulkan.pipeline.graphics.ColorBlendAttachment;
import com.jme3.vulkan.pipeline.graphics.DepthTest;
import com.jme3.vulkan.pipeline.graphics.StencilTest;
import com.jme3.vulkan.scene.Node;
import com.jme3.vulkan.scene.components.Blend;
import com.jme3.vulkan.scene.components.ColorWriteMask;
import com.jme3.vulkan.scene.components.CullModeType;
import com.jme3.vulkan.scene.components.DepthTestType;
import com.jme3.vulkan.scene.components.PolygonModeType;
import com.jme3.vulkan.scene.components.Stencil;
import com.jme3.vulkan.util.Flag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Base identity key for a {@link com.jme3.vulkan.pipeline.graphics.GraphicsPipeline}, and the
 * fixed-function state it is created from: every render-state parameter that can actually require
 * a distinct pipeline - the
 * Vulkan-native equivalent of what {@code com.jme3.material.RenderState} covers for the
 * classic renderer (depth testing, blending, face culling, stencil operations,
 * wireframe/polygon mode), expressed with this package's own enums instead - plus a
 * mesh's topology, face winding, and primitive-restart setting, and the
 * {@link VertexInput} layout a technique's shader needs to read that mesh. It also holds the
 * state a technique or render target decides rather than a node: the formats and sample count of
 * the attachments rendered to, push constant ranges, and other rarely changed rasterization state.
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
        Mesh mesh = node.getMesh();
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
        Mesh mesh = node.getMesh();
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
    private List<PushConstantRange> pushConstants = Collections.emptyList();
    private boolean depthClamp = false;
    private boolean depthBoundsTest = false;
    private boolean rasterizerDiscard = false;
    private LogicOp logicOp = null;
    private int samples = 1;
    private List<Format> colorFormats = Collections.emptyList();
    private Format depthFormat = null;
    private Format stencilFormat = null;
    private Set<DynamicState> dynamicStates = Collections.emptySet();

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
                && Objects.equals(vertexInput, k.vertexInput)
                && pushConstants.equals(k.pushConstants)
                && depthClamp == k.depthClamp
                && depthBoundsTest == k.depthBoundsTest
                && rasterizerDiscard == k.rasterizerDiscard
                && logicOp == k.logicOp
                && samples == k.samples
                && colorFormats.equals(k.colorFormats)
                && depthFormat == k.depthFormat
                && stencilFormat == k.stencilFormat
                && dynamicStates.equals(k.dynamicStates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topology, winding, primitiveRestart, polygonMode, cullMode, depthTest,
                colorWriteMask, blend, stencil, vertexInput, pushConstants, depthClamp, depthBoundsTest,
                rasterizerDiscard, logicOp, samples, colorFormats, depthFormat, stencilFormat, dynamicStates);
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

    /**
     * Gets the push constant ranges the pipeline's layout must have, e.g. a shader's
     * {@link com.jme3.vulkan.shadergen.Shader#getPushConstantRanges() ranges}. Part of the key
     * because a pipeline is created against one layout and can't be used with another.
     *
     * @return push constant ranges, empty (not null) if the pipeline has none
     */
    public List<PushConstantRange> getPushConstants() {
        return pushConstants;
    }

    /**
     * @param pushConstants push constant ranges the pipeline's layout must have, copied
     */
    public void setPushConstants(List<PushConstantRange> pushConstants) {
        this.pushConstants = pushConstants.isEmpty() ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(pushConstants));
    }

    public boolean isDepthClamp() {
        return depthClamp;
    }

    public void setDepthClamp(boolean depthClamp) {
        this.depthClamp = depthClamp;
    }

    /**
     * @return true if fragments outside the depth bounds are discarded; the bounds themselves
     *         are dynamic state
     */
    public boolean isDepthBoundsTest() {
        return depthBoundsTest;
    }

    public void setDepthBoundsTest(boolean depthBoundsTest) {
        this.depthBoundsTest = depthBoundsTest;
    }

    public boolean isRasterizerDiscard() {
        return rasterizerDiscard;
    }

    public void setRasterizerDiscard(boolean rasterizerDiscard) {
        this.rasterizerDiscard = rasterizerDiscard;
    }

    /**
     * @return logic op applied to color attachments instead of blending, or null if disabled
     */
    public LogicOp getLogicOp() {
        return logicOp;
    }

    public void setLogicOp(LogicOp logicOp) {
        this.logicOp = logicOp;
    }

    /**
     * @return number of samples per pixel of the attachments rendered to
     */
    public int getSamples() {
        return samples;
    }

    /**
     * @param samples number of samples per pixel of the attachments rendered to: 1, 2, 4, 8, 16, 32 or 64
     */
    public void setSamples(int samples) {
        if (samples < 1 || samples > 64 || Integer.bitCount(samples) != 1) {
            throw new IllegalArgumentException("Sample count must be a power of two from 1 to 64: " + samples);
        }
        this.samples = samples;
    }

    /**
     * Gets the formats of the color attachments rendered to, in attachment order. Every color
     * attachment is blended with {@link #getBlend()} and written with {@link #getColorWriteMask()}.
     *
     * @return color attachment formats, empty (not null) if there are none
     */
    public List<Format> getColorFormats() {
        return colorFormats;
    }

    public void setColorFormats(List<Format> colorFormats) {
        this.colorFormats = colorFormats.isEmpty() ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(colorFormats));
    }

    /**
     * @return format of the depth attachment rendered to, or null if there is none
     */
    public Format getDepthFormat() {
        return depthFormat;
    }

    public void setDepthFormat(Format depthFormat) {
        this.depthFormat = depthFormat;
    }

    /**
     * @return format of the stencil attachment rendered to, or null if there is none
     */
    public Format getStencilFormat() {
        return stencilFormat;
    }

    public void setStencilFormat(Format stencilFormat) {
        this.stencilFormat = stencilFormat;
    }

    /**
     * Sets the color, depth and stencil formats and the sample count to those of the attachments of
     * {@code frameBuffer}, so pipelines created from this key can render to it. The depth and stencil
     * formats are taken from the depth/stencil target only for the aspects it is rendered with. If
     * {@code frameBuffer} has no attachments, the sample count is left unchanged.
     *
     * @param frameBuffer frame buffer to render to
     * @throws IllegalArgumentException if the attachments of {@code frameBuffer} don't all have the
     *                                  same sample count
     */
    public void setAttachments(FrameBuffer<?> frameBuffer) {
        int fboSamples = -1;
        List<Format> colors = new ArrayList<>(frameBuffer.getColorTargets().size());
        for (RenderTarget<?> t : frameBuffer.getColorTargets()) {
            colors.add(t.getFormat());
            fboSamples = checkSamples(fboSamples, t);
        }
        setColorFormats(colors);
        RenderTarget<?> ds = frameBuffer.getDepthStencilTarget();
        if (ds != null) {
            depthFormat = ds.getAspects().contains(EngineImage.Aspect.Depth) ? ds.getFormat() : null;
            stencilFormat = ds.getAspects().contains(EngineImage.Aspect.Stencil) ? ds.getFormat() : null;
            fboSamples = checkSamples(fboSamples, ds);
        } else {
            depthFormat = null;
            stencilFormat = null;
        }
        if (fboSamples > 0) {
            setSamples(fboSamples);
        }
    }

    private static int checkSamples(int samples, RenderTarget<?> target) {
        int s = target.getView().getImage().getSamples();
        if (samples > 0 && s != samples) {
            throw new IllegalArgumentException("Frame buffer attachments have different sample counts: " + samples + " and " + s + ".");
        }
        return s;
    }

    /**
     * Gets the states to make dynamic beyond those every pipeline makes dynamic. A pipeline always
     * makes the viewport and scissor dynamic, and the values this key leaves out when the feature
     * that uses them is enabled: line width for line rasterization, depth bias values when depth
     * bias is enabled, depth bounds when the depth bounds test is enabled, and stencil masks and
     * reference when the stencil test is enabled.
     *
     * @return additional dynamic states
     */
    public Set<DynamicState> getDynamicStates() {
        return dynamicStates;
    }

    public void setDynamicStates(Set<DynamicState> dynamicStates) {
        this.dynamicStates = dynamicStates.isEmpty() ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(dynamicStates));
    }

}
