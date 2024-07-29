/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer;

import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;
import com.jme3.profile.VpStep;
import com.jme3.scene.Spatial;
import com.jme3.util.SafeArrayList;
import java.util.List;

/**
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
    public void beginRenderFrame(RenderManager rm) {}
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
