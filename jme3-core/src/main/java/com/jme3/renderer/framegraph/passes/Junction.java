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
 * @param <T>
 */
public class Junction <T> extends RenderPass {
    
    private int length = 2;
    private int index = 0;
    private ResourceTicket<T> output;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        for (int i = 0; i < length; i++) {
            addInput("Input"+i);
        }
        output = addOutput("Value");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        output.setSource(getInputTickets().get(index));
    }
    @Override
    protected void execute(FGRenderContext context) {}
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean isReferenced() {
        return true;
    }

    public void setIndex(int index) {
        this.index = 0;
    }
    public void setLength(int length) {
        this.length = length;
    }
    
}
