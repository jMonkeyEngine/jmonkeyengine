/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.pass;

import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.MyFrameGraph;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.renderer.framegraph.TextureTargetParam;
import com.jme3.renderer.framegraph.ValueRenderParam;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import java.util.ArrayList;

/**
 *
 * @author codex
 */
public class GBufferModule extends OpaqueModule {
    
    private final static String GBUFFER_PASS = "GBufferPass";
    public final static String[] RENDER_TARGETS = {"RT_0", "RT_1", "RT_2", "RT_3", "RT_4"};
    public final static String G_FRAME_BUFFER = "GBufferFramebuffer";
    public final static String LIGHT_DATA = "LIGHT_DATA";
    public final static String EXECUTE_STATE = "EXECUTE_STATE";
    
    private final LightList lightData = new LightList(null);
    private final ArrayList<Light> tempLights = new ArrayList<>();
    private ValueRenderParam<Boolean> hasDraw;
    private FrameBuffer gBuffer;
    private ValueRenderParam<FrameBuffer> bufferParam;
    private final TextureTargetParam[] targets = new TextureTargetParam[5];
    private final ColorRGBA mask = new ColorRGBA(0, 0, 0, 0);
    private int width = -1, height = -1;
    
    @Override
    public void initialize(MyFrameGraph frameGraph) {
        super.initialize(frameGraph);
        for (int i = 0; i < targets.length; i++) {
            targets[i] = addParameter(new TextureTargetParam(RENDER_TARGETS[i], null));
        }
        addParameter(new ValueRenderParam<>(LIGHT_DATA, lightData));
        hasDraw = addParameter(new ValueRenderParam<>(EXECUTE_STATE, false));
        bufferParam = addParameter(new ValueRenderParam<>(G_FRAME_BUFFER, gBuffer));
    }
    
    @Override
    public void prepare(RenderContext context) {
        super.prepare(context);
        ViewPort vp = getViewPort(context);
        reshape(context.getRenderer(), vp.getCamera().getWidth(), vp.getCamera().getHeight());
    }

    @Override
    public void executeDrawCommands(RenderContext context) {
        if(canExecute){
            hasDraw.accept(false);
            tempLights.clear();
            lightData.clear();
            ViewPort vp = getViewPort(context);
            //reshape(context.getRenderer(), vp.getCamera().getWidth(), vp.getCamera().getHeight());
            FrameBuffer opfb = vp.getOutputFrameBuffer();
            vp.setOutputFrameBuffer(gBuffer);
            ColorRGBA opClearColor = vp.getBackgroundColor();
            mask.set(opClearColor);
            mask.a = 0.0f;
            context.getRenderer().setFrameBuffer(gBuffer);
            context.getRenderer().setBackgroundColor(mask);
            context.getRenderer().clearBuffers(vp.isClearColor(), vp.isClearDepth(), vp.isClearStencil());
            String techOrig = context.getRenderManager().getForcedTechnique();
            context.getRenderManager().setForcedTechnique(GBUFFER_PASS);
            super.executeDrawCommands(context);
            context.getRenderManager().setForcedTechnique(techOrig);
            vp.setOutputFrameBuffer(opfb);
            context.getRenderer().setBackgroundColor(opClearColor);
            context.getRenderer().setFrameBuffer(vp.getOutputFrameBuffer());
            //bHasDrawVarSource.setValue(bHasDraw);
            if (hasDraw.produce()) {
                for(Light light : tempLights){
                    lightData.add(light);
                }
                //context.getRenderer().copyFrameBuffer(gBuffer, vp.getOutputFrameBuffer(), false, true);
            }
        }
    }
    
    @Override
    public boolean drawGeometry(RenderManager rm, Geometry geom) {
        Material material = geom.getMaterial();
        if(material.getMaterialDef().getTechniqueDefs(rm.getForcedTechnique()) == null)return false;
        rm.renderGeometry(geom);
        if(material.getActiveTechnique() != null){
            if(material.getMaterialDef().getTechniqueDefs(GBUFFER_PASS) != null){
                LightList lights = geom.getFilterWorldLights();
                for(Light light : lights){
                    if(!tempLights.contains(light)){
                        tempLights.add(light);
                    }
                }
                // Whether it has lights or not, material objects containing GBufferPass will perform
                // DeferredShading, and shade according to shadingModelId
                hasDraw.accept(true);
                return true;
            }
        }
        return false;
    }
    
    @Override
    public void reset() {
        super.reset();
        tempLights.clear();
        lightData.clear();
        hasDraw.accept(false);
    }
    
    protected void reshape(Renderer renderer, int w, int h) {
        if (width == w && height == h) {
            return;
        }
        width = w;
        height = h;
        if (gBuffer != null) {
            gBuffer.dispose();
            gBuffer.deleteObject(renderer);
        }
        gBuffer = new FrameBuffer(w, h, 1);
        // To ensure accurate results, 32bit is used here for generalization.
        Texture2D[] textures = new Texture2D[5];
        textures[0] = new Texture2D(w, h, Image.Format.RGBA16F);
        textures[1] = new Texture2D(w, h, Image.Format.RGBA16F);
        textures[2] = new Texture2D(w, h, Image.Format.RGBA16F);
        // The third buffer provides 32-bit floating point to store high-precision information, such as normals
        textures[3] = new Texture2D(w, h, Image.Format.RGBA32F);
        // Depth16/Depth32/Depth32F provide higher precision to prevent clipping when camera gets close,
        // but it seems some devices do not support copying Depth16/Depth32/Depth32F to default FrameBuffer.
        textures[4] = new Texture2D(w, h, Image.Format.Depth);
        for (int i = 0; i < textures.length; i++) {
            FrameBuffer.FrameBufferTextureTarget t = FrameBuffer.FrameBufferTarget.newTarget(textures[i]);
            gBuffer.addColorTarget(t);
            targets[i].setTextureTarget(t);
        }
        gBuffer.setMultiTarget(true);
        bufferParam.accept(gBuffer);
    }
    
    protected ViewPort getViewPort(RenderContext context) {
        if (forcedViewPort == null) {
            return context.getViewPort();
        } else {
            return forcedViewPort;
        }
    }
    
}
