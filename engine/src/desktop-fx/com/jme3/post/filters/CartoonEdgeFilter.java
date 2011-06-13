/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.post.filters;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.post.Filter;
import com.jme3.post.Filter.Pass;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.Image.Format;

/**
 * Applies a cartoon-style edge detection filter to all objects in the scene.
 *
 * @author Kirill Vainer
 */
public class CartoonEdgeFilter extends Filter {

    private Pass normalPass;
    private float edgeWidth = 1.0f;
    private float edgeIntensity = 1.0f;
    private float normalThreshold = 0.5f;
    private float depthThreshold = 0.1f;
    private float normalSensitivity = 1.0f;
    private float depthSensitivity = 10.0f;
    private ColorRGBA edgeColor = new ColorRGBA(0, 0, 0, 1);

    /**
     * Creates a CartoonEdgeFilter
     */
    public CartoonEdgeFilter() {
        super("CartoonEdgeFilter");
    }

    @Override
    protected boolean isRequiresDepthTexture() {
        return true;
    }

    @Override
    protected void postQueue(RenderManager renderManager, ViewPort viewPort) {
        Renderer r = renderManager.getRenderer();
        r.setFrameBuffer(normalPass.getRenderFrameBuffer());
        renderManager.getRenderer().clearBuffers(true, true, true);
        renderManager.setForcedTechnique("PreNormalPass");
        renderManager.renderViewPortQueues(viewPort, false);
        renderManager.setForcedTechnique(null);
        renderManager.getRenderer().setFrameBuffer(viewPort.getOutputFrameBuffer());
    }

    @Override
    protected Material getMaterial() {
        material.setTexture("NormalsTexture", normalPass.getRenderedTexture());
        return material;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        normalPass = new Pass();
        normalPass.init(renderManager.getRenderer(), w, h, Format.RGBA8, Format.Depth);
        material = new Material(manager, "Common/MatDefs/Post/CartoonEdge.j3md");
        material.setFloat("EdgeWidth", edgeWidth);
        material.setFloat("EdgeIntensity", edgeIntensity);
        material.setFloat("NormalThreshold", normalThreshold);
        material.setFloat("DepthThreshold", depthThreshold);
        material.setFloat("NormalSensitivity", normalSensitivity);
        material.setFloat("DepthSensitivity", depthSensitivity);
        material.setColor("EdgeColor", edgeColor);
    }

    /**
     * Return the depth sensitivity<br>
     * for more details see {@link setDepthSensitivity(float depthSensitivity)}
     * @return 
     */
    public float getDepthSensitivity() {
        return depthSensitivity;
    }

    /**
     * sets the depth sensitivity<br>
     * defines how much depth will influence edges, default is 10
     * @param depthSensitivity 
     */
    public void setDepthSensitivity(float depthSensitivity) {
        this.depthSensitivity = depthSensitivity;
        if (material != null) {
            material.setFloat("DepthSensitivity", depthSensitivity);
        }
    }

    /**
     * returns the depth threshold<br>
     * for more details see {@link setDepthThreshold(float depthThreshold)}
     * @return 
     */
    public float getDepthThreshold() {
        return depthThreshold;
    }

    /**
     * sets the depth threshold<br>
     * Defines at what threshold of difference of depth an edge is outlined default is 0.1f
     * @param depthThreshold 
     */
    public void setDepthThreshold(float depthThreshold) {
        this.depthThreshold = depthThreshold;
        if (material != null) {
            material.setFloat("DepthThreshold", depthThreshold);
        }
    }

    /**
     * returns the edge intensity<br>
     * for more details see {@link setEdgeIntensity(float edgeIntensity) }
     * @return 
     */
    public float getEdgeIntensity() {
        return edgeIntensity;
    }

    /**
     * sets the edge intensity<br>
     * Defineshow visilble will be the outlined edges
     * @param edgeIntensity 
     */
    public void setEdgeIntensity(float edgeIntensity) {
        this.edgeIntensity = edgeIntensity;
        if (material != null) {
            material.setFloat("EdgeIntensity", edgeIntensity);
        }
    }

    /**
     * returns the width of the edges
     * @return 
     */
    public float getEdgeWidth() {
        return edgeWidth;
    }

    /**
     * sets the witdh of the edge in pixels default is 1
     * @param edgeWidth 
     */
    public void setEdgeWidth(float edgeWidth) {
        this.edgeWidth = edgeWidth;
        if (material != null) {
            material.setFloat("EdgeWidth", edgeWidth);
        }

    }

    /**
     * returns the normals sensitivity<br>
     * form more details see {@link setNormalSensitivity(float normalSensitivity)}
     * @return 
     */
    public float getNormalSensitivity() {
        return normalSensitivity;
    }

    /**
     * sets the normals sensitivity default is 1
     * @param normalSensitivity 
     */
    public void setNormalSensitivity(float normalSensitivity) {
        this.normalSensitivity = normalSensitivity;
        if (material != null) {
            material.setFloat("NormalSensitivity", normalSensitivity);
        }
    }

    /**
     * returns the normal threshold<br>
     * for more details see {@link setNormalThreshold(float normalThreshold)}
     * 
     * @return 
     */
    public float getNormalThreshold() {
        return normalThreshold;
    }

    /**
     * sets the normal threshold default is 0.5
     * @param normalThreshold 
     */
    public void setNormalThreshold(float normalThreshold) {
        this.normalThreshold = normalThreshold;
        if (material != null) {
            material.setFloat("NormalThreshold", normalThreshold);
        }
    }

    /**
     * returns the edge color
     * @return
     */
    public ColorRGBA getEdgeColor() {
        return edgeColor;
    }

    /**
     * Sets the edge color, default is black
     * @param edgeColor
     */
    public void setEdgeColor(ColorRGBA edgeColor) {
        this.edgeColor = edgeColor;
        if (material != null) {
            material.setColor("EdgeColor", edgeColor);
        }
    }
}
