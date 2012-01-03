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
package com.jme3.post.filters;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;

/**
 * A Post Processing filter that makes the screen look like it was drawn as
 * diagonal lines with a pen.
 * Try combining this with a cartoon edge filter to obtain manga style visuals.
 *
 * Based on an article from Geeks3D:
 *    <a href="http://www.geeks3d.com/20110219/shader-library-crosshatching-glsl-filter/" rel="nofollow">http://www.geeks3d.com/20110219/shader-library-crosshatching-glsl-filter/</a>
 *
 * @author Roy Straver a.k.a. Baal Garnaal
 */
public class CrossHatchFilter extends Filter {

    private ColorRGBA lineColor = ColorRGBA.Black.clone();
    private ColorRGBA paperColor = ColorRGBA.White.clone();
    private float colorInfluenceLine = 0.8f;
    private float colorInfluencePaper = 0.1f;
    private float fillValue = 0.9f;
    private float luminance1 = 0.9f;
    private float luminance2 = 0.7f;
    private float luminance3 = 0.5f;
    private float luminance4 = 0.3f;
    private float luminance5 = 0.0f;
    private float lineThickness = 1.0f;
    private float lineDistance = 4.0f;

    /**
     * Creates a crossHatch filter
     */
    public CrossHatchFilter() {
        super("CrossHatchFilter");
    }

    /**
     * Creates a crossHatch filter
     * @param lineColor the colors of the lines
     * @param paperColor the paper color
     */
    public CrossHatchFilter(ColorRGBA lineColor, ColorRGBA paperColor) {
        this();
        this.lineColor = lineColor;
        this.paperColor = paperColor;
    }

    @Override
    protected boolean isRequiresDepthTexture() {
        return false;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        material = new Material(manager, "Common/MatDefs/Post/CrossHatch.j3md");
        material.setColor("LineColor", lineColor);
        material.setColor("PaperColor", paperColor);

        material.setFloat("ColorInfluenceLine", colorInfluenceLine);
        material.setFloat("ColorInfluencePaper", colorInfluencePaper);

        material.setFloat("FillValue", fillValue);

        material.setFloat("Luminance1", luminance1);
        material.setFloat("Luminance2", luminance2);
        material.setFloat("Luminance3", luminance3);
        material.setFloat("Luminance4", luminance4);
        material.setFloat("Luminance5", luminance5);

        material.setFloat("LineThickness", lineThickness);
        material.setFloat("LineDistance", lineDistance);
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    /**
     * Sets color used to draw lines
     * @param lineColor 
     */
    public void setLineColor(ColorRGBA lineColor) {
        this.lineColor = lineColor;
        if (material != null) {
            material.setColor("LineColor", lineColor);
        }
    }

    /**
     * Sets color used as background
     * @param paperColor 
     */
    public void setPaperColor(ColorRGBA paperColor) {
        this.paperColor = paperColor;
        if (material != null) {
            material.setColor("PaperColor", paperColor);
        }
    }

    /**
     * Sets color influence of original image on lines drawn
     * @param colorInfluenceLine 
     */
    public void setColorInfluenceLine(float colorInfluenceLine) {
        this.colorInfluenceLine = colorInfluenceLine;
        if (material != null) {
            material.setFloat("ColorInfluenceLine", colorInfluenceLine);
        }
    }

    /**
     * Sets color influence of original image on non-line areas
     * @param colorInfluencePaper 
     */
    public void setColorInfluencePaper(float colorInfluencePaper) {
        this.colorInfluencePaper = colorInfluencePaper;
        if (material != null) {
            material.setFloat("ColorInfluencePaper", colorInfluencePaper);
        }
    }

    /**
     * Sets line/paper color ratio for areas with values < luminance5,
     * really dark areas get no lines but a filled blob instead
     * @param fillValue 
     */
    public void setFillValue(float fillValue) {
        this.fillValue = fillValue;
        if (material != null) {
            material.setFloat("FillValue", fillValue);
        }
    }

    /**
     *
     * Sets minimum luminance levels for lines drawn
     * @param luminance1 Top-left to down right 1
     * @param luminance2 Top-right to bottom left 1
     * @param luminance3 Top-left to down right 2
     * @param luminance4 Top-right to bottom left 2
     * @param luminance5 Blobs
     */
    public void setLuminanceLevels(float luminance1, float luminance2, float luminance3, float luminance4, float luminance5) {
        this.luminance1 = luminance1;
        this.luminance2 = luminance2;
        this.luminance3 = luminance3;
        this.luminance4 = luminance4;
        this.luminance5 = luminance5;

        if (material != null) {
            material.setFloat("Luminance1", luminance1);
            material.setFloat("Luminance2", luminance2);
            material.setFloat("Luminance3", luminance3);
            material.setFloat("Luminance4", luminance4);
            material.setFloat("Luminance5", luminance5);
        }
    }

    /**
     * Sets the thickness of lines drawn
     * @param lineThickness 
     */
    public void setLineThickness(float lineThickness) {
        this.lineThickness = lineThickness;
        if (material != null) {
            material.setFloat("LineThickness", lineThickness);
        }
    }

    /**
     * Sets minimum distance between lines drawn
     * Primary lines are drawn at 2*lineDistance
     * Secondary lines are drawn at lineDistance
     * @param lineDistance 
     */
    public void setLineDistance(float lineDistance) {
        this.lineDistance = lineDistance;
        if (material != null) {
            material.setFloat("LineDistance", lineDistance);
        }
    }

    /**
     * Returns line color
     * @return 
     */
    public ColorRGBA getLineColor() {
        return lineColor;
    }

    /**
     * Returns paper background color
     * @return 
     */
    public ColorRGBA getPaperColor() {
        return paperColor;
    }

    /**
     * Returns current influence of image colors on lines
     */
    public float getColorInfluenceLine() {
        return colorInfluenceLine;
    }

    /**
     * Returns current influence of image colors on paper background
     */
    public float getColorInfluencePaper() {
        return colorInfluencePaper;
    }

    /**
     * Returns line/paper color ratio for blobs
     */
    public float getFillValue() {
        return fillValue;
    }

    /**
     * Returns the thickness of the lines drawn
     */
    public float getLineThickness() {
        return lineThickness;
    }

    /**
     * Returns minimum distance between lines
     */
    public float getLineDistance() {
        return lineDistance;
    }

    /**
     * Returns treshold for lines 1
     */
    public float getLuminance1() {
        return luminance1;
    }

    /**
     * Returns treshold for lines 2
     */
    public float getLuminance2() {
        return luminance2;
    }

    /**
     * Returns treshold for lines 3
     */
    public float getLuminance3() {
        return luminance3;
    }

    /**
     * Returns treshold for lines 4
     */
    public float getLuminance4() {
        return luminance4;
    }

    /**
     * Returns treshold for blobs
     */
    public float getLuminance5() {
        return luminance5;
    }
}