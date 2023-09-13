package com.jme3.renderer.pipeline;

import com.jme3.light.Light;
import com.jme3.light.LightList;
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
import com.jme3.ui.Picture;

import java.util.ArrayList;
import java.util.List;

public class Deferred extends RenderPipeline{
    public final static String S_CONTEXT_InGBUFF_0 = "Context_InGBuff0";
    public final static String S_CONTEXT_InGBUFF_1 = "Context_InGBuff1";
    public final static String S_CONTEXT_InGBUFF_2 = "Context_InGBuff2";
    public final static String S_CONTEXT_InGBUFF_3 = "Context_InGBuff3";
    public final static String S_CONTEXT_InGBUFF_4 = "Context_InGBuff4";
    public final static String S_GBUFFER_PASS = "GBufferPass";
    public final static String S_DEFERRED_LIGHTING_PASS = "DeferredLighting";


    private FrameBuffer gBuffer;
    private Texture2D gBufferData0;
    private Texture2D gBufferData1;
    private Texture2D gBufferData2;
    private Texture2D gBufferData3;
    private Texture2D gBufferData4;
    private Material fsMat;
    private Picture fsQuad;
    private boolean reshape;
    private boolean drawing;
    private final List<Light> tempLights = new ArrayList<Light>();
    private final LightList filteredLightList = new LightList(null);

    public Deferred(TechniqueDef.Pipeline pipeline) {
        super(pipeline);
    }

    @Override
    public void begin(RenderManager rm, ViewPort vp) {
        // Make sure to create gBuffer only when needed
        if(gBuffer == null){
            fsQuad = new Picture("filter full screen quad");
            fsQuad.setWidth(1);
            fsQuad.setHeight(1);
            reshape(vp.getCamera().getWidth(), vp.getCamera().getHeight());
        }
        tempLights.clear();
        filteredLightList.clear();
    }

    @Override
    public void reshape(int w, int h) {
        gBufferData0 = new Texture2D(w, h, Image.Format.RGBA16F);
        gBufferData1 = new Texture2D(w, h, Image.Format.RGBA16F);
        gBufferData2 = new Texture2D(w, h, Image.Format.RGBA16F);
        gBufferData3 = new Texture2D(w, h, Image.Format.RGBA32F);   // The third buffer provides 32-bit floating point to store high-precision information, such as normals
        // todo:后续调整为Depth24Stencil8,然后使用一个SceneColorFBO用于渲染所有3D部分,然后将其color_attach_0复制到BackBuffer中
        // todo:然后开启DepthTest绘制最后的所有GUI
        gBufferData4 = new Texture2D(w, h, Image.Format.Depth);
        gBuffer = new FrameBuffer(w, h, 1);
        gBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(gBufferData0));
        gBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(gBufferData1));
        gBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(gBufferData2));
        gBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(gBufferData3));
        gBuffer.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(gBufferData4));
        gBuffer.setMultiTarget(true);
        reshape = true;
    }

    @Override
    public void draw(RenderManager rm, RenderQueue rq, ViewPort vp, boolean flush) {
        if (rq.isQueueEmpty(RenderQueue.Bucket.Opaque)) {
            drawing = false;
            return;
        }
//        drawing = true;
        Camera cam = vp.getCamera();
        Renderer renderer = rm.getRenderer();
        AppProfiler prof = rm.getAppProfiler();

        // render opaque objects with default depth range
        // opaque objects are sorted front-to-back, reducing overdraw
        if (prof!=null) prof.vpStep(VpStep.RenderBucket, vp, RenderQueue.Bucket.Opaque);

        // G-Buffer Pass
        FrameBuffer opfb = vp.getOutputFrameBuffer();
        vp.setOutputFrameBuffer(gBuffer);
        renderer.setFrameBuffer(gBuffer);
        renderer.clearBuffers(vp.isClearColor(), vp.isClearDepth(), vp.isClearStencil());
        String techOrig = rm.getForcedTechnique();
        rm.setForcedTechnique(S_GBUFFER_PASS);
        rq.renderQueue(RenderQueue.Bucket.Opaque, rm, cam, flush);
        rm.setForcedTechnique(techOrig);
        vp.setOutputFrameBuffer(opfb);
        renderer.setFrameBuffer(vp.getOutputFrameBuffer());

        // Deferred Pass
        if(fsMat != null){
            // Check context parameters
            MaterialDef matDef = fsMat.getMaterialDef();
            if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_0) != null && (reshape || fsMat.getTextureParam(S_CONTEXT_InGBUFF_0) == null)){
                fsMat.setTexture(S_CONTEXT_InGBUFF_0, gBufferData0);
            }
            if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_1) != null && (reshape || fsMat.getTextureParam(S_CONTEXT_InGBUFF_1) == null)){
                fsMat.setTexture(S_CONTEXT_InGBUFF_1, gBufferData1);
            }
            if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_2) != null && (reshape || fsMat.getTextureParam(S_CONTEXT_InGBUFF_2) == null)){
                fsMat.setTexture(S_CONTEXT_InGBUFF_2, gBufferData2);
            }
            if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_3) != null && (reshape || fsMat.getTextureParam(S_CONTEXT_InGBUFF_3) == null)){
                fsMat.setTexture(S_CONTEXT_InGBUFF_3, gBufferData3);
            }
            if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_4) != null && (reshape || fsMat.getTextureParam(S_CONTEXT_InGBUFF_4) == null)){
                fsMat.setTexture(S_CONTEXT_InGBUFF_4, gBufferData4);
            }
            for(Light l : tempLights){
                filteredLightList.add(l);
            }
            boolean depthWrite = fsMat.getAdditionalRenderState().isDepthWrite();
            fsMat.getAdditionalRenderState().setDepthWrite(false);
            fsQuad.setMaterial(fsMat);
            fsQuad.updateGeometricState();
            fsMat.render(fsQuad, filteredLightList, rm);
    //        rm.renderGeometry(fsQuad);
            fsMat.getAdditionalRenderState().setDepthWrite(depthWrite);
        }
        else{
            drawing = false;
        }

        reshape = false;
    }

    @Override
    public boolean drawGeometry(RenderManager rm, Geometry geom) {
        Material material = geom.getMaterial();
//        // Check context parameters
//        MaterialDef matDef = material.getMaterialDef();
//        if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_0) != null && (reshape || material.getTextureParam(S_CONTEXT_InGBUFF_0) == null)){
//            material.setTexture(S_CONTEXT_InGBUFF_0, gBufferData0);
//        }
//        if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_1) != null && (reshape || material.getTextureParam(S_CONTEXT_InGBUFF_1) == null)){
//            material.setTexture(S_CONTEXT_InGBUFF_1, gBufferData1);
//        }
//        if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_2) != null && (reshape || material.getTextureParam(S_CONTEXT_InGBUFF_2) == null)){
//            material.setTexture(S_CONTEXT_InGBUFF_2, gBufferData2);
//        }
//        if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_3) != null && (reshape || material.getTextureParam(S_CONTEXT_InGBUFF_3) == null)){
//            material.setTexture(S_CONTEXT_InGBUFF_3, gBufferData3);
//        }
        rm.renderGeometry(geom);
        if(material.getActiveTechnique() != null){
            // todo:应该使用一个统一的材质材质,其中根据lightModeId分开着色
            if(material.getMaterialDef().getTechniqueDefs(S_DEFERRED_LIGHTING_PASS) != null || rm.joinPipeline(material.getActiveTechnique().getDef().getPipeline())){
                if(fsMat == null){
                    fsMat = material.clone();
                    fsMat.selectTechnique(S_DEFERRED_LIGHTING_PASS, rm);
                }
                LightList lights = geom.getFilterWorldLights();
                for(Light light : lights){
                    if(!tempLights.contains(light)){
                        tempLights.add(light);
                    }
                }
                drawing = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public void end(RenderManager rm, ViewPort vp) {
        if(drawing){
//            System.out.println("copy depth!");
            rm.getRenderer().copyFrameBuffer(gBuffer, vp.getOutputFrameBuffer(), false, true);
        }
    }
}
