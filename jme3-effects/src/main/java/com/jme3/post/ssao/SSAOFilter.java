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
package com.jme3.post.ssao;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.post.Filter.Pass;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.shader.VarType;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.util.ArrayList;

/**
 * SSAO stands for screen space ambient occlusion
 * It's a technique that fake ambient lighting by computing shadows that near by objects would casts on each others 
 * under the effect of an ambient light
 * more info on this in this blog post <a href="http://jmonkeyengine.org/2010/08/16/screen-space-ambient-occlusion-for-jmonkeyengine-3-0/">http://jmonkeyengine.org/2010/08/16/screen-space-ambient-occlusion-for-jmonkeyengine-3-0/</a>
 * 
 * @author Rémy Bouquet aka Nehon
 */
public class SSAOFilter extends Filter {

    private Pass normalPass;
    private Vector3f frustumCorner;
    private Vector2f frustumNearFar;
    private Vector2f[] samples = {new Vector2f(1.0f, 0.0f), new Vector2f(-1.0f, 0.0f), new Vector2f(0.0f, 1.0f), new Vector2f(0.0f, -1.0f)};
    private float sampleRadius = 5.1f;
    private float intensity = 1.5f;
    private float scale = 0.2f;
    private float bias = 0.1f;
    private boolean useOnlyAo = false;
    private boolean useAo = true;
    private Material ssaoMat;
    private Pass ssaoPass;
//    private Material downSampleMat;
//    private Pass downSamplePass;
    private float downSampleFactor = 1f;
    private RenderManager renderManager;
    private ViewPort viewPort;
    private boolean approximateNormals = false;

    /**
     * Create a Screen Space Ambient Occlusion Filter
     */
    public SSAOFilter() {
        super("SSAOFilter");
    }

    /**
     * Create a Screen Space Ambient Occlusion Filter
     * @param sampleRadius The radius of the area where random samples will be picked. default 5.1f
     * @param intensity intensity of the resulting AO. default 1.2f
     * @param scale distance between occluders and occludee. default 0.2f
     * @param bias the width of the occlusion cone considered by the occludee. default 0.1f
     */
    public SSAOFilter(float sampleRadius, float intensity, float scale, float bias) {
        this();
        this.sampleRadius = sampleRadius;
        this.intensity = intensity;
        this.scale = scale;
        this.bias = bias;
    }

    @Override
    protected boolean isRequiresDepthTexture() {
        return true;
    }

    @Override
    protected void postQueue(RenderQueue queue) {
        if(!approximateNormals) {
            Renderer r = renderManager.getRenderer();
            r.setFrameBuffer(normalPass.getRenderFrameBuffer());
            renderManager.getRenderer().clearBuffers(true, true, true);
            renderManager.setForcedTechnique("PreNormalPass");
            renderManager.renderViewPortQueues(viewPort, false);
            renderManager.setForcedTechnique(null);
            renderManager.getRenderer().setFrameBuffer(viewPort.getOutputFrameBuffer());
        }
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        this.renderManager = renderManager;
        this.viewPort = vp;
        int screenWidth = w;
        int screenHeight = h;
        postRenderPasses = new ArrayList<Pass>();

        normalPass = new Pass();
        normalPass.init(renderManager.getRenderer(), (int) (screenWidth / downSampleFactor), (int) (screenHeight / downSampleFactor), Format.RGBA8, Format.Depth);


        frustumNearFar = new Vector2f();

        float farY = (vp.getCamera().getFrustumTop() / vp.getCamera().getFrustumNear()) * vp.getCamera().getFrustumFar();
        float farX = farY * ((float) screenWidth / (float) screenHeight);
        frustumCorner = new Vector3f(farX, farY, vp.getCamera().getFrustumFar());
        frustumNearFar.x = vp.getCamera().getFrustumNear();
        frustumNearFar.y = vp.getCamera().getFrustumFar();





        //ssao Pass
        ssaoMat = new Material(manager, "Common/MatDefs/SSAO/ssao.j3md");
        ssaoMat.setTexture("Normals", normalPass.getRenderedTexture());
        Texture random = manager.loadTexture("Common/MatDefs/SSAO/Textures/random.png");
        random.setWrap(Texture.WrapMode.Repeat);
        ssaoMat.setTexture("RandomMap", random);

        ssaoPass = new Pass("SSAO pass") {

            @Override
            public boolean requiresDepthAsTexture() {
                return true;
            }
        };

        ssaoPass.init(renderManager.getRenderer(), (int) (screenWidth / downSampleFactor), (int) (screenHeight / downSampleFactor), Format.RGBA8, Format.Depth, 1, ssaoMat);
//        ssaoPass.getRenderedTexture().setMinFilter(Texture.MinFilter.Trilinear);
//        ssaoPass.getRenderedTexture().setMagFilter(Texture.MagFilter.Bilinear);
        postRenderPasses.add(ssaoPass);
        material = new Material(manager, "Common/MatDefs/SSAO/ssaoBlur.j3md");
        material.setTexture("SSAOMap", ssaoPass.getRenderedTexture());

        ssaoMat.setVector3("FrustumCorner", frustumCorner);
        ssaoMat.setFloat("SampleRadius", sampleRadius);
        ssaoMat.setFloat("Intensity", intensity);
        ssaoMat.setFloat("Scale", scale);
        ssaoMat.setFloat("Bias", bias);
        material.setBoolean("UseAo", useAo);
        material.setBoolean("UseOnlyAo", useOnlyAo);
        ssaoMat.setVector2("FrustumNearFar", frustumNearFar);
        material.setVector2("FrustumNearFar", frustumNearFar);
        ssaoMat.setParam("Samples", VarType.Vector2Array, samples);
        ssaoMat.setBoolean("ApproximateNormals", approximateNormals);

        float xScale = 1.0f / w;
        float yScale = 1.0f / h;

        float blurScale = 2f;
        material.setFloat("XScale", blurScale * xScale);
        material.setFloat("YScale", blurScale * yScale);

    }

    @Override
    protected void cleanUpFilter(Renderer r) {
        normalPass.cleanup(r);
    }    
    
    /**
     * Return the bias<br>
     * see {@link  #setBias(float bias)}
     * @return  bias
     */
    public float getBias() {
        return bias;
    }

    /**
     * Sets the the width of the occlusion cone considered by the occludee default is 0.1f
     * @param bias 
     */
    public void setBias(float bias) {
        this.bias = bias;
        if (ssaoMat != null) {
            ssaoMat.setFloat("Bias", bias);
        }
    }

    /**
     * returns the ambient occlusion intensity
     * @return intensity
     */
    public float getIntensity() {
        return intensity;
    }

    /**
     * Sets the Ambient occlusion intensity default is 1.2f
     * @param intensity 
     */
    public void setIntensity(float intensity) {
        this.intensity = intensity;
        if (ssaoMat != null) {
            ssaoMat.setFloat("Intensity", intensity);
        }

    }

    /**
     * returns the sample radius<br>
     * see {link setSampleRadius(float sampleRadius)}
     * @return the sample radius
     */
    public float getSampleRadius() {
        return sampleRadius;
    }

    /**
     * Sets the radius of the area where random samples will be picked dafault 5.1f     
     * @param sampleRadius 
     */
    public void setSampleRadius(float sampleRadius) {
        this.sampleRadius = sampleRadius;
        if (ssaoMat != null) {
            ssaoMat.setFloat("SampleRadius", sampleRadius);
        }

    }

    /**
     * returns the scale<br>
     * see {@link #setScale(float scale)}
     * @return scale
     */
    public float getScale() {
        return scale;
    }

    /**
     * 
     * Returns the distance between occluders and occludee. default 0.2f
     * @param scale 
     */
    public void setScale(float scale) {
        this.scale = scale;
        if (ssaoMat != null) {
            ssaoMat.setFloat("Scale", scale);
        }
    }

    /**
     * debugging only , will be removed
     * @return Whether or not
     */
    public boolean isUseAo() {
        return useAo;
    }

    /**
     * debugging only , will be removed
     */
    public void setUseAo(boolean useAo) {
        this.useAo = useAo;
        if (material != null) {
            material.setBoolean("UseAo", useAo);
        }

    }

    public void setApproximateNormals(boolean approximateNormals) {
        this.approximateNormals = approximateNormals;
        if (ssaoMat != null) {
            ssaoMat.setBoolean("ApproximateNormals", approximateNormals);
        }
    }

    public boolean isApproximateNormals() {
        return approximateNormals;
    }

    /**
     * debugging only , will be removed
     * @return useOnlyAo
     */
    public boolean isUseOnlyAo() {
        return useOnlyAo;
    }

    /**
     * debugging only , will be removed
     */
    public void setUseOnlyAo(boolean useOnlyAo) {
        this.useOnlyAo = useOnlyAo;
        if (material != null) {
            material.setBoolean("UseOnlyAo", useOnlyAo);
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(sampleRadius, "sampleRadius", 5.1f);
        oc.write(intensity, "intensity", 1.5f);
        oc.write(scale, "scale", 0.2f);
        oc.write(bias, "bias", 0.1f);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        sampleRadius = ic.readFloat("sampleRadius", 5.1f);
        intensity = ic.readFloat("intensity", 1.5f);
        scale = ic.readFloat("scale", 0.2f);
        bias = ic.readFloat("bias", 0.1f);
    }
}
