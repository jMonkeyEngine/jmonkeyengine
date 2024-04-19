/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.pass;

import com.jme3.asset.AssetManager;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.FGGlobal;
import com.jme3.renderer.framegraph.FrameBufferParam;
import com.jme3.renderer.framegraph.MatRenderParam;
import com.jme3.renderer.framegraph.MyFrameGraph;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.renderer.framegraph.ValueRenderParam;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.shader.VarType;
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
    public final static String LIGHT_DATA = "LIGHT_DATA";
    public final static String EXECUTE_STATE = "EXECUTE_STATE";
    protected final static String S_DEFERRED_PASS = "DeferredPass";
    private static final String MATDEF = "Common/MatDefs/ShadingCommon/DeferredShading.j3md";
    
    protected MatRenderParam[] matParams = new MatRenderParam[5];
    protected ValueRenderParam<LightList> lightList;
    protected ValueRenderParam<Boolean> executeState;
    protected FrameBufferParam frameBuffer;
    
    public DeferredShadingModule(AssetManager assetManager) {
        super(assetManager, RenderQueue.Bucket.Opaque);
    }
    
    @Override
    public void initialize(MyFrameGraph frameGraph) {
        
        screenMat = createMaterial();
        screenRect = new Picture("DeferredShadingRect");
        screenRect.setWidth(1);
        screenRect.setHeight(1);
        screenRect.setMaterial(screenMat);
        
        // material render parameters automatically apply their values
        matParams[0] = addParameter(new MatRenderParam(RT_0, screenMat, VarType.Texture2D));
        matParams[1] = addParameter(new MatRenderParam(RT_1, screenMat, VarType.Texture2D));
        matParams[2] = addParameter(new MatRenderParam(RT_2, screenMat, VarType.Texture2D));
        matParams[3] = addParameter(new MatRenderParam(RT_3, screenMat, VarType.Texture2D));
        matParams[4] = addParameter(new MatRenderParam(RT_4, screenMat, VarType.Texture2D));
        lightList = addParameter(new ValueRenderParam<>(LIGHT_DATA));
        executeState = addParameter(new ValueRenderParam<>(EXECUTE_STATE));
        frameBuffer = addParameter(new FrameBufferParam(FGGlobal.S_DEFAULT_FB, false, true, true));
        bindParameters(frameGraph);
        
    }
    
    protected Material createMaterial() {
        return new Material(assetManager, MATDEF);
    }
    
    protected void bindParameters(MyFrameGraph frameGraph) {
        for (int i = 0; i < matParams.length; i++) {
            frameGraph.bindToOutput(GBufferPass.RENDER_TARGETS[i], matParams[i]);
        }
        frameGraph.bindToOutput(GBufferPass.LIGHT_DATA, lightList);
        frameGraph.bindToOutput(GBufferPass.EXECUTE_STATE, executeState);
        frameGraph.bindToOutput(GBufferPass.G_FRAME_BUFFER, frameBuffer);
    }
    
    @Override
    public void prepare(RenderContext context) {
        super.prepare(context);
        ViewPort vp = forcedViewPort;
        if (vp == null) {
            vp = context.getViewPort();
        }
        frameBuffer.accept(vp.getOutputFrameBuffer());
        executeState.accept(false);
        lightList.erase();
    }
    
    @Override
    public void executeDrawCommands(RenderContext renderContext) {
        screenMat.selectTechnique(S_DEFERRED_PASS, renderContext.getRenderManager());
        boolean depthWrite = screenMat.getAdditionalRenderState().isDepthWrite();
        boolean depthTest = screenMat.getAdditionalRenderState().isDepthTest();
        screenMat.getAdditionalRenderState().setDepthWrite(false);
        screenMat.getAdditionalRenderState().setDepthTest(false);
        screenMat.setBoolean("UseLightsCullMode", false);
        screenRect.updateGeometricState();
        screenMat.render(screenRect, lightList.produce(), renderContext.getRenderManager());
        screenMat.getAdditionalRenderState().setDepthWrite(depthWrite);
        screenMat.getAdditionalRenderState().setDepthTest(depthTest);
    }
    
    @Override
    public void dispatchPassSetup(RenderQueue queue) {
        boolean exState = executeState.validate() && executeState.produce();
        boolean hasLightData = lightList.validate() && lightList.produce().size() > 0;
        canExecute = hasLightData || exState;
    }
    
    @Override
    public boolean drawGeometry(RenderManager rm, Geometry geom) {
        return true;
    }
    
}
