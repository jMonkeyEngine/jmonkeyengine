/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.framegraph.io.GraphSource;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import java.io.IOException;

/**
 * Merges several inputs into one output by choosing one input to connect to
 * the output using a controllable index.
 * <p>
 * This pass does no rendering and cannot be culled.
 * 
 * @author codex
 * @param <T>
 */
public class Junction <T> extends RenderPass {
    
    private int length;
    private ResourceTicket<T> output;
    private GraphSource<Integer> source;
    
    public Junction() {
        this(2);
    }
    public Junction(int length) {
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
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(length, "length", 2);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        length = in.readInt("length", 2);
    }
    
    public void setLength(int length) {
        this.length = length;
    }
    
}
