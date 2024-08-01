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
import java.util.LinkedList;

/**
 * Abstract implementation of PipelineContext that manages
 * registered client pipelines automatically.
 * 
 * @author codex
 */
public abstract class AbstractPipelineContext implements PipelineContext {
    
    private final LinkedList<RenderPipeline> usedPipelines = new LinkedList<>();
    
    @Override
    public boolean registerClientPipeline(RenderManager rm, RenderPipeline pipeline) {
        boolean firstCall = false;
        if (!pipeline.hasRenderedThisFrame()) {
            if (firstCall = usedPipelines.isEmpty()) {
                startRenderFrame(rm);
            }
            pipeline.startRenderFrame(rm);
            usedPipelines.add(pipeline);
        } else if (usedPipelines.isEmpty()) {
            throw new IllegalStateException("Pipeline cannot have rendered at this point, but claims it did.");
        }
        return firstCall;
    }
    @Override
    public void endContextRenderFrame(RenderManager rm) {
        for (RenderPipeline p : usedPipelines) {
            p.endRenderFrame(rm);
        }
        usedPipelines.clear();
        endRenderFrame(rm);
    }
    
    /**
     * Called when this context is first fetched for rendering
     * during the current render frame.
     * 
     * @param rm 
     */
    protected abstract void startRenderFrame(RenderManager rm);
    
    /**
     * Called when a render frame that this context participated
     * in ends.
     * 
     * @param rm 
     */
    protected abstract void endRenderFrame(RenderManager rm);
    
}
