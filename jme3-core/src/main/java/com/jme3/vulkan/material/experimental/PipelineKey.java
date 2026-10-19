package com.jme3.vulkan.material.experimental;

import com.jme3.scene.Mesh;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.scene.Node;
import com.jme3.vulkan.scene.Scene;
import com.jme3.vulkan.util.Flag;

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

    public static Optional<PipelineKey> create(Node node) {
        Mesh mesh = node.getComponent()
    }

    private final PolygonMode polygonMode;
    private final CullMode cullMode;

    private final boolean depthTestEnabled;
    private final boolean depthWriteEnabled;
    private final CompareOp depthCompareOp;
    private final boolean depthBiasEnabled;

    private final Flag<ColorComponent> colorWriteMask;

    private final boolean blendEnabled;
    private final BlendFactor srcColorBlendFactor;
    private final BlendFactor dstColorBlendFactor;
    private final BlendOp colorBlendOp;
    private final BlendFactor srcAlphaBlendFactor;
    private final BlendFactor dstAlphaBlendFactor;
    private final BlendOp alphaBlendOp;

    private final boolean stencilTestEnabled;
    private final CompareOp frontStencilCompareOp;
    private final int frontStencilFailOp;
    private final int frontStencilDepthFailOp;
    private final int frontStencilPassOp;
    private final CompareOp backStencilCompareOp;
    private final int backStencilFailOp;
    private final int backStencilDepthFailOp;
    private final int backStencilPassOp;

    protected PipelineKey(PolygonMode polygonMode, CullMode cullMode,
            boolean depthTestEnabled, boolean depthWriteEnabled, CompareOp depthCompareOp, boolean depthBiasEnabled,
            Flag<ColorComponent> colorWriteMask,
            boolean blendEnabled,
            BlendFactor srcColorBlendFactor, BlendFactor dstColorBlendFactor, BlendOp colorBlendOp,
            BlendFactor srcAlphaBlendFactor, BlendFactor dstAlphaBlendFactor, BlendOp alphaBlendOp,
            boolean stencilTestEnabled,
            CompareOp frontStencilCompareOp, int frontStencilFailOp, int frontStencilDepthFailOp, int frontStencilPassOp,
            CompareOp backStencilCompareOp, int backStencilFailOp, int backStencilDepthFailOp, int backStencilPassOp) {
        this.polygonMode = polygonMode;
        this.cullMode = cullMode;
        this.depthTestEnabled = depthTestEnabled;
        this.depthWriteEnabled = depthWriteEnabled;
        this.depthCompareOp = depthCompareOp;
        this.depthBiasEnabled = depthBiasEnabled;
        this.colorWriteMask = colorWriteMask;
        this.blendEnabled = blendEnabled;
        this.srcColorBlendFactor = srcColorBlendFactor;
        this.dstColorBlendFactor = dstColorBlendFactor;
        this.colorBlendOp = colorBlendOp;
        this.srcAlphaBlendFactor = srcAlphaBlendFactor;
        this.dstAlphaBlendFactor = dstAlphaBlendFactor;
        this.alphaBlendOp = alphaBlendOp;
        this.stencilTestEnabled = stencilTestEnabled;
        this.frontStencilCompareOp = frontStencilCompareOp;
        this.frontStencilFailOp = frontStencilFailOp;
        this.frontStencilDepthFailOp = frontStencilDepthFailOp;
        this.frontStencilPassOp = frontStencilPassOp;
        this.backStencilCompareOp = backStencilCompareOp;
        this.backStencilFailOp = backStencilFailOp;
        this.backStencilDepthFailOp = backStencilDepthFailOp;
        this.backStencilPassOp = backStencilPassOp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PipelineKey k = (PipelineKey)o;
        return depthTestEnabled == k.depthTestEnabled
                && depthWriteEnabled == k.depthWriteEnabled
                && depthBiasEnabled == k.depthBiasEnabled
                && blendEnabled == k.blendEnabled
                && stencilTestEnabled == k.stencilTestEnabled
                && frontStencilFailOp == k.frontStencilFailOp
                && frontStencilDepthFailOp == k.frontStencilDepthFailOp
                && frontStencilPassOp == k.frontStencilPassOp
                && backStencilFailOp == k.backStencilFailOp
                && backStencilDepthFailOp == k.backStencilDepthFailOp
                && backStencilPassOp == k.backStencilPassOp
                && polygonMode == k.polygonMode
                && cullMode == k.cullMode
                && depthCompareOp == k.depthCompareOp
                && Objects.equals(colorWriteMask, k.colorWriteMask)
                && srcColorBlendFactor == k.srcColorBlendFactor
                && dstColorBlendFactor == k.dstColorBlendFactor
                && colorBlendOp == k.colorBlendOp
                && srcAlphaBlendFactor == k.srcAlphaBlendFactor
                && dstAlphaBlendFactor == k.dstAlphaBlendFactor
                && alphaBlendOp == k.alphaBlendOp
                && frontStencilCompareOp == k.frontStencilCompareOp
                && backStencilCompareOp == k.backStencilCompareOp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(polygonMode, cullMode, depthTestEnabled, depthWriteEnabled, depthCompareOp,
                depthBiasEnabled, colorWriteMask, blendEnabled, srcColorBlendFactor, dstColorBlendFactor,
                colorBlendOp, srcAlphaBlendFactor, dstAlphaBlendFactor, alphaBlendOp, stencilTestEnabled,
                frontStencilCompareOp, frontStencilFailOp, frontStencilDepthFailOp, frontStencilPassOp,
                backStencilCompareOp, backStencilFailOp, backStencilDepthFailOp, backStencilPassOp);
    }

}
