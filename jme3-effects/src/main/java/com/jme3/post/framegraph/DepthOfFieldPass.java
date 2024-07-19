/*
 * Copyright (c) 2024 jMonkeyEngine
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
package com.jme3.post.framegraph;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.renderer.framegraph.passes.RenderPass;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;
import java.io.IOException;

/**
 * Port of {@link com.jme3.post.filters.DepthOfFieldFilter} to a RenderPass.
 * <p>
 * Inputs:
 * <ul>
 *   <li>Color ({@link Texture2D}): scene color texture.</li>
 *   <li>Depth ({@link Texture2D}): scene depth texture.</li>
 * </ul>
 * Outputs:
 * <ul>
 *   <li>Color ({@link Texture2D}): resulting color texture.</li>
 * </ul>
 * 
 * @author codex
 */
public class DepthOfFieldPass extends RenderPass {
    
    private ResourceTicket<Texture2D> color, depth;
    private ResourceTicket<Texture2D> result;
    private final TextureDef<Texture2D> texDef = TextureDef.texture2D();
    private Material material;
    private float focusDistance = 50f;
    private float focusRange = 10f;
    private float blurScale = 1f;
    private float blurThreshold = 0.2f;
    private boolean debugUnfocus;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        color = addInput("Color");
        depth = addInput("Depth");
        result = addOutput("Color");
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Post/DepthOfField.j3md");
        material.setFloat("FocusDistance", focusDistance);
        material.setFloat("FocusRange", focusRange);
        material.setFloat("BlurThreshold", blurThreshold);
        material.setBoolean("DebugUnfocus", debugUnfocus);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(texDef, result);
        reserve(result);
        reference(color, depth);
        texDef.setSize(context.getWidth(), context.getHeight());
        material.setFloat("XScale", blurScale/context.getWidth());
        material.setFloat("YScale", blurScale/context.getHeight());
    }
    @Override
    protected void execute(FGRenderContext context) {
        FrameBuffer fb = getFrameBuffer(context, 1);
        resources.acquireColorTarget(fb, result);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        material.setTexture("Texture", resources.acquire(color));
        material.setTexture("DepthTexture", resources.acquire(depth));
        context.renderFullscreen(material);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {
        material = null;
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(blurScale, "blurScale", 1f);
        oc.write(blurThreshold, "blurThreshold", 0.2f);
        oc.write(focusDistance, "focusDistance", 50f);
        oc.write(focusRange, "focusRange", 10f);
        oc.write(debugUnfocus, "debugUnfocus", false);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        blurScale = ic.readFloat("blurScale", 1f);
        blurThreshold = ic.readFloat("blurThreshold", 0.2f);
        focusDistance = ic.readFloat("focusDistance", 50f);
        focusRange = ic.readFloat("focusRange", 10f);
        debugUnfocus = ic.readBoolean("debugUnfocus", false);
    }
    
    /**
     *  Sets the distance at which objects are purely in focus.
     *
     * @param f the desired distance (in world units, default=50)
     */
    public void setFocusDistance(float f) {

        this.focusDistance = f;
        if (material != null) {
            material.setFloat("FocusDistance", focusDistance);
        }

    }
    
    /**
     * returns the focus distance
     * @return the distance
     */
    public float getFocusDistance() {
        return focusDistance;
    }
    
    /**
     *  Sets the range to either side of focusDistance where the
     *  objects go gradually out of focus.  Less than focusDistance - focusRange
     *  and greater than focusDistance + focusRange, objects are maximally "blurred".
     *
     * @param f the desired half extent (in world units, default=10)
     */
    public void setFocusRange(float f) {
        this.focusRange = f;
        if (material != null) {
            material.setFloat("FocusRange", focusRange);
        }

    }
    
    /**
     * returns the focus range
     * @return the distance
     */
    public float getFocusRange() {
        return focusRange;
    }
    
    /**
     *  Sets the blur amount by scaling the convolution filter up or
     *  down.  A value of 1 (the default) performs a sparse 5x5 evenly
     *  distributed convolution at pixel level accuracy.  Higher values skip
     *  more pixels, and so on until you are no longer blurring the image
     *  but simply hashing it.
     *
     *  The sparse convolution is as follows:
     *%MINIFYHTMLc3d0cd9fab65de6875a381fd3f83e1b338%*
     *  Where 'x' is the texel being modified.  Setting blur scale higher
     *  than 1 spaces the samples out.
     *
     * @param f the desired filter scale (default=1)
     */
    public void setBlurScale(float f) {
        this.blurScale = f;
    }
    
    /**
     * returns the blur scale
     * @return the scale
     */
    public float getBlurScale() {
        return blurScale;
    }
    
    /**
     *  Sets the minimum blur factor before the convolution filter is
     *  calculated.  The default is 0.2 which means if the "unfocus"
     *  amount is less than 0.2 (where 0 is no blur and 1.0 is full blurScale) 
     *  then no blur will be applied at all.  Depending on the GPU implementation,
     *  this may be an optimization since it uses branching to skip the expensive
     *  convolution filter.
     *
     *  <p>In scenes where the focus distance is close (like 0) and the focus range
     *  is relatively large, this threshold will remove some subtlety in
     *  the near-camera blurring and should be set smaller than the default
     *  or to 0 to disable completely.  Sometimes that cut-off is desired if
     *  mid-to-far field unfocusing is all that is desired.</p>
     *
     * @param f the desired blur factor (default=0.2)
     */
    public void setBlurThreshold( float f ) {
        this.blurThreshold = f;
        if (material != null) {
            material.setFloat("BlurThreshold", blurThreshold);
        }
    }
    
    /**
     * returns the blur threshold.
     * @return the threshold
     */
    public float getBlurThreshold() {
        return blurThreshold;
    }
    
    /**
     *  Turns on/off debugging of the 'unfocus' value that is used to
     *  mix the convolution filter.  When this is on, the 'unfocus' value
     *  is rendered as gray scale.  This can be used to more easily visualize
     *  where in your view the focus is centered and how steep the gradient/cutoff
     *  is, etcetera.
     *
     * @param b true to enable debugging, false to disable it (default=false)
     */
    public void setDebugUnfocus( boolean b ) {
        this.debugUnfocus = b;
        if( material != null ) {
            material.setBoolean("DebugUnfocus", debugUnfocus);
        }
    } 
    
    /**
     * 
     * @return 
     */
    public boolean getDebugUnfocus() {
        return debugUnfocus;
    }
    
}
