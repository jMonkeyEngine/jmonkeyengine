/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.renderPass.IRenderGeometry;

/**
 * All passes that need to perform rendering must inherit from this class.<br/>
 * @author JohnKkk
 */
public abstract class FGRenderQueuePass extends FGBindingPass implements IRenderGeometry {
    protected ViewPort forceViewPort;
    // It is just geometry data for now. If we extend the RHI interface in the future, it may be adjusted to MeshDrawCommand.
    protected GeometryList passMeshDrawCommandList;
    protected boolean canExecute;
    
    public FGRenderQueuePass(String name) {
        super(name);
    }

    public void setForceViewPort(ViewPort forceViewPort) {
        this.forceViewPort = forceViewPort;
    }

    /**
     * Dispatch visible mesh draw commands to process task, to prepare for this pass.<br/>
     * @param renderQueue
     */
    public abstract void dispatchPassSetup(RenderQueue renderQueue);

    @Override
    public void execute(FGRenderContext renderContext) {
        renderContext.renderManager.setRenderGeometryHandler(this);
        dispatchPassSetup(renderContext.renderQueue);
        if(!canExecute){
            renderContext.renderManager.setRenderGeometryHandler(null);
            return;
        }
        bindAll(renderContext);

        // todo:Use the default queue temporarily to avoid creating a temporary copy
        if(passMeshDrawCommandList != null && passMeshDrawCommandList.size() > 0){
            // drawcall
        }
        executeDrawCommandList(renderContext);
        renderContext.renderManager.setRenderGeometryHandler(null);
    }
    public abstract void executeDrawCommandList(FGRenderContext renderContext);

    @Override
    public void reset() {
        super.reset();
        if(passMeshDrawCommandList != null && passMeshDrawCommandList.size() > 0){
            passMeshDrawCommandList.clear();
        }
    }
    
    
}
