/*
 * Copyright (c) 2024 jMonkeyEngine
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
package com.jme3.renderer.pipeline;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;

/**
 * Pipeline for rendering a ViewPort.
 * 
 * @author codex
 * @param <T>
 */
public interface RenderPipeline <T extends PipelineContext> {
    
    /**
     * Fetches the PipelineContext this pipeline requires for rendering
     * from the RenderManager.
     * 
     * @param rm
     * @return pipeline context (not null)
     */
    public T fetchPipelineContext(RenderManager rm);
    
    /**
     * Returns true if this pipeline has rendered a viewport this render frame.
     * 
     * @return 
     */
    public boolean hasRenderedThisFrame();
    
    /**
     * Called before this pipeline is rendered for the first time this frame.
     * <p>
     * Only called if the pipeline will actually be rendered.
     * 
     * @param rm 
     */
    public void startRenderFrame(RenderManager rm);
    
    /**
     * Renders the pipeline.
     * 
     * @param rm
     * @param context
     * @param vp
     * @param tpf 
     */
    public void pipelineRender(RenderManager rm, T context, ViewPort vp, float tpf);
    
    /**
     * Called after all rendering is complete in a rendering frame this
     * pipeline participated in.
     * 
     * @param rm 
     */
    public void endRenderFrame(RenderManager rm);
    
}
