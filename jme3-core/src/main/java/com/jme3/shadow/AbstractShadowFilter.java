/*
 * Copyright (c) 2009-2025 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector4f;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

/**
 * Generic abstract filter that holds common implementations for the different
 * shadow filters
 *
 * @author Rémy Bouquet aka Nehon
 */
public abstract class AbstractShadowFilter<T extends AbstractShadowRenderer> extends Filter implements JmeCloneable {

    protected T shadowRenderer;
    protected ViewPort viewPort;

    private final Vector4f tempVec4 = new Vector4f();
    private final Matrix4f tempMat4 = new Matrix4f();

    /**
     * For serialization only. Do not use.
     */
    protected AbstractShadowFilter() {
    }

    /**
     * Creates an AbstractShadowFilter. Subclasses invoke this constructor.
     *
     * @param assetManager The application's asset manager.
     * @param shadowMapSize The size of the rendered shadow maps (e.g., 512, 1024, 2048).
     * @param shadowRenderer The shadowRenderer to use for this Filter
     */
    protected AbstractShadowFilter(AssetManager assetManager, int shadowMapSize, T shadowRenderer) {
        super("Post Shadow");
        this.shadowRenderer = shadowRenderer;
        // this is legacy setting for shadows with backface shadows
        this.shadowRenderer.setRenderBackFacesShadows(true);
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    @Override
    protected boolean isRequiresDepthTexture() {
        return true;
    }

    /**
     * @deprecated Use {@link #getMaterial()} instead.
     * @return The Material used by this filter.
     */
    @Deprecated
    public Material getShadowMaterial() {       
        return material;
    }

    @Override
    protected void preFrame(float tpf) {
        shadowRenderer.preFrame(tpf);
        Matrix4f m = viewPort.getCamera().getViewProjectionMatrix();
        material.setMatrix4("ViewProjectionMatrixInverse", tempMat4.set(m).invertLocal());
        material.setVector4("ViewProjectionMatrixRow2", tempVec4.set(m.m20, m.m21, m.m22, m.m23));
    }

    @Override
    protected void postQueue(RenderQueue queue) {
        shadowRenderer.postQueue(queue);
        if (shadowRenderer.skipPostPass) {
            // removing the shadow map so that the post pass is skipped
            material.setTexture("ShadowMap0", null);
        }
    }

    @Override
    protected void postFrame(RenderManager renderManager, ViewPort viewPort, FrameBuffer prevFilterBuffer, FrameBuffer sceneBuffer) {
        if (!shadowRenderer.skipPostPass) {
            shadowRenderer.setPostShadowParams();
        }
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        shadowRenderer.needsfallBackMaterial = true;
        material = new Material(manager, "Common/MatDefs/Shadow/PostShadowFilter.j3md");
        shadowRenderer.setPostShadowMaterial(material);
        shadowRenderer.initialize(renderManager, vp);
        this.viewPort = vp;
    }

    /**
     * How far the shadows are rendered in the view
     *
     * @return shadowZExtend
     * @see #setShadowZExtend(float zFar)
     */
    public float getShadowZExtend() {
        return shadowRenderer.getShadowZExtend();
    }

    /**
     * Set the distance from the eye where the shadows will be rendered default
     * value is dynamically computed to the shadow casters/receivers union bound
     * zFar, capped to view frustum far value.
     *
     * @param zFar the zFar values that override the computed one
     */
    public void setShadowZExtend(float zFar) {
        shadowRenderer.setShadowZExtend(zFar);
    }

    /**
     * Define the length over which the shadow will fade out when using a
     * shadowZextend
     *
     * @param length the fade length in world units
     */
    public void setShadowZFadeLength(float length) {
        shadowRenderer.setShadowZFadeLength(length);
    }

    /**
     * get the length over which the shadow will fade out when using a
     * shadowZextend
     *
     * @return the fade length in world units
     */
    public float getShadowZFadeLength() {
        return shadowRenderer.getShadowZFadeLength();
    }

    /**
     * returns the shadow intensity
     *
     * @see #setShadowIntensity(float shadowIntensity)
     * @return shadowIntensity
     */
    public float getShadowIntensity() {
        return shadowRenderer.getShadowIntensity();
    }

    /**
     * Set the shadowIntensity, the value should be between 0 and 1, a 0 value
     * gives a bright and invisible shadow, a 1 value gives a pitch black
     * shadow, default is 0.7
     *
     * @param shadowIntensity the darkness of the shadow
     */
    final public void setShadowIntensity(float shadowIntensity) {
        shadowRenderer.setShadowIntensity(shadowIntensity);
    }

    /**
     * returns the edges thickness <br>
     *
     * @see #setEdgesThickness(int edgesThickness)
     * @return edgesThickness
     */
    public int getEdgesThickness() {
        return shadowRenderer.getEdgesThickness();
    }

    /**
     * Sets the shadow edges thickness. Default is 10. Setting it to lower values
     * can help reduce the jagged effect of shadow edges.
     *
     * @param edgesThickness the desired thickness (in tenths of a pixel, default=10)
     */
    public void setEdgesThickness(int edgesThickness) {
        shadowRenderer.setEdgesThickness(edgesThickness);
    }

    /**
     * isFlushQueues does nothing and is kept only for backward compatibility
     *
     * @return false
     */
    @Deprecated
    public boolean isFlushQueues() {
        return shadowRenderer.isFlushQueues();
    }

    /**
     * sets the shadow compare mode see {@link CompareMode} for more info
     *
     * @param compareMode the desired mode
     */
    final public void setShadowCompareMode(CompareMode compareMode) {
        shadowRenderer.setShadowCompareMode(compareMode);
    }

    /**
     * returns the shadow compare mode
     *
     * @see CompareMode
     * @return the shadowCompareMode
     */
    public CompareMode getShadowCompareMode() {
        return shadowRenderer.getShadowCompareMode();
    }

    /**
     * Sets the filtering mode for shadow edges see {@link EdgeFilteringMode}
     * for more info
     *
     * @param filterMode the desired mode
     */
    final public void setEdgeFilteringMode(EdgeFilteringMode filterMode) {
        shadowRenderer.setEdgeFilteringMode(filterMode);
    }

    /**
     *
     * !! WARNING !! this parameter is defaulted to true for the ShadowFilter.
     * Setting it to true, may produce edges artifacts on shadows.
     *
     * Set to true if you want back faces shadows on geometries.
     * Note that back faces shadows will be blended over dark lighten areas and may produce overly dark lighting.
     *
     * Setting this parameter will override this parameter for ALL materials in the scene.
     * This also will automatically adjust the faceCullMode and the PolyOffset of the pre shadow pass.
     * You can modify them by using {@link #getPreShadowForcedRenderState()}
     *
     * If you want to set it differently for each material in the scene you have to use the ShadowRenderer instead
     * of the shadow filter.
     *
     * @param renderBackFacesShadows true or false.
     */
    public void setRenderBackFacesShadows(boolean renderBackFacesShadows) {
        shadowRenderer.setRenderBackFacesShadows(renderBackFacesShadows);
    }

    /**
     * if this filter renders back faces shadows
     * @return true if this filter renders back faces shadows
     */
    public boolean isRenderBackFacesShadows() {
        return shadowRenderer.isRenderBackFacesShadows();
    }

    /**
     * returns the pre shadows pass render state.
     * use it to adjust the RenderState parameters of the pre shadow pass.
     * Note that this will be overridden if the preShadow technique in the material has a ForcedRenderState
     * @return the pre shadow render state.
     */
    public RenderState getPreShadowForcedRenderState() {
        return shadowRenderer.getPreShadowForcedRenderState();
    }

    /**
     * returns the edge filtering mode
     *
     * @see EdgeFilteringMode
     * @return the enum value
     */
    public EdgeFilteringMode getEdgeFilteringMode() {
        return shadowRenderer.getEdgeFilteringMode();
    }

    /**
     * Read the number of shadow maps rendered by this filter.
     *
     * @return count
     */
    public int getNumShadowMaps() {
        return shadowRenderer.getNumShadowMaps();
    }

    /**
     * Read the size of each shadow map rendered by this filter.
     *
     * @return a map's height (which is also its width, in pixels)
     */
    public int getShadowMapSize() {
        return shadowRenderer.getShadowMapSize();
    }

    @Override
    @SuppressWarnings("unchecked")
    public AbstractShadowFilter<T> jmeClone() {
        try {
            return (AbstractShadowFilter<T>) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cloneFields(final Cloner cloner, final Object original) {
        material = cloner.clone(material);
        shadowRenderer = cloner.clone(shadowRenderer);
        shadowRenderer.setPostShadowMaterial(material);
    }

}
