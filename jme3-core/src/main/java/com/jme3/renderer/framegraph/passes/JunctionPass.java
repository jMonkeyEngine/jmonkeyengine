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
public class JunctionPass <T> extends RenderPass {
    
    private int length;
    private ResourceTicket<T> output;
    private GraphSource<Integer> source;
    
    public JunctionPass() {
        this(2);
    }
    public JunctionPass(int length) {
        this.length = length;
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        for (int i = 0; i < length; i++) {
            addInput("Input"+i);
        }
        output = addOutput("Value");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        output.setSource(getInputTickets().get(source.getGraphValue(context.getViewPort())));
    }
    @Override
    protected void execute(FGRenderContext context) {}
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean isUsed() {
        return true;
    }
    
    public void setLength(int length) {
        this.length = length;
    }
    
}
