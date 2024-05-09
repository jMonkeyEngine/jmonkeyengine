/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;

/**
 *
 * @author codex
 */
public class Junction extends RenderPass {
    
    private ResourceTicket ticket;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        ticket = addInput(addOutput("Junction"));
    }
    @Override
    protected void prepare(FGRenderContext context) {
        referenceOptional(ticket);
    }
    @Override
    protected void execute(FGRenderContext context) {}
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    public boolean setIndex(int index) {
        return ticket.setSourceIndex(index);
    }
    public int getNumOptions() {
        return ticket.getSources().size();
    }
    
}
