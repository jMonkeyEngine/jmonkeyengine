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
import com.jme3.renderer.framegraph.AbstractModule;
import com.jme3.renderer.framegraph.parameters.MatRenderParam;
import com.jme3.renderer.framegraph.MyFrameGraph;
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
public class DeferredShadingModule extends AbstractModule {
    
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
    protected Material screenMat;
    protected Picture screenRect;
    protected DeferredSinglePassLightingLogic logic;
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
    public void initialize(MyFrameGraph frameGraph) {
        
        screenMat = createMaterial();
        screenRect = new Picture("DeferredShadingPass_Rect");
        screenRect.setWidth(1);
        screenRect.setHeight(1);
        screenRect.setIgnoreTransform(true);
        screenRect.setMaterial(screenMat);
        
        RenderState rs = screenMat.getAdditionalRenderState();
        rs.setDepthWrite(true);
        rs.setDepthTest(true);
        rs.setDepthFunc(RenderState.TestFunction.Greater);
        //rs.setBlendMode(RenderState.BlendMode.Alpha);
        //screenMat.setTransparent(true);
        screenMat.setBoolean("UseLightsCullMode", false);
        
        assignTechniqueLogic(screenMat);
        
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
    @Override
    public void prepare(RenderContext context) {
        if (debug == null) {
            debug = new FrameBuffer(context.getWidth(), context.getHeight(), 1);
            FrameBuffer.FrameBufferTextureTarget t = FrameBuffer.FrameBufferTarget.newTarget(
                    new Texture2D(context.getWidth(), context.getHeight(), GBufferModule.DEPTH_FORMAT));
            debug.setDepthTarget(t);
            depthCopy.setTextureTarget(t);
        }
    }
    @Override
    public void execute(RenderContext context) {
        
        context.getRenderer().copyFrameBuffer(gBuffer.produce(),
                context.getViewPort().getOutputFrameBuffer(), false, true);
        
        context.getRenderer().copyFrameBuffer(gBuffer.produce(), debug, false, true);
        
        //makeRenderStateTests(context, "pre tests");
        selectTechnique(screenMat, context.getRenderManager());
        context.setDepthRange(1, 1);
        context.getRenderer().setFrameBuffer(context.getViewPort().getOutputFrameBuffer());
        screenRect.updateGeometricState();
        context.getRenderManager().renderGeometry(screenRect, lightList.produce());
        //screenMat.render(screenRect, lightList.produce(), context.getRenderManager());
        
        //makeRenderStateTests(context, "post tests");
        
    }
    @Override
    public void reset() {}
    
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
    protected void assignTechniqueLogic(Material material) {
        for (TechniqueDef t : screenMat.getMaterialDef().getTechniqueDefs(PASS)) {
            t.setLogic(new DeferredSinglePassLightingLogic(t, true));
        }
    }
    protected void selectTechnique(Material mat, RenderManager rm) {
        mat.selectTechnique(PASS, rm);
    }
    
    private void makeRenderStateTests(RenderContext context, String label) {
        Renderer r = context.getRenderer();
        RenderManager rm = context.getRenderManager();
        ViewPort vp = context.getViewPort();
        System.out.println(label+":");
        test(r.getCurrentFrameBuffer(), "framebuffer");
        test(context.getDepthRange(), "depth");
        test(rm.getCurrentCamera(), "camera");
        test(rm.getGeometryRenderHandler(), "handler");
        test(rm.getRenderFilter(), "geometry filter");
        test(rm.getPreferredLightMode(), "preferred light mode");
        test(rm.getForcedTechnique(), "forced technique");
        test(rm.getForcedRenderState(), "forced renderstate");
        test(rm.getForcedMatParams().size(), "num forced mat params");
        test(rm.getLightFilter(), "light filter");
        test(vp.getBackgroundColor(), "viewport background");
        test(vp.getOutputFrameBuffer(), "viewport output framebuffer");
        test("color:"+vp.isClearColor()+", depth:"+vp.isClearDepth()+", stencil:"+vp.isClearStencil(), "viewport clear flags");
    }
    private void test(Object object, String label) {
        System.out.println("  "+label+" = "+(object == null ? "null" : object.toString().replaceAll("\n", "; ")));
    }
    
}
