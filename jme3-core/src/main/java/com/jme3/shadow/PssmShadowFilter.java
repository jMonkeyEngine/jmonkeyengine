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
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.shadow.PssmShadowRenderer.CompareMode;
import com.jme3.shadow.PssmShadowRenderer.FilterMode;
import com.jme3.texture.FrameBuffer;
import java.io.IOException;

/** 
 * 
 * This Filter does basically the same as a PssmShadowRenderer except it renders 
 * the post shadow pass as a fulscreen quad pass instead of a geometry pass.
 * It's mostly faster than PssmShadowRenderer as long as you have more than a about ten shadow recieving objects.
 * The expense is the draw back that the shadow Recieve mode set on spatial is ignored.
 * So basically all and only objects that render depth in the scene receive shadows.
 * See this post for more details http://jmonkeyengine.org/groups/general-2/forum/topic/silly-question-about-shadow-rendering/#post-191599
 * 
 * API is basically the same as the PssmShadowRenderer;
 * 
 * @author Rémy Bouquet aka Nehon
 * @deprecated use {@link DirectionalLightShadowFilter}
 */
@Deprecated
public class PssmShadowFilter extends Filter {

    private PssmShadowRenderer pssmRenderer;
    private ViewPort viewPort;

    /**
     * Creates a PSSM Shadow Filter 
     * More info on the technique at <a href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a>
     * @param manager the application asset manager
     * @param size the size of the rendered shadowmaps (512,1024,2048, etc...)
     * @param nbSplits the number of shadow maps rendered (the more shadow maps the more quality, the less fps). 
     */
    public PssmShadowFilter(AssetManager manager, int size, int nbSplits) {
        super("Post Shadow");
        material = new Material(manager, "Common/MatDefs/Shadow/PostShadowFilter.j3md");
        pssmRenderer = new PssmShadowRenderer(manager, size, nbSplits, material);
        pssmRenderer.needsfallBackMaterial = true;
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
        pssmRenderer.preFrame(tpf);
        material.setMatrix4("ViewProjectionMatrixInverse", viewPort.getCamera().getViewProjectionMatrix().invert());
        Matrix4f m = viewPort.getCamera().getViewProjectionMatrix();
        material.setVector4("ViewProjectionMatrixRow2", tmpv.set(m.m20, m.m21, m.m22, m.m23));

    }

    @Override
    protected void postQueue(RenderQueue queue) {
        pssmRenderer.postQueue(queue);
    }

    @Override
    protected void postFrame(RenderManager renderManager, ViewPort viewPort, FrameBuffer prevFilterBuffer, FrameBuffer sceneBuffer) {
        pssmRenderer.setPostShadowParams();
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        pssmRenderer.initialize(renderManager, vp);
        this.viewPort = vp;
    }

    /**
     * returns the light direction used by the processor
     * @return 
     */
    public Vector3f getDirection() {
        return pssmRenderer.getDirection();
    }

    /**
     * Sets the light direction to use to compute shadows
     * @param direction 
     */
    public void setDirection(Vector3f direction) {
        pssmRenderer.setDirection(direction);
    }

    /**
     * returns the lambda parameter
     * @see #setLambda(float lambda)
     * @return lambda
     */
    public float getLambda() {
        return pssmRenderer.getLambda();
    }

    /**
     * Adjust the repartition of the different shadow maps in the shadow extend
     * usualy goes from 0.0 to 1.0
     * a low value give a more linear repartition resulting in a constant quality in the shadow over the extends, but near shadows could look very jagged
     * a high value give a more logarithmic repartition resulting in a high quality for near shadows, but the quality quickly decrease over the extend.
     * the default value is set to 0.65f (theoric optimal value).
     * @param lambda the lambda value.
     */
    public void setLambda(float lambda) {
        pssmRenderer.setLambda(lambda);
    }

    /**
     * How far the shadows are rendered in the view
     * @see setShadowZExtend(float zFar)
     * @return shadowZExtend
     */
    public float getShadowZExtend() {
        return pssmRenderer.getShadowZExtend();
    }

    /**
     * Set the distance from the eye where the shadows will be rendered
     * default value is dynamically computed to the shadow casters/receivers union bound zFar, capped to view frustum far value.
     * @param zFar the zFar values that override the computed one
     */
    public void setShadowZExtend(float zFar) {
        pssmRenderer.setShadowZExtend(zFar);
    }

    /**
     * returns the shadow intensity
     * @see #setShadowIntensity(float shadowIntensity)
     * @return shadowIntensity
     */
    public float getShadowIntensity() {
        return pssmRenderer.getShadowIntensity();
    }

    /**
     * Set the shadowIntensity, the value should be between 0 and 1,
     * a 0 value gives a bright and invisible shadow,
     * a 1 value gives a pitch black shadow,
     * default is 0.7
     * @param shadowIntensity the darkness of the shadow
     */
    final public void setShadowIntensity(float shadowIntensity) {
        pssmRenderer.setShadowIntensity(shadowIntensity);
    }

    /**
     * returns the edges thickness <br>
     * @see #setEdgesThickness(int edgesThickness)
     * @return edgesThickness
     */
    public int getEdgesThickness() {
        return pssmRenderer.getEdgesThickness();
    }

    /**
     * Sets the shadow edges thickness. default is 1, setting it to lower values can help to reduce the jagged effect of the shadow edges
     * @param edgesThickness 
     */
    public void setEdgesThickness(int edgesThickness) {
        pssmRenderer.setEdgesThickness(edgesThickness);
    }

    /**
     * returns true if the PssmRenderer flushed the shadow queues
     * @return flushQueues
     */
    public boolean isFlushQueues() {
        return pssmRenderer.isFlushQueues();
    }

    /**
     * Set this to false if you want to use several PssmRederers to have multiple shadows cast by multiple light sources.
     * Make sure the last PssmRenderer in the stack DO flush the queues, but not the others
     * @param flushQueues 
     */
    public void setFlushQueues(boolean flushQueues) {
        pssmRenderer.setFlushQueues(flushQueues);
    }

    /**
     * sets the shadow compare mode see {@link CompareMode} for more info
     * @param compareMode 
     */
    final public void setCompareMode(CompareMode compareMode) {
        pssmRenderer.setCompareMode(compareMode);
    }
    
    /**
     * Sets the filtering mode for shadow edges see {@link FilterMode} for more info
     * @param filterMode 
     */
    final public void setFilterMode(FilterMode filterMode) {
        pssmRenderer.setFilterMode(filterMode);
    }
    
     /**
     * Define the length over which the shadow will fade out when using a shadowZextend
     * @param length the fade length in world units
     */
    public void setShadowZFadeLength(float length){
       pssmRenderer.setShadowZFadeLength(length);        
    }
    
     /**
     * get the length over which the shadow will fade out when using a shadowZextend
     * @return the fade length in world units
     */
    public float getShadowZFadeLength(){       
        return pssmRenderer.getShadowZFadeLength();        
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
