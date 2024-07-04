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
import com.jme3.renderer.framegraph.GeometryQueue;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.queue.GeometryList;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class QueueMergePass extends RenderPass {
    
    private int groupSize = 2;
    private ResourceTicket<GeometryQueue> result;
    private final GeometryQueue target = new GeometryQueue();

    public QueueMergePass() {}
    public QueueMergePass(int groupSize) {
        this.groupSize = groupSize;
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        addInputGroup("Queues", groupSize);
        result = addOutput("Result");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(null, result);
        referenceOptional(getGroupArray("Queues"));
    }
    @Override
    protected void execute(FGRenderContext context) {
        GeometryQueue[] queues = acquireArray("Queues", n -> new GeometryQueue[n]);
        for (GeometryQueue q : queues) {
            target.add(q);
        }
        resources.setPrimitive(result, target);
    }
    @Override
    protected void reset(FGRenderContext context) {
        target.clear();
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(groupSize, "groupSize", 2);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        groupSize = in.readInt("groupSize", 2);
    }
    
    public void setGroupSize(int groupSize) {
        if (isAssigned()) {
            throw new IllegalStateException("Cannot alter group size while assigned to a framegraph.");
        }
        this.groupSize = groupSize;
    }
    
    public int getGroupSize() {
        return groupSize;
    }
    
}
