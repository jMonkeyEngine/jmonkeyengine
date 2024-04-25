/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.parameters.RenderParameterGroup;
import com.jme3.renderer.framegraph.parameters.ParameterBinding;
import com.jme3.renderer.framegraph.parameters.ParameterSpace;
import com.jme3.renderer.framegraph.parameters.RenderParameter;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.util.LinkedList;

/**
 * Manages render passes, dependencies, and resources in a node-based parameter system.
 * 
 * @author codex
 */
public class MyFrameGraph {
    
    private final LinkedList<FGModule> passes = new LinkedList<>();
    private final ParameterSpace parameters = new ParameterSpace();
    private final RenderContext context;
    
    public MyFrameGraph(RenderManager renderManager) {
        context = new RenderContext(renderManager);
    }
    
    /**
     * Prepares the context for rendering with the viewport.
     * 
     * @param vp viewport being rendered
     * @param tpf time per frame
     */
    public void prepareRender(ViewPort vp, float tpf) {
        prepareRender(vp, null, tpf);
    }
    
    /**
     * Prepares the context for rendering with the viewport.
     * 
     * @param vp viewport being rendered
     * @param prof app profiler, or null for no profiling
     * @param tpf time per frame
     */
    public void prepareRender(ViewPort vp, AppProfiler prof, float tpf) {
        context.update(vp, prof, tpf);
    }
    
    /**
     * 
     */
    public void preFrame() {
        for (FGModule p : passes) {
            p.preFrame(context);
        }
    }
    
    /**
     * 
     */
    public void postQueue() {
        for (FGModule p : passes) {
            p.postQueue(context);
        }
    }
    
    /**
     * Executes this framegraph for rendering.
     */
    public void execute() {
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
            // reset depth render range
            context.setDepthRange(DepthRange.IDENTITY);
            // reset geometry handler
            context.getRenderManager().setGeometryRenderHandler(null);
        }
        // reset passes
        for (FGModule p : passes) {
            p.reset();
        }
    }
    
    /**
     * Adds the pass to this framegraph and registers it with the
     * local parameter space.
     * 
     * @param pass 
     */
    public void add(FGModule pass) {
        pass.initialize(this);
        passes.add(pass);
        registerParameterGroup(pass);
    }
    
    /**
     * Gets the first pass that is of or a subclass of the given type.
     * 
     * @param <T>
     * @param type
     * @return pass of the given type, or null if none exists
     */
    public <T extends FGModule> T get(Class<T> type) {
        for (FGModule p : passes) {
            if (type.isAssignableFrom(p.getClass())) {
                return (T)p;
            }
        }
        return null;
    }
    
    /**
     * Gets the local parameter space.
     * 
     * @return 
     */
    public ParameterSpace getLocalParameters() {
        return parameters;
    }
    
    /**
     * Gets the world parameter space belonging to the render manager.
     * 
     * @return 
     */
    public ParameterSpace getWorldParameters() {
        return context.getRenderManager().getParameters();
    }
    
    /**
     * Gets the render context.
     * 
     * @return 
     */
    public RenderContext getContext() {
        return context;
    }
    
    /**
     * Registers the parameter with the local parameter space.
     * 
     * @param <T>
     * @param param
     * @return the given parameter
     */
    public <T extends RenderParameter> T registerParameter(T param) {
        parameters.register(param);
        return param;
    }
    
    /**
     * Registers the parameter group with the local parameter space.
     * 
     * @param group 
     */
    public void registerParameterGroup(RenderParameterGroup group) {
        parameters.register(group);
    }
    
    /**
     * Binds the given input parameter to the named target output parameter in
     * the local parameter space.
     * 
     * @param target
     * @param input
     * @return 
     */
    public ParameterBinding bindToOutput(String target, RenderParameter input) {
        return parameters.bindToOutput(target, input);
    }
    
    /**
     * Binds the given output parameter to the named target input parameter in
     * the local parameter space.
     * 
     * @param target
     * @param output
     * @return 
     */
    public ParameterBinding bindToInput(String target, RenderParameter output) {
        return parameters.bindToInput(target, output);
    }
    
    /**
     * Removes the parameter from the local parameter space.
     * 
     * @param param 
     */
    public void removeParameter(RenderParameter param) {
        parameters.remove(param);
    }
    
    /**
     * Removes the parameter group from the local parameter space.
     * 
     * @param group 
     */
    public void removeParameterGroup(RenderParameterGroup group) {
        parameters.remove(group);
    }
    
}
