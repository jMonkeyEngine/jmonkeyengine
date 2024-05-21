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
package com.jme3.renderer.framegraph.passes;

import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;
import com.jme3.profile.VpStep;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.util.SafeArrayList;

/**
 * Runs viewport post-processors.
 * <p>
 * FilterPostProcessor is rendered to the viewport's output framebuffer.
 * 
 * @author codex
 */
public class PostProcessingPass extends RenderPass {

    @Override
    protected void initialize(FrameGraph frameGraph) {}
    @Override
    protected void prepare(FGRenderContext context) {}
    @Override
    protected void execute(FGRenderContext context) {
        SafeArrayList<SceneProcessor> processors = context.getViewPort().getProcessors();
        if (!processors.isEmpty()) {
            context.popFrameBuffer();
            AppProfiler prof = context.getProfiler();
            if (prof != null) {
                prof.vpStep(VpStep.PostFrame, context.getViewPort(), null);
            }
            for (SceneProcessor proc : processors.getArray()) {
                if (prof != null) {
                    prof.spStep(SpStep.ProcPostFrame, proc.getClass().getSimpleName());
                }
                proc.postFrame(context.getViewPort().getOutputFrameBuffer());
            }
            if (prof != null) {
                prof.vpStep(VpStep.ProcEndRender, context.getViewPort(), null);
            }
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean isUsed() {
        return true;
    }
    @Override
    public void preFrame(FGRenderContext context) {
        SafeArrayList<SceneProcessor> processors = context.getViewPort().getProcessors();
        if (!processors.isEmpty()) {
            AppProfiler prof = context.getProfiler();
            for (SceneProcessor p : processors.getArray()) {
                if (!p.isInitialized()) {
                    p.initialize(context.getRenderManager(), context.getViewPort());
                }
                p.setProfiler(prof);
                if (prof != null) {
                    prof.spStep(SpStep.ProcPreFrame, p.getClass().getSimpleName());
                }
                p.preFrame(context.getTpf());
            }
        }
    }
    @Override
    public void postQueue(FGRenderContext context) {
        SafeArrayList<SceneProcessor> processors = context.getViewPort().getProcessors();
        if (!processors.isEmpty()) {
            AppProfiler prof = context.getProfiler();
            for (SceneProcessor p : processors.getArray()) {
                if (prof != null) {
                    prof.spStep(SpStep.ProcPostQueue, p.getClass().getSimpleName());
                }
                p.postQueue(context.getViewPort().getQueue());
            }
        }
    }
    
}
