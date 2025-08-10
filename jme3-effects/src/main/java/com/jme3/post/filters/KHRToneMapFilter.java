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
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.io.IOException;

/**
 * Tone-mapping filter that uses khronos neutral pbr tone mapping curve.
 */
public class KHRToneMapFilter extends Filter {

    private static final float DEFAULT_EXPOSURE = 0.0f;
    private static final float DEFAULT_GAMMA = 1.0f;
    
    private final Vector3f exposure = new Vector3f(DEFAULT_EXPOSURE, DEFAULT_EXPOSURE, DEFAULT_EXPOSURE);
    private final Vector3f gamma = new Vector3f(DEFAULT_GAMMA, DEFAULT_GAMMA, DEFAULT_GAMMA);

    /**
     * Creates a tone-mapping filter with the default white-point.
     */
    public KHRToneMapFilter() {
        super("KHRToneMapFilter");
    }


    @Override
    protected boolean isRequiresDepthTexture() {
        return false;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        material = new Material(manager, "Common/MatDefs/Post/KHRToneMap.j3md");
        material.setVector3("Exposure", exposure);
        material.setVector3("Gamma", gamma);
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    /**
     * Set the exposure for the tone mapping.
     */
    public void setExposure(Vector3f whitePoint) {
        this.exposure.set(whitePoint);
    }

    /**
     * Get the exposure for the tone mapping.
     * 
     * @return The exposure vector.
     */
    public Vector3f getExposure() {
        return exposure;
    }

    
    /**
     * Set the gamma for the tone mapping.
     */
    public void setGamma(Vector3f gamma) {
        this.gamma.set(gamma);
    }

    /**
     * Get the gamma for the tone mapping.
     * 
     * @return The gamma vector.
     */
    public Vector3f getGamma() {
        return gamma;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(exposure, "exposure", new Vector3f(DEFAULT_EXPOSURE, DEFAULT_EXPOSURE, DEFAULT_EXPOSURE));
        oc.write(gamma, "gamma", new Vector3f(DEFAULT_GAMMA, DEFAULT_GAMMA, DEFAULT_GAMMA));
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        exposure.set((Vector3f)ic.readSavable("exposure", new Vector3f(DEFAULT_EXPOSURE, DEFAULT_EXPOSURE, DEFAULT_EXPOSURE)));
        gamma.set((Vector3f)ic.readSavable("gamma", new Vector3f(DEFAULT_GAMMA, DEFAULT_GAMMA, DEFAULT_GAMMA)));
    }

}
