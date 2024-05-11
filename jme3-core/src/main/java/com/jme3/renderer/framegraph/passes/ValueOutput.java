/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;

/**
 *
 * @author codex
 * @param <T>
 */
public class ValueOutput <T> extends RenderPass {
    
    private final Class<T> type;
    private String target;
    private ViewPort viewPort;
    private ResourceTicket<T> ticket;

    public ValueOutput(Class<T> type, String target) {
        this.type = type;
        this.target = target;
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        ticket = addInput("Value");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        referenceOptional(ticket);
    }
    @Override
    protected void execute(FGRenderContext context) {
        if (viewPort == null || context.getViewPort() == viewPort) {
            T value = resources.acquireOrElse(ticket, null);
            if (value != null) {
                context.getBlackboard().set(target, type, value);
                resources.setConstant(ticket);
            }
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}

    public void setTarget(String target) {
        this.target = target;
    }
    public void setViewPort(ViewPort viewPort) {
        this.viewPort = viewPort;
    }
    
}
