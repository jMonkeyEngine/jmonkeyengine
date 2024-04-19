/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.pass;

import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;
import com.jme3.profile.VpStep;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.AbstractModule;
import com.jme3.renderer.framegraph.DepthRange;
import com.jme3.renderer.framegraph.MyFrameGraph;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.util.SafeArrayList;

/**
 *
 * @author codex
 */
public class PostProcessingModule extends AbstractModule {
    
    private AppProfiler profiler;
    
    public PostProcessingModule() {
        this(null);
    }
    public PostProcessingModule(AppProfiler profiler) {
        this.profiler = profiler;
    }
    
    @Override
    public void initialize(MyFrameGraph frameGraph) {}
    @Override
    public void prepare(RenderContext context) {}
    @Override
    public void execute(RenderContext context) {
        context.setDepthRange(DepthRange.IDENTITY);
        ViewPort vp = context.getViewPort();
        SafeArrayList<SceneProcessor> processors = vp.getProcessors();
        if (processors != null) {
            if (profiler != null) {
                profiler.vpStep(VpStep.PostFrame, vp, null);
            }
            for (SceneProcessor p : processors.getArray()) {
                if (profiler != null) {
                    profiler.spStep(SpStep.ProcPostFrame, p.getClass().getSimpleName());
                }
                p.postFrame(vp.getOutputFrameBuffer());
            }
            if (profiler != null) {
                profiler.vpStep(VpStep.ProcEndRender, vp, null);
            }
        }
    }
    @Override
    public void reset() {}
    
    public void setProfiler(AppProfiler profiler) {
        this.profiler = profiler;
    }
    
    public AppProfiler getProfiler() {
        return profiler;
    }
    
}
