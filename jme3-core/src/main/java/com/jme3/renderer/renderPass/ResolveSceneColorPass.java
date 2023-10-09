package com.jme3.renderer.renderPass;

import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.shader.VarType;
import com.jme3.ui.Picture;

/**
 * @author JohnKkk
 */
public class ResolveSceneColorPass extends ScreenPass {
    public final static String S_SCENE_COLOR_RT = "SceneColorRT";
    public final static String S_SCENE_DEPTH = "SceneDepth";
    private static final String _S_RESOLVE_SCENE_COLOR_MAT_DEF = "Common/MatDefs/Misc/ResolveSceneColor.j3md";

    public ResolveSceneColorPass(String name) {
        super(name, RenderQueue.Bucket.Opaque);
    }

    @Override
    public void dispatchPassSetup(RenderQueue renderQueue) {
        canExecute = true;
    }

    @Override
    public void executeDrawCommandList(FGRenderContext renderContext) {
        renderContext.renderManager.getRenderer().setFrameBuffer(null);
        boolean depthWrite = screenMat.getAdditionalRenderState().isDepthWrite();
        boolean depthTest = screenMat.getAdditionalRenderState().isDepthTest();
        screenMat.getAdditionalRenderState().setDepthWrite(false);
        screenMat.getAdditionalRenderState().setDepthTest(false);
        screenRect.updateGeometricState();
        screenMat.render(screenRect, renderContext.renderManager);
        screenMat.getAdditionalRenderState().setDepthWrite(depthWrite);
        screenMat.getAdditionalRenderState().setDepthTest(depthTest);
    }
    public void updateExposure(float exposure){
        screenMat.setFloat("Exposure", exposure);
    }

    @Override
    public boolean drawGeometry(RenderManager rm, Geometry geom) {
        return true;
    }

    @Override
    public void init() {
        MaterialDef def = (MaterialDef) assetManager.loadAsset(_S_RESOLVE_SCENE_COLOR_MAT_DEF);
        screenMat = new Material(def);
        screenRect = new Picture(getName() + "_rect");
        screenRect.setWidth(1);
        screenRect.setHeight(1);
        screenRect.setMaterial(screenMat);

        // register Sinks
        registerSink(new FGTextureBindableSink<FGRenderTargetSource.RenderTargetSourceProxy>(S_SCENE_COLOR_RT, binds, binds.size(), screenMat, VarType.Texture2D));
        registerSink(new FGFramebufferCopyBindableSink<FGFramebufferSource.FrameBufferSourceProxy>(FGGlobal.S_DEFAULT_FB, null, false, true, true, binds, binds.size()));
    }
}
