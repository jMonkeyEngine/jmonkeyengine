package com.jme3.renderer.renderPass;

import com.jme3.renderer.Camera;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.queue.RenderQueue;

/**
 * @author JohnKkk
 */
public class GuiPass extends ForwardPass{
    @Override
    public void executeDrawCommandList(FGRenderContext renderContext) {
        Camera cam = null;
        if(forceViewPort != null){
            cam = forceViewPort.getCamera();
        }
        else{
            cam = renderContext.viewPort.getCamera();
        }
        if(canExecute){
            renderContext.setDepthRange(0, 0);
            renderContext.renderManager.setCamera(cam, true);
        }
        super.executeDrawCommandList(renderContext);
        if(canExecute){
            renderContext.renderManager.setCamera(cam, false);
        }
    }

    public GuiPass() {
        super("GUIPass", RenderQueue.Bucket.Gui);
    }
}
