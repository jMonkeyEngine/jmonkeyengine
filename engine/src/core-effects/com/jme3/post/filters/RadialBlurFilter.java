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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.shader.VarType;
import java.io.IOException;

/**
 * Radially blurs the scene from the center of it
 * @author Rémy Bouquet aka Nehon
 */
public class RadialBlurFilter extends Filter {

    private float sampleDist = 1.0f;
    private float sampleStrength = 2.2f;
    private float[] samples = {-0.08f, -0.05f, -0.03f, -0.02f, -0.01f, 0.01f, 0.02f, 0.03f, 0.05f, 0.08f};

    /**
     * Creates a RadialBlurFilter
     */
    public RadialBlurFilter() {
        super("Radial blur");
    }

    /**
     * Creates a RadialBlurFilter
     * @param sampleDist the distance between samples
     * @param sampleStrength the strenght of each sample
     */
    public RadialBlurFilter(float sampleDist, float sampleStrength) {
        this();
        this.sampleDist = sampleDist;
        this.sampleStrength = sampleStrength;
    }

    @Override
    protected Material getMaterial() {

        material.setFloat("SampleDist", sampleDist);
        material.setFloat("SampleStrength", sampleStrength);
        material.setParam("Samples", VarType.FloatArray, samples);

        return material;
    }

    /**
     * return the sample distance
     * @return 
     */
    public float getSampleDistance() {
        return sampleDist;
    }

    /**
     * sets the samples distances default is 1
     * @param sampleDist 
     */
    public void setSampleDistance(float sampleDist) {
        this.sampleDist = sampleDist;
    }

    /**
     * 
     * @return 
     * @deprecated use {@link #getSampleDistance()}
     */
    @Deprecated
    public float getSampleDist() {
        return sampleDist;
    }

    /**
     * 
     * @param sampleDist
     * @deprecated use {@link #setSampleDistance(float sampleDist)}
     */
    @Deprecated
    public void setSampleDist(float sampleDist) {
        this.sampleDist = sampleDist;
    }

    /**
     * Returns the sample Strength
     * @return 
     */
    public float getSampleStrength() {
        return sampleStrength;
    }

    /**
     * sets the sample streanght default is 2.2
     * @param sampleStrength 
     */
    public void setSampleStrength(float sampleStrength) {
        this.sampleStrength = sampleStrength;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        material = new Material(manager, "Common/MatDefs/Blur/RadialBlur.j3md");
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(sampleDist, "sampleDist", 1.0f);
        oc.write(sampleStrength, "sampleStrength", 2.2f);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        sampleDist = ic.readFloat("sampleDist", 1.0f);
        sampleStrength = ic.readFloat("sampleStrength", 2.2f);
    }
}
