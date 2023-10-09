package com.jme3.renderer.renderPass;

import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FGRenderQueuePass;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;

/**
 * @author JohnKkk
 */
public class ForwardPass extends FGRenderQueuePass {
    private RenderQueue.Bucket bucket;
    public ForwardPass(String name, RenderQueue.Bucket bucket) {
        super(name);
        this.bucket = bucket;
    }

    @Override
    public void executeDrawCommandList(FGRenderContext renderContext) {
        if(!canExecute)return;
        Camera cam = null;
        if(forceViewPort != null){
            cam = forceViewPort.getCamera();
        }
        else{
            cam = renderContext.viewPort.getCamera();
        }
        RenderManager rm = renderContext.renderManager;
        renderContext.renderQueue.renderQueue(this.bucket, rm, cam, true);
    }

    @Override
    public void dispatchPassSetup(RenderQueue renderQueue) {
        canExecute = !renderQueue.isQueueEmpty(this.bucket);
    }

    @Override
    public boolean drawGeometry(RenderManager rm, Geometry geom) {
        rm.renderGeometry(geom);
        return true;
    }
}
