/*
 * Copyright (c) 2009-2023 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.pass.RenderGeometry;

/**
 * All passes that need to perform rendering must inherit from this class.<br/>
 * @author JohnKkk
 */
public abstract class FGRenderQueuePass extends FGBindingPass implements RenderGeometry {
    protected ViewPort forceViewPort;
    // It is just geometry data for now. If we extend the RHI interface in the future, it may be adjusted to MeshDrawCommand.
    protected GeometryList passMeshDrawCommandList;
    protected boolean canExecute;
    
    public FGRenderQueuePass(String name) {
        super(name);
    }

    /**
     * A RenderPass may use a specified viewport. This can be set using this function.<br/>
     * @param forceViewPort targetViewPort
     */
    public void setForceViewPort(ViewPort forceViewPort) {
        this.forceViewPort = forceViewPort;
    }

    /**
     * Dispatch visible mesh draw commands to process task, to prepare for this pass.<br/>
     * todo:For the current GLRenderer, the MeshDrawCommand concept actually does not exist. So this is prepared for future Vulkan-like renderers<br/>
     * @param renderQueue targetRenderQueue
     */
    public abstract void dispatchPassSetup(RenderQueue renderQueue);

    @Override
    public void execute(FGRenderContext renderContext) {
        renderContext.getRenderManager().setRenderGeometryHandler(this);
        dispatchPassSetup(renderContext.getRenderQueue());
        if(!canExecute){
            renderContext.getRenderManager().setRenderGeometryHandler(null);
            return;
        }
        bindAll(renderContext);

        // todo:Use the default queue temporarily to avoid creating a temporary copy
        if(passMeshDrawCommandList != null && passMeshDrawCommandList.size() > 0){
            // drawcall
        }
        executeDrawCommandList(renderContext);
        renderContext.getRenderManager().setRenderGeometryHandler(null);
    }

    /**
     * todo:For the current GLRenderer, the MeshDrawCommand concept actually does not exist. So this is prepared for future Vulkan-like renderers<br/>
     * @param renderContext
     */
    public abstract void executeDrawCommandList(FGRenderContext renderContext);

    @Override
    public void resetPass() {
        super.resetPass();
        if(passMeshDrawCommandList != null && passMeshDrawCommandList.size() > 0){
            passMeshDrawCommandList.clear();
        }
    }
    
    
}
