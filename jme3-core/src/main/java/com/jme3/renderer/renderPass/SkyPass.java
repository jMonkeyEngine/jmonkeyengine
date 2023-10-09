package com.jme3.renderer.renderPass;

import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.queue.RenderQueue;

/**
 * @author JohnKkk
 */
public class SkyPass extends ForwardPass{
    public SkyPass() {
        super("Sky", RenderQueue.Bucket.Sky);
    }

    @Override
    public void executeDrawCommandList(FGRenderContext renderContext) {
        if(canExecute){
            renderContext.setDepthRange(1, 1);
        }
        super.executeDrawCommandList(renderContext);
    }
}
