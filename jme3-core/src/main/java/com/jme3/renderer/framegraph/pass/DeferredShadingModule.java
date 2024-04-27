/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.pass;

import com.jme3.asset.AssetManager;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.TechniqueDef;
import com.jme3.material.logic.DeferredSinglePassLightingLogic;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.parameters.MatRenderParam;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.renderer.framegraph.parameters.TextureTargetParam;
import com.jme3.renderer.framegraph.parameters.ValueRenderParam;
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
    private final static String PASS = "DeferredPass";
    private static final String MATDEF = "Common/MatDefs/ShadingCommon/DeferredShading.j3md";
    
    protected final AssetManager assetManager;
    protected MatRenderParam[] matParams = new MatRenderParam[5];
    protected ValueRenderParam<LightList> lightList;
    protected ValueRenderParam<Boolean> executeState;
    protected ValueRenderParam<FrameBuffer> gBuffer;
    private FrameBuffer debug;
    private TextureTargetParam depthCopy;
    
    public DeferredShadingModule(AssetManager assetManager) {
        this.assetManager = assetManager;
    }
    
    @Override
    public void initialize(FrameGraph frameGraph) {
        
        super.initialize(frameGraph);
        
        Material screenMat = screenRect.getMaterial();
        RenderState rs = screenMat.getAdditionalRenderState();
        rs.setDepthTest(true);
        rs.setDepthWrite(true);
        rs.setDepthFunc(RenderState.TestFunction.Greater);
        screenMat.setBoolean("UseLightsCullMode", false);
        
        for (TechniqueDef t : screenMat.getMaterialDef().getTechniqueDefs(PASS)) {
            t.setLogic(new DeferredSinglePassLightingLogic(t, true));
        }
        
        // material render parameters automatically apply their values
        matParams[0] = addInput(new MatRenderParam(RT_0, screenMat, VarType.Texture2D));
        matParams[1] = addInput(new MatRenderParam(RT_1, screenMat, VarType.Texture2D));
        matParams[2] = addInput(new MatRenderParam(RT_2, screenMat, VarType.Texture2D));
        matParams[3] = addInput(new MatRenderParam(RT_3, screenMat, VarType.Texture2D));
        matParams[4] = addInput(new MatRenderParam(RT_4, screenMat, VarType.Texture2D));
        lightList = addInput(new ValueRenderParam<>(LIGHT_DATA));
        executeState = addInput(new ValueRenderParam<>(EXECUTE_STATE));
        gBuffer = addInput(new ValueRenderParam<>(IN_FRAME_BUFFER));
        depthCopy = addOutput(new TextureTargetParam(DEPTH_DEBUG, null));
        
        for (int i = 0; i < matParams.length; i++) {
            frameGraph.bindToOutput(GBufferModule.RENDER_TARGETS[i], matParams[i]);
        }
        frameGraph.bindToOutput(GBufferModule.LIGHT_DATA, lightList);
        frameGraph.bindToOutput(GBufferModule.EXECUTE_STATE, executeState);
        frameGraph.bindToOutput(GBufferModule.G_FRAME_BUFFER, gBuffer);
        
    }
    @Override
    public boolean prepare(RenderContext context) {
        if (debug == null) {
            debug = new FrameBuffer(context.getWidth(), context.getHeight(), 1);
            FrameBuffer.FrameBufferTextureTarget t = FrameBuffer.FrameBufferTarget.newTarget(
                    new Texture2D(context.getWidth(), context.getHeight(), GBufferModule.DEPTH_FORMAT));
            debug.setDepthTarget(t);
            depthCopy.setTextureTarget(t);
        }
        return true;
    }
    @Override
    public void execute(RenderContext context) {
        
        context.getRenderer().copyFrameBuffer(gBuffer.get(),
                context.getViewPort().getOutputFrameBuffer(), false, true);

        context.getRenderer().copyFrameBuffer(gBuffer.get(), debug, false, true);
        
        screenRect.getMaterial().selectTechnique(PASS, context.getRenderManager());
        context.setDepthRange(1, 1);
        context.getRenderer().setFrameBuffer(context.getViewPort().getOutputFrameBuffer());
        screenRect.updateGeometricState();
        context.getRenderManager().renderGeometry(screenRect, lightList.get());
        
    }
    @Override
    public void reset() {}
    @Override
    protected Material createScreenMaterial() {
        return new Material(assetManager, MATDEF);
    }
    
}
