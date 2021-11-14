package com.jme3.renderer.pipeline;

import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.TechniqueDef;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.VpStep;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

public class Deferred extends RenderPipeline{
    public final static String S_CONTEXT_InGBUFF_0 = "Context_InGBuff0";
    public final static String S_CONTEXT_InGBUFF_1 = "Context_InGBuff1";
    public final static String S_CONTEXT_InGBUFF_2 = "Context_InGBuff2";
    public final static String S_CONTEXT_InGBUFF_3 = "Context_InGBuff3";
    public final static String S_GBUFFER_PASS = "GBufferPass";


    private FrameBuffer gBuffer;
    private Texture2D gBufferData0;
    private Texture2D gBufferData1;
    private Texture2D gBufferData2;
    private Texture2D gBufferData3;
    private boolean reshape;

    public Deferred(TechniqueDef.Pipeline pipeline) {
        super(pipeline);
    }

    @Override
    public void begin(RenderManager rm, ViewPort vp) {
        // Make sure to create gBuffer only when needed
        if(gBuffer == null){
            reshape(vp.getCamera().getWidth(), vp.getCamera().getHeight());
        }
    }

    @Override
    public void reshape(int w, int h) {
        gBufferData0 = new Texture2D(w, h, Image.Format.RGBA16F);
        gBufferData1 = new Texture2D(w, h, Image.Format.RGBA16F);
        gBufferData2 = new Texture2D(w, h, Image.Format.RGBA16F);
        gBufferData3 = new Texture2D(w, h, Image.Format.Depth16);
        gBuffer = new FrameBuffer(w, h, 1);
        gBuffer.setDepthTexture(gBufferData3);
        gBuffer.addColorTexture(gBufferData0);
        gBuffer.addColorTexture(gBufferData1);
        gBuffer.addColorTexture(gBufferData2);
        gBuffer.setMultiTarget(true);
        reshape = true;
    }

    @Override
    public void draw(RenderManager rm, RenderQueue rq, ViewPort vp, boolean flush) {
        Camera cam = vp.getCamera();
        Renderer renderer = rm.getRenderer();
        AppProfiler prof = rm.getAppProfiler();

        // render opaque objects with default depth range
        // opaque objects are sorted front-to-back, reducing overdraw
        if (prof!=null) prof.vpStep(VpStep.RenderBucket, vp, RenderQueue.Bucket.Opaque);

        // G-Buffer Pass
        renderer.setFrameBuffer(gBuffer);
        String techOrig = rm.getForcedTechnique();
        rm.setForcedTechnique(S_GBUFFER_PASS);
        rq.renderQueue(RenderQueue.Bucket.Opaque, rm, cam, flush);
        rm.setForcedTechnique(techOrig);
        renderer.setFrameBuffer(vp.getOutputFrameBuffer());

        // Deferred Pass

        reshape = false;
    }

    @Override
    public void drawGeometry(RenderManager rm, Geometry geom) {
        Material material = geom.getMaterial();
        // Check context parameters
        MaterialDef matDef = material.getMaterialDef();
        if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_0) != null && (reshape || material.getTextureParam(S_CONTEXT_InGBUFF_0) == null)){
            material.setTexture(S_CONTEXT_InGBUFF_0, gBufferData0);
        }
        if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_1) != null && (reshape || material.getTextureParam(S_CONTEXT_InGBUFF_1) == null)){
            material.setTexture(S_CONTEXT_InGBUFF_1, gBufferData1);
        }
        if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_2) != null && (reshape || material.getTextureParam(S_CONTEXT_InGBUFF_2) == null)){
            material.setTexture(S_CONTEXT_InGBUFF_2, gBufferData2);
        }
        if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_3) != null && (reshape || material.getTextureParam(S_CONTEXT_InGBUFF_3) == null)){
            material.setTexture(S_CONTEXT_InGBUFF_3, gBufferData3);
        }
    }

    @Override
    public void end(RenderManager rm, ViewPort vp) {
        rm.getRenderer().copyFrameBuffer(gBuffer, null, false, true);
    }
}
