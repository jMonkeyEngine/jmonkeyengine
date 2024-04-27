/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.pass;

import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.GeometryRenderHandler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.renderer.framegraph.parameters.TextureTargetParam;
import com.jme3.renderer.framegraph.parameters.ValueRenderParam;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import java.util.ArrayList;

/**
 *
 * @author codex
 */
public class GBufferModule extends ForwardModule implements GeometryRenderHandler {
    
    private final static String GBUFFER_PASS = "GBufferPass";
    public final static String[] RENDER_TARGETS = {
        "GBufferPass.RT0", "GBufferPass.RT1", "GBufferPass.RT2", "GBufferPass.RT3", "GBufferPass.RT4"
    };
    public final static String G_FRAME_BUFFER = "GBufferPass.Framebuffer";
    public final static String LIGHT_DATA = "GBufferPass.LightData";
    public final static String EXECUTE_STATE = "GBufferPass.ExecuteState";
    public final static Image.Format DEPTH_FORMAT = Image.Format.Depth;
    
    private final LightList lightData = new LightList(null);
    private final ArrayList<Light> tempLights = new ArrayList<>();
    private ValueRenderParam<Boolean> hasDraw;
    private FrameBuffer gBuffer;
    private ValueRenderParam<FrameBuffer> bufferParam;
    private final TextureTargetParam[] targets = new TextureTargetParam[5];
    private final ColorRGBA mask = new ColorRGBA();

    public GBufferModule() {
        super(RenderQueue.Bucket.Opaque);
    }
    
    @Override
    public void initialize(FrameGraph frameGraph) {
        super.initialize(frameGraph);
        for (int i = 0; i < targets.length; i++) {
            targets[i] = addParameter(new TextureTargetParam(RENDER_TARGETS[i], null));
        }
        addParameter(new ValueRenderParam<>(LIGHT_DATA, lightData));
        hasDraw = addParameter(new ValueRenderParam<>(EXECUTE_STATE, false));
        bufferParam = addParameter(new ValueRenderParam<>(G_FRAME_BUFFER, gBuffer));
    }
    
    @Override
    public boolean prepare(RenderContext context) {
        if (context.isSizeChanged() || gBuffer == null) {
            reshape(context.getRenderer(), context.getWidth(), context.getHeight());
        }
        return super.prepare(context);
    }

    @Override
    public void execute(RenderContext context) {
        ViewPort vp = context.getViewPort();
        String tempFT = context.getRenderManager().getForcedTechnique();
        context.getRenderer().setFrameBuffer(gBuffer);
        context.getRenderer().setBackgroundColor(mask.set(vp.getBackgroundColor()).setAlpha(0));
        context.getRenderer().clearBuffers(vp.isClearColor(), vp.isClearDepth(), vp.isClearStencil());
        context.getRenderManager().setForcedTechnique(GBUFFER_PASS);
        context.getRenderManager().setGeometryRenderHandler(this);
        super.execute(context);
        context.getRenderManager().setGeometryRenderHandler(null);
        context.getRenderManager().setForcedTechnique(tempFT);
        context.getRenderer().setBackgroundColor(vp.getBackgroundColor());
        context.getRenderer().setFrameBuffer(vp.getOutputFrameBuffer());
        for (Light light : tempLights) {
            lightData.add(light);
        }
    }
    
    @Override
    public boolean renderGeometry(RenderManager rm, Geometry geom) {
        Material material = geom.getMaterial();
        if(material.getMaterialDef().getTechniqueDefs(rm.getForcedTechnique()) == null) {
            return false;
        }
        rm.renderGeometry(geom);
        if (material.getActiveTechnique() != null) {
            if (material.getMaterialDef().getTechniqueDefs(GBUFFER_PASS) != null) {
                LightList lights = geom.getFilterWorldLights();
                for (Light light : lights) {
                    if (!tempLights.contains(light)) {
                        tempLights.add(light);
                    }
                }
                // Whether it has lights or not, material objects containing GBufferPass will perform
                // DeferredShading, and shade according to shadingModelId
                hasDraw.set(true);
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
        hasDraw.set(false);
    }
    
    protected void reshape(Renderer renderer, int w, int h) {
        if (gBuffer != null) {
            //w = gBuffer.getWidth();
            //h = gBuffer.getHeight();
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
        textures[4] = new Texture2D(w, h, DEPTH_FORMAT);
        //gBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.RGBA8));
        //for (int i = 0; i < textures.length; i++) {
        //    FrameBuffer.FrameBufferTextureTarget t = FrameBuffer.FrameBufferTarget.newTarget(textures[i]);
        //    gBuffer.addColorTarget(t);
        //    targets[i].setTextureTarget(t);
        //}
        FrameBuffer.FrameBufferTextureTarget t0 = FrameBuffer.FrameBufferTarget.newTarget(textures[0]);
        gBuffer.addColorTarget(t0);
        targets[0].setTextureTarget(t0);
        FrameBuffer.FrameBufferTextureTarget t1 = FrameBuffer.FrameBufferTarget.newTarget(textures[1]);
        gBuffer.addColorTarget(t1);
        targets[1].setTextureTarget(t1);
        FrameBuffer.FrameBufferTextureTarget t2 = FrameBuffer.FrameBufferTarget.newTarget(textures[2]);
        gBuffer.addColorTarget(t2);
        targets[2].setTextureTarget(t2);
        FrameBuffer.FrameBufferTextureTarget t3 = FrameBuffer.FrameBufferTarget.newTarget(textures[3]);
        gBuffer.addColorTarget(t3);
        targets[3].setTextureTarget(t3);
        FrameBuffer.FrameBufferTextureTarget t4 = FrameBuffer.FrameBufferTarget.newTarget(textures[4]);
        gBuffer.setDepthTarget(t4);
        targets[4].setTextureTarget(t4);
        gBuffer.setMultiTarget(true);
        bufferParam.set(gBuffer);
    }
    
}
