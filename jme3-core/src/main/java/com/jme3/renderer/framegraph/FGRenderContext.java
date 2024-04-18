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

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;

/**
 * In order to be compatible with existing logic, FGRenderContext is currently just a local proxy, and may gradually replace the existing state machine manager in the future.
 * @author JohnKkk
 */
public class FGRenderContext {
    
    protected static class FGPipelineObjectState {
        
        public float startDepth;
        public float endDepth;

        public FGPipelineObjectState(float startDepth, float endDepth) {
            this.startDepth = startDepth;
            this.endDepth = endDepth;
        }
        
    }

    private final RenderManager renderManager;
    private ViewPort viewPort;
    protected FGPipelineObjectState currentPSO;

    public FGRenderContext(RenderManager renderManager, ViewPort viewPort) {
        this.renderManager = renderManager;
        this.viewPort = viewPort;
        currentPSO = new FGPipelineObjectState(0, 1);
    }
    
    public void setDepthRange(float start, float end){
        if(currentPSO.startDepth != start || currentPSO.endDepth != end){
            renderManager.getRenderer().setDepthRange(start, end);
            currentPSO.startDepth = start;
            currentPSO.endDepth = end;
        }
    }
    
    public void setViewPort(ViewPort viewPort) {
        this.viewPort = viewPort;
    } 
    
    public RenderManager getRenderManager() {
        return renderManager;
    }
    
    public ViewPort getViewPort() {
        return viewPort;
    }
    
    public Renderer getRenderer() {
        return renderManager.getRenderer();
    }
    
    public RenderQueue getRenderQueue() {
        if (viewPort != null) {
            return viewPort.getQueue();
        } else {
            return null;
        }
    }
    
}
