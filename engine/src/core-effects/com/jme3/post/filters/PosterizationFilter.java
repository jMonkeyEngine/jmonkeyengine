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
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;

/**
 * A Post Processing filter to change colors appear with sharp edges as if the
 * available amount of colors available was not enough to draw the true image.
 * Possibly useful in cartoon styled games. Use the strength variable to lessen
 * influence of this filter on the total result. Values from 0.2 to 0.7 appear
 * to give nice results.
 *
 * Based on an article from Geeks3D:
 *    <a href="http://www.geeks3d.com/20091027/shader-library-posterization-post-processing-effect-glsl/" rel="nofollow">http://www.geeks3d.com/20091027/shader-library-posterization-post-processing-effect-glsl/</a>
 *
 * @author: Roy Straver a.k.a. Baal Garnaal
 */
public class PosterizationFilter extends Filter {

    private int numColors = 8;
    private float gamma = 0.6f;
    private float strength = 1.0f;

    /**
     * Creates a posterization Filter
     */
    public PosterizationFilter() {
        super("PosterizationFilter");
    }

    /**
     * Creates a posterization Filter with the given number of colors
     * @param numColors 
     */
    public PosterizationFilter(int numColors) {
        this();
        this.numColors = numColors;
    }

    /**
     * Creates a posterization Filter with the given number of colors and gamma
     * @param numColors
     * @param gamma 
     */
    public PosterizationFilter(int numColors, float gamma) {
        this(numColors);
        this.gamma = gamma;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        material = new Material(manager, "Common/MatDefs/Post/Posterization.j3md");
        material.setInt("NumColors", numColors);
        material.setFloat("Gamma", gamma);
        material.setFloat("Strength", strength);
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    /**
     * Sets number of color levels used to draw the screen
     */
    public void setNumColors(int numColors) {
        this.numColors = numColors;
        if (material != null) {
            material.setInt("NumColors", numColors);
        }
    }

    /**
     * Sets gamma level used to enhange visual quality
     */
    public void setGamma(float gamma) {
        this.gamma = gamma;
        if (material != null) {
            material.setFloat("Gamma", gamma);
        }
    }

    /**
     * Sets urrent strength value, i.e. influence on final image
     */
    public void setStrength(float strength) {
        this.strength = strength;
        if (material != null) {
            material.setFloat("Strength", strength);
        }
    }

    /**
     * Returns number of color levels used
     */
    public int getNumColors() {
        return numColors;
    }

    /**
     * Returns current gamma value
     */
    public float getGamma() {
        return gamma;
    }

    /**
     * Returns current strength value, i.e. influence on final image
     */
    public float getStrength() {
        return strength;
    }
}