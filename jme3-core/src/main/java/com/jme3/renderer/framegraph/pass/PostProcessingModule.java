/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.pass;

import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;
import com.jme3.profile.VpStep;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.DepthRange;
import com.jme3.renderer.framegraph.MyFrameGraph;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.util.SafeArrayList;

/**
 *
 * @author codex
 */
public class PostProcessingModule extends AbstractModule {
    
    @Override
    public void initialize(MyFrameGraph frameGraph) {}
    @Override
    public void preFrame(RenderContext context) {
        ViewPort vp = context.getViewPort();
        SafeArrayList<SceneProcessor> processors = vp.getProcessors();
        AppProfiler prof = context.getProfiler();
        if (processors != null && !processors.isEmpty()) {
            if (prof != null) {
                prof.vpStep(VpStep.PreFrame, vp, null);
            }
            for (SceneProcessor p : processors.getArray()) {
                if (!p.isInitialized()) {
                    p.initialize(context.getRenderManager(), vp);
                }
                p.setProfiler(context.getProfiler());
                if (prof != null) {
                    prof.spStep(SpStep.ProcPreFrame, p.getClass().getSimpleName());
                }
                p.preFrame(context.getTpf());
            }
        }
    }
    @Override
    public void postQueue(RenderContext context) {
        ViewPort vp = context.getViewPort();
        SafeArrayList<SceneProcessor> processors = vp.getProcessors();
        AppProfiler prof = context.getProfiler();
        if (processors != null && !processors.isEmpty()) {
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
    }
    @Override
    public boolean prepare(RenderContext context) {
        return true;
    }
    @Override
    public void execute(RenderContext context) {
        context.setDepthRange(DepthRange.IDENTITY);
        ViewPort vp = context.getViewPort();
        SafeArrayList<SceneProcessor> processors = vp.getProcessors();
        AppProfiler prof = context.getProfiler();
        if (processors != null && !processors.isEmpty()) {
            if (prof != null) {
                prof.vpStep(VpStep.PostFrame, vp, null);
            }
            for (SceneProcessor p : processors.getArray()) {
                if (prof != null) {
                    prof.spStep(SpStep.ProcPostFrame, p.getClass().getSimpleName());
                }
                p.postFrame(vp.getOutputFrameBuffer());
            }
            if (prof != null) {
                prof.vpStep(VpStep.ProcEndRender, vp, null);
            }
        }
    }
    @Override
    public void reset() {}
    
}
