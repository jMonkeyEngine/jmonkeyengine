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
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector4f;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import java.io.IOException;

/**
 *
 * Generic abstract filter that holds common implementations for the different
 * shadow filtesr
 *
 * @author Rémy Bouquet aka Nehon
 */
public abstract class AbstractShadowFilter<T extends AbstractShadowRenderer> extends Filter {

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
    }

    @Override
    protected void postFrame(RenderManager renderManager, ViewPort viewPort, FrameBuffer prevFilterBuffer, FrameBuffer sceneBuffer) {
        shadowRenderer.setPostShadowParams();
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        shadowRenderer.needsfallBackMaterial = true;
        shadowRenderer.initialize(renderManager, vp);
        this.viewPort = vp;
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
     * gives a bright and invisilble shadow, a 1 value gives a pitch black
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
     * returns true if the PssmRenderer flushed the shadow queues
     *
     * @return flushQueues
     */
    public boolean isFlushQueues() {
        return shadowRenderer.isFlushQueues();
    }

    /**
     * Set this to false if you want to use several PssmRederers to have
     * multiple shadows cast by multiple light sources. Make sure the last
     * PssmRenderer in the stack DO flush the queues, but not the others
     *
     * @param flushQueues
     */
    public void setFlushQueues(boolean flushQueues) {
        shadowRenderer.setFlushQueues(flushQueues);
    }

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
     * returns the the edge filtering mode
     *
     * @see EdgeFilteringMode
     * @return
     */
    public EdgeFilteringMode getEdgeFilteringMode() {
        return shadowRenderer.getEdgeFilteringMode();
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
