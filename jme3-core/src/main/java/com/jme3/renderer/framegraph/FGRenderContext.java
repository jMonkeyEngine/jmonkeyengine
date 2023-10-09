package com.jme3.renderer.framegraph;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;

/**
 * @author JohnKkk
 */
public class FGRenderContext {
    // PSO
    public static class FGPipelineObjectState{
        public float startDepth;
        public float endDepth;
    }

    public RenderManager renderManager;
    public RenderQueue renderQueue;
    public ViewPort viewPort;
    protected FGPipelineObjectState currentPSO;

    public FGRenderContext(RenderManager renderManager, RenderQueue renderQueue, ViewPort viewPort) {
        this.renderManager = renderManager;
        this.renderQueue = renderQueue;
        this.viewPort = viewPort;
        currentPSO = new FGPipelineObjectState();
        currentPSO.startDepth = 0;
        currentPSO.endDepth = 1;
    }
    public final void setDepthRange(float start, float end){
        if(currentPSO.startDepth != start || currentPSO.endDepth != end){
            renderManager.getRenderer().setDepthRange(start, end);
            currentPSO.startDepth = start;
            currentPSO.endDepth = end;
        }
    }
}
