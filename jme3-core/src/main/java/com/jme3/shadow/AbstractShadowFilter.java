/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector4f;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;

/**
 *
 * Generic abstract filter that holds common implementations for the different
 * shadow filters
 *
 * @author Rémy Bouquet aka Nehon
 */
public abstract class AbstractShadowFilter<T extends AbstractShadowRenderer> extends Filter implements Cloneable, JmeCloneable {

    protected T shadowRenderer;
    protected ViewPort viewPort;

    /**
     * Abstract class constructor
     *
     * @param manager the application asset manager
     * @param shadowMapSize the size of the rendered shadowmaps (512,1024,2048,
     * etc...)
     * @param nbShadowMaps the number of shadow maps rendered (the more shadow
     * maps the more quality, the less fps).
     * @param shadowRenderer the shadowRenderer to use for this Filter
     */
    @SuppressWarnings("all")
    protected AbstractShadowFilter(AssetManager manager, int shadowMapSize, T shadowRenderer) {
        super("Post Shadow");
        material = new Material(manager, "Common/MatDefs/Shadow/PostShadowFilter.j3md");       
        this.shadowRenderer = shadowRenderer;
        this.shadowRenderer.setPostShadowMaterial(material);

        //this is legacy setting for shadows with backface shadows
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

    public Material getShadowMaterial() {       
        return material;
    }
    Vector4f tmpv = new Vector4f();

    @Override
    protected void preFrame(float tpf) {
        shadowRenderer.preFrame(tpf);
        material.setMatrix4("ViewProjectionMatrixInverse", viewPort.getCamera().getViewProjectionMatrix().invert());
        Matrix4f m = viewPort.getCamera().getViewProjectionMatrix();
        material.setVector4("ViewProjectionMatrixRow2", tmpv.set(m.m20, m.m21, m.m22, m.m23));

    }

    @Override
    protected void postQueue(RenderQueue queue) {
        shadowRenderer.postQueue(queue);
         if(shadowRenderer.skipPostPass){
             //removing the shadow map so that the post pass is skipped
             material.setTexture("ShadowMap0", null);
         }
    }

    @Override
    protected void postFrame(RenderManager renderManager, ViewPort viewPort, FrameBuffer prevFilterBuffer, FrameBuffer sceneBuffer) {
        if(!shadowRenderer.skipPostPass){
            shadowRenderer.setPostShadowParams();
        }
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        shadowRenderer.needsfallBackMaterial = true;
        shadowRenderer.initialize(renderManager, vp);
        this.viewPort = vp;
    }
    
      /**
     * How far the shadows are rendered in the view
     *
     * @see #setShadowZExtend(float zFar)
     * @return shadowZExtend
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
     * returns the shdaow intensity
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
     * Sets the shadow edges thickness. default is 1, setting it to lower values
     * can help to reduce the jagged effect of the shadow edges
     *
     * @param edgesThickness
     */
    public void setEdgesThickness(int edgesThickness) {
        shadowRenderer.setEdgesThickness(edgesThickness);
    }

    /**
     * isFlushQueues does nothing and is kept only for backward compatibility
     */
    @Deprecated
    public boolean isFlushQueues() {
        return shadowRenderer.isFlushQueues();
    }

    /**
     * setFlushQueues does nothing now and is kept only for backward compatibility
     */
    @Deprecated
    public void setFlushQueues(boolean flushQueues) {}

    /**
     * sets the shadow compare mode see {@link CompareMode} for more info
     *
     * @param compareMode
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
     * @param filterMode
     */
    final public void setEdgeFilteringMode(EdgeFilteringMode filterMode) {
        shadowRenderer.setEdgeFilteringMode(filterMode);
    }

    /**
     *
     * !! WARNING !! this parameter is defaulted to true for the ShadowFilter.
     * Setting it to true, may produce edges artifacts on shadows.     *
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
    public void setRenderBackFacesShadows(Boolean renderBackFacesShadows) {
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
     * returns the the edge filtering mode
     *
     * @see EdgeFilteringMode
     * @return
     */
    public EdgeFilteringMode getEdgeFilteringMode() {
        return shadowRenderer.getEdgeFilteringMode();
    }

    @Override
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

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
    }
}
