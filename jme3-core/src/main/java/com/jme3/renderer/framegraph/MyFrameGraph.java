/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class MyFrameGraph {
    
    private final LinkedList<FGModule> passes = new LinkedList<>();
    private final ParameterManager parameters = new ParameterManager();
    private final RenderContext context;
    
    public MyFrameGraph(RenderManager renderManager) {
        context = new RenderContext(renderManager);
    }
    
    public void execute(ViewPort vp) {
        context.setViewPort(vp);
        // prepare passes for execution
        for (FGModule p : passes) {
            p.prepare(context);
        }
        // execute
        for (FGModule p : passes) {
            // accept parameters as arguments to the pass
            parameters.pull(p);
            // execute pass
            p.execute(context);
            // apply resulting output parameters to connected input parameters
            parameters.push(p);
        }
        // reset passes
        for (FGModule p : passes) {
            p.reset();
        }
    }
    
    public void add(FGModule pass) {
        pass.initialize(this);
        passes.add(pass);
        registerParameterGroup(pass);
    }
    
    public ParameterManager getParameters() {
        return parameters;
    }
    
    public RenderContext getContext() {
        return context;
    }
    
    public <T extends RenderParameter> T registerParameter(T param) {
        parameters.register(param);
        return param;
    }
    
    public void registerParameterGroup(RenderParameterGroup group) {
        parameters.register(group);
    }
    
    public void bindToOutput(String target, RenderParameter input) {
        parameters.bindToOutput(target, input);
    }
    
    public void bindToInput(String target, RenderParameter output) {
        parameters.bindToInput(target, output);
    }
    
    public void removeParameter(RenderParameter param) {
        parameters.remove(param);
    }
    
    public void removeParameterGroup(RenderParameterGroup group) {
        parameters.remove(group);
    }
    
}
