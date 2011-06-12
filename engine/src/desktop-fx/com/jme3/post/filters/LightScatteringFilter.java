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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import java.io.IOException;

/**
 *
 * @author nehon
 */
public class LightScatteringFilter extends Filter {

    private Vector3f lightPosition;
    private Vector3f screenLightPos = new Vector3f();
    private int nbSamples = 50;
    private float blurStart = 0.02f;
    private float blurWidth = 0.9f;
    private float lightDensity = 1.4f;
    private boolean adaptative = true;
    Vector3f viewLightPos = new Vector3f();
    private boolean display=true;
    private float innerLightDensity;

    public LightScatteringFilter() {
        super("Light Scattering");
    }

    public LightScatteringFilter(Vector3f lightPosition) {
        this();
        this.lightPosition = lightPosition;
    }

    @Override
    public boolean isRequiresDepthTexture() {
        return true;
    }

    @Override
    public Material getMaterial() {
        material.setVector3("LightPosition", screenLightPos);
        material.setInt("NbSamples", nbSamples);
        material.setFloat("BlurStart", blurStart);
        material.setFloat("BlurWidth", blurWidth);
        material.setFloat("LightDensity", innerLightDensity);
        material.setBoolean("Display", display);
        return material;
    }

    @Override
    public void postQueue(RenderManager renderManager, ViewPort viewPort) {
        getClipCoordinates(lightPosition, screenLightPos, viewPort.getCamera());
        //  screenLightPos.x = screenLightPos.x / viewPort.getCamera().getWidth();
        //  screenLightPos.y = screenLightPos.y / viewPort.getCamera().getHeight();

        viewPort.getCamera().getViewMatrix().mult(lightPosition, viewLightPos);
        //System.err.println("viewLightPos "+viewLightPos);
        display = screenLightPos.x < 1.6f && screenLightPos.x > -0.6f && screenLightPos.y < 1.6f && screenLightPos.y > -0.6f && viewLightPos.z < 0;
//System.err.println("camdir "+viewPort.getCamera().getDirection());
//System.err.println("lightPos "+lightPosition);
//System.err.println("screenLightPos "+screenLightPos);
        if (adaptative) {
            innerLightDensity = Math.max(lightDensity - Math.max(screenLightPos.x, screenLightPos.y), 0.0f);
        }else{
            innerLightDensity=lightDensity;
        }
    }

    public Vector3f getClipCoordinates(Vector3f worldPosition, Vector3f store, Camera cam) {

        float w = cam.getViewProjectionMatrix().multProj(worldPosition, store);
        store.divideLocal(w);

        store.x = ((store.x + 1f) * (cam.getViewPortRight() - cam.getViewPortLeft()) / 2f + cam.getViewPortLeft());
        store.y = ((store.y + 1f) * (cam.getViewPortTop() - cam.getViewPortBottom()) / 2f + cam.getViewPortBottom());
        store.z = (store.z + 1f) / 2f;

        return store;
    }

    @Override
    public void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        material = new Material(manager, "Common/MatDefs/Post/LightScattering.j3md");
    }

    public float getBlurStart() {
        return blurStart;
    }

    public void setBlurStart(float blurStart) {
        this.blurStart = blurStart;
    }

    public float getBlurWidth() {
        return blurWidth;
    }

    public void setBlurWidth(float blurWidth) {
        this.blurWidth = blurWidth;
    }

    public float getLightDensity() {
        return lightDensity;
    }

    public void setLightDensity(float lightDensity) {
        this.lightDensity = lightDensity;
    }

    public Vector3f getLightPosition() {
        return lightPosition;
    }

    public void setLightPosition(Vector3f lightPosition) {
        this.lightPosition = lightPosition;
    }

    public int getNbSamples() {
        return nbSamples;
    }

    public void setNbSamples(int nbSamples) {
        this.nbSamples = nbSamples;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(lightPosition, "lightPosition", Vector3f.ZERO);
        oc.write(nbSamples, "nbSamples", 50);
        oc.write(blurStart, "blurStart", 0.02f);
        oc.write(blurWidth, "blurWidth", 0.9f);
        oc.write(lightDensity, "lightDensity", 1.4f);
        oc.write(adaptative, "adaptative", true);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        lightPosition = (Vector3f) ic.readSavable("lightPosition", Vector3f.ZERO);
        nbSamples = ic.readInt("nbSamples", 50);
        blurStart = ic.readFloat("blurStart", 0.02f);
        blurWidth = ic.readFloat("blurWidth", 0.9f);
        lightDensity = ic.readFloat("lightDensity", 1.4f);
        adaptative = ic.readBoolean("adaptative", true);
    }

    @Override
    public void cleanUpFilter(Renderer r) {
    }
}
