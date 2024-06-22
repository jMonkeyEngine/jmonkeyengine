/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.ValueDef;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.NullComparator;
import java.io.IOException;

/**
 * Joins a number of geometry lists into one.
 * 
 * @author codex
 */
public class QueueJoinPass extends RenderPass {
    
    private int groupSize = 2;
    private ResourceTicket<GeometryList> result;
    private ValueDef<GeometryList> listDef;
    
    public QueueJoinPass() {}
    public QueueJoinPass(int groupSize) {
        this.groupSize = groupSize;
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        addInputGroup("Input", groupSize);
        result = addOutput("Result");
        listDef = new ValueDef<>(GeometryList.class, obj -> new GeometryList(new NullComparator()));
        listDef.setReviser(list -> list.clear());
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(listDef, result);
        reference(getGroupArray("Input"));
    }
    @Override
    protected void execute(FGRenderContext context) {
        GeometryList target = resources.acquire(result);
        GeometryList[] sources = acquireArray("Input", GeometryList[]::new);
        for (GeometryList s : sources) {
            target.addList(s);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        if (isAssigned()) {
            out.write(getGroupArray("Input").length, "groupSize", 2);
        } else {
            out.write(groupSize, "groupSize", 2);
        }
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        groupSize = in.readInt("groupSize", 2);
    }
    
    public void setGroupSize(int groupSize) {
        if (isAssigned()) {
            throw new IllegalStateException("Cannot resize group while assigned to a framegraph.");
        }
        this.groupSize = groupSize;
    }
    
}
