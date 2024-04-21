/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.pass;

import com.jme3.asset.AssetManager;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.MatRenderParam;
import com.jme3.renderer.framegraph.MyFrameGraph;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.renderer.framegraph.TextureTargetParam;
import com.jme3.renderer.framegraph.ValueRenderParam;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

/**
 *
 * @author codex
 */
public class DeferredShadingModule extends ScreenModule {
    
    public final static String RT_0 = "Context_InGBuff0";
    public final static String RT_1 = "Context_InGBuff1";
    public final static String RT_2 = "Context_InGBuff2";
    public final static String RT_3 = "Context_InGBuff3";
    public final static String RT_4 = "Context_InGBuff4";
    public final static String LIGHT_DATA = "DeferredShadingPass.LightData";
    public final static String EXECUTE_STATE = "DeferredShadingPass.ExecuteState";
    public final static String IN_FRAME_BUFFER = "DeferredShadingPass.InFrameBuffer";
    public final static String DEPTH_DEBUG = "DeferredShadingPass.DepthDebug";
    protected final static String DEFERRED_PASS = "DeferredPass";
    private static final String MATDEF = "Common/MatDefs/ShadingCommon/DeferredShading.j3md";
    
    protected MatRenderParam[] matParams = new MatRenderParam[5];
    protected ValueRenderParam<LightList> lightList;
    protected ValueRenderParam<Boolean> executeState;
    protected ValueRenderParam<FrameBuffer> gBuffer;
    private FrameBuffer debug;
    private TextureTargetParam depthCopy;
    
    public DeferredShadingModule(AssetManager assetManager) {
        super(assetManager, RenderQueue.Bucket.Opaque);
    }
    
    @Override
    public void initialize(MyFrameGraph frameGraph) {
        
        screenMat = createMaterial();
        screenRect = new Picture("DeferredShadingPass_Rect");
        screenRect.setWidth(1);
        screenRect.setHeight(1);
        screenRect.setMaterial(screenMat);
        
        RenderState rs = screenMat.getAdditionalRenderState();
        rs.setDepthWrite(true);
        rs.setDepthTest(true);
        rs.setDepthFunc(RenderState.TestFunction.Greater);
        //rs.setBlendMode(RenderState.BlendMode.Alpha);
        //screenMat.setTransparent(true);
        screenMat.setBoolean("UseLightsCullMode", false);
        
        // material render parameters automatically apply their values
        matParams[0] = addParameter(new MatRenderParam(RT_0, screenMat, VarType.Texture2D));
        matParams[1] = addParameter(new MatRenderParam(RT_1, screenMat, VarType.Texture2D));
        matParams[2] = addParameter(new MatRenderParam(RT_2, screenMat, VarType.Texture2D));
        matParams[3] = addParameter(new MatRenderParam(RT_3, screenMat, VarType.Texture2D));
        matParams[4] = addParameter(new MatRenderParam(RT_4, screenMat, VarType.Texture2D));
        lightList = addParameter(new ValueRenderParam<>(LIGHT_DATA));
        executeState = addParameter(new ValueRenderParam<>(EXECUTE_STATE));
        gBuffer = addParameter(new ValueRenderParam<>(IN_FRAME_BUFFER));
        
        depthCopy = addParameter(new TextureTargetParam(DEPTH_DEBUG, null));
        
        bindParameters(frameGraph);
        
    }
    
    protected Material createMaterial() {
        return new Material(assetManager, MATDEF);
    }
    
    protected void bindParameters(MyFrameGraph frameGraph) {
        for (int i = 0; i < matParams.length; i++) {
            frameGraph.bindToOutput(GBufferModule.RENDER_TARGETS[i], matParams[i]);
        }
        frameGraph.bindToOutput(GBufferModule.LIGHT_DATA, lightList);
        frameGraph.bindToOutput(GBufferModule.EXECUTE_STATE, executeState);
        frameGraph.bindToOutput(GBufferModule.G_FRAME_BUFFER, gBuffer);
    }
    
    @Override
    public void prepare(RenderContext context) {
        super.prepare(context);
        if (debug == null) {
            debug = new FrameBuffer(context.getWidth(), context.getHeight(), 1);
            FrameBuffer.FrameBufferTextureTarget t = FrameBuffer.FrameBufferTarget.newTarget(
                    new Texture2D(context.getWidth(), context.getHeight(), GBufferModule.DEPTH_FORMAT));
            debug.setDepthTarget(t);
            depthCopy.setTextureTarget(t);
        }
    }
    
    @Override
    public void executeDrawCommands(RenderContext context) {
                
        context.getRenderer().copyFrameBuffer(gBuffer.produce(),
                context.getViewPort().getOutputFrameBuffer(), false, true);
        
        context.getRenderer().copyFrameBuffer(gBuffer.produce(), debug, false, true);
        
        context.setDepthRange(1, 1);
        context.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
        screenMat.selectTechnique(DEFERRED_PASS, context.getRenderManager());
        screenRect.updateGeometricState();
        context.getRenderManager().renderGeometry(screenRect);
        //screenMat.render(screenRect, lightList.produce(), context.getRenderManager());
        
    }
    
    @Override
    public void dispatchPassSetup(RenderQueue queue) {
        //boolean exState = executeState.validate() && executeState.produce();
        //boolean hasLightData = lightList.validate() && lightList.produce().size() > 0;
        //canExecute = hasLightData || exState;
        canExecute = true;
    }
    
    @Override
    public boolean drawGeometry(RenderManager rm, Geometry geom) {
        return true;
    }
    
}
