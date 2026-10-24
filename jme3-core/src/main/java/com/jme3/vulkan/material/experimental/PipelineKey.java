package com.jme3.vulkan.material.experimental;

import com.jme3.scene.Mesh;
import com.jme3.vulkan.StencilOp;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.pipeline.graphics.ColorBlendAttachment;
import com.jme3.vulkan.pipeline.graphics.StencilTest;
import com.jme3.vulkan.scene.Node;
import com.jme3.vulkan.scene.Scene;
import com.jme3.vulkan.util.Flag;
import org.checkerframework.checker.units.qual.C;
import sun.awt.X11.ToplevelStateListener;

import java.util.Objects;
import java.util.Optional;

/**
 * Base identity key for a {@link com.jme3.vulkan.pipeline.graphics.GraphicsPipeline}:
 * every render-state parameter that can actually require a distinct pipeline - the
 * Vulkan-native equivalent of what {@code com.jme3.material.RenderState} covers for the
 * classic renderer (depth testing, blending, face culling, stencil operations,
 * wireframe/polygon mode), expressed with this package's own enums instead.
 * <p>
 * Deliberately excludes anything Vulkan treats as dynamic state regardless of pipeline
 * identity - line width, depth bias factor/units, and stencil reference/compare/write
 * masks all have a {@code vkCmdSet*} call and never require a different pipeline, only
 * whether a feature is <em>enabled</em> does (hence {@code depthBiasEnabled} is here but
 * no bias value is). Baking dynamic-state values into the key would only fragment the
 * pipeline cache for no benefit.
 * <p>
 * A specific technique's key should extend this with whatever else, beyond render
 * state, actually changes which pipeline one of its nodes needs (e.g. a mesh's topology
 * or face winding, or a shader permutation). A subclass must fold its own fields into
 * {@code equals}/{@code hashCode} alongside {@code super}'s, since two keys with equal
 * render state but different subclass fields still need distinct pipelines.
 */
public class PipelineKey {

    public static PipelineKey create(Node node) {
        PipelineKey key = new PipelineKey();
        Mesh mesh = node.getProperty(Scene.MeshType.class);
        if (mesh != null) {
            key.topology = mesh.getTopology();
            key.winding = mesh.getFaceWinding();
        }
    }

    private Topology topology = Topology.TriangleList;
    private FaceWinding winding = FaceWinding.Clockwise;
    private PolygonMode polygonMode = PolygonMode.Fill;
    private CullMode cullMode = CullMode.Back;
    private boolean depthTestEnabled = true;
    private boolean depthWriteEnabled = true;
    private CompareOp depthCompareOp = CompareOp.LessOrEqual;
    private boolean depthBiasEnabled = false;
    private Flag<ColorComponent> colorWriteMask = ColorComponent.All;
    private ColorBlendAttachment blend = new ColorBlendAttachment(false);
    private StencilTest stencil = new StencilTest();

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
                && depthTestEnabled == k.depthTestEnabled
                && depthWriteEnabled == k.depthWriteEnabled
                && depthBiasEnabled == k.depthBiasEnabled
                && polygonMode == k.polygonMode
                && cullMode == k.cullMode
                && depthCompareOp == k.depthCompareOp
                && Flag.equals(colorWriteMask, k.colorWriteMask)
                && Objects.equals(blend, k.blend)
                && Objects.equals(stencil, k.stencil);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topology, winding, polygonMode, cullMode, depthTestEnabled, depthWriteEnabled,
                            depthCompareOp, depthBiasEnabled, colorWriteMask, blend, stencil);
    }

}
