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

import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;
import com.jme3.profile.VpStep;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.util.SafeArrayList;
import java.util.List;

/**
 * Port of the standard forward renderer to a pipeline.
 * 
 * @author codex
 */
public class ForwardPipeline implements RenderPipeline {

    private boolean rendered = false;
    
    @Override
    public PipelineContext fetchPipelineContext(RenderManager rm) {
        return rm.getDefaultContext();
    }
    @Override
    public boolean hasRenderedThisFrame() {
        return rendered;
    }
    @Override
    public void startRenderFrame(RenderManager rm) {}
    @Override
    public void pipelineRender(RenderManager rm, PipelineContext context, ViewPort vp, float tpf) {
        
        AppProfiler prof = rm.getProfiler();
        
        SafeArrayList<SceneProcessor> processors = vp.getProcessors();
        if (processors.isEmpty()) {
            processors = null;
        }
        
        if (processors != null) {
            if (prof != null) {
                prof.vpStep(VpStep.PreFrame, vp, null);
            }
            for (SceneProcessor p : processors.getArray()) {
                if (!p.isInitialized()) {
                    p.initialize(rm, vp);
                }
                p.setProfiler(prof);
                if (prof != null) {
                    prof.spStep(SpStep.ProcPreFrame, p.getClass().getSimpleName());
                }
                p.preFrame(tpf);
            }
        }
        
        rm.applyViewPort(vp);
        
        if (prof != null) {
            prof.vpStep(VpStep.RenderScene, vp, null);
        }
        // flatten scenes into render queue
        List<Spatial> scenes = vp.getScenes();
        for (int i = scenes.size() - 1; i >= 0; i--) {
            rm.renderScene(scenes.get(i), vp);
        }
        if (processors != null) {
            if (prof != null) {
                prof.vpStep(VpStep.PostQueue, vp, null);
            }
            for (SceneProcessor p : processors.getArray()) {
                if (prof != null) {
                    prof.spStep(SpStep.ProcPostQueue, p.getClass().getSimpleName());
                }
                p.postQueue(vp.getQueue());
            }
        }

        if (prof != null) {
            prof.vpStep(VpStep.FlushQueue, vp, null);
        }
        rm.flushQueue(vp);

        if (processors != null) {
            if (prof != null) {
                prof.vpStep(VpStep.PostFrame, vp, null);
            }
            for (SceneProcessor proc : processors.getArray()) {
                if (prof != null) {
                    prof.spStep(SpStep.ProcPostFrame, proc.getClass().getSimpleName());
                }
                proc.postFrame(vp.getOutputFrameBuffer());
            }
            if (prof != null) {
                prof.vpStep(VpStep.ProcEndRender, vp, null);
            }
        }

        // render the translucent objects queue after processors have been rendered
        rm.renderTranslucentQueue(vp);

        // clear any remaining spatials that were not rendered.
        rm.clearQueue(vp);
        
        rendered = true;

        /*
         * the call to setCamera will indirectly cause a clipRect to be set, must be cleared to avoid surprising results
         * if renderer#copyFrameBuffer is used later
         */
        rm.getRenderer().clearClipRect();

        if (prof != null) {
            prof.vpStep(VpStep.EndRender, vp, null);
        }
        
    }
    @Override
    public void endRenderFrame(RenderManager rm) {
        rendered = false;
    }
    
}
