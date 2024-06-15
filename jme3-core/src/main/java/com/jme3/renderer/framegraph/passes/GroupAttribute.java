/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.client.GraphSource;
import com.jme3.renderer.framegraph.client.GraphTarget;
import java.util.ArrayList;

/**
 *
 * @author codex
 */
public class GroupAttribute extends RenderPass {
    
    public static final String INPUT = "Input", OUTPUT = "Output";
    
    private int groupSize = 2;
    private final ArrayList<GraphSource> sources = new ArrayList<>(5);
    private final ArrayList<GraphTarget> targets = new ArrayList<>(5);
    
    public GroupAttribute() {}
    public GroupAttribute(int groupSize) {
        this.groupSize = groupSize;
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        addInputGroup(INPUT, groupSize);
        addOutputGroup(OUTPUT, groupSize);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        for (ResourceTicket t : getGroup(OUTPUT)) {
            declare(null, t);
        }
        referenceOptional(getGroup(INPUT));
    }
    @Override
    protected void execute(FGRenderContext context) {
        ViewPort vp = context.getViewPort();
        ResourceTicket[] inTickets = getGroup(INPUT);
        for (int i = 0, n = Math.min(groupSize, targets.size()); i < n; i++) {
            Object value = resources.acquireOrElse(inTickets[i], null);
            GraphTarget t = targets.get(i);
            if (t != null && t.setGraphValue(vp, value)) {
                resources.setConstant(inTickets[i]);
            }
        }
        int i = 0;
        ResourceTicket[] outTickets = getGroup(OUTPUT);
        for (int n = Math.min(groupSize, sources.size()); i < n; i++) {
            GraphSource s = sources.get(i);
            if (s != null) {
                Object value = s.getGraphValue(vp);
                if (value != null) {
                    resources.setPrimitive(outTickets[i], value);
                    continue;
                }
            }
            resources.setUndefined(outTickets[i]);
        }
        for (; i < groupSize; i++) {
            resources.setUndefined(outTickets[i]);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
       
    public void setGroupSize(int groupSize) {
        if (isAssigned()) {
            throw new IllegalStateException("Cannot alter group size while assigned to a framegraph.");
        }
        this.groupSize = groupSize;
    }
    public void setSource(int i, GraphSource source) {
        while (sources.size() <= i) {
            sources.add(null);
        }
        sources.set(i, source);
    }
    public void setTarget(int i, GraphTarget target) {
        while (targets.size() <= i) {
            targets.add(null);
        }
        targets.set(i, target);
    }
    
    public int getGroupSize() {
        return groupSize;
    }
    public GraphSource getSource(int i) {
        if (i < sources.size()) {
            return sources.get(i);
        } else {
            return null;
        }
    }
    public GraphTarget getTarget(int i) {
        if (i < targets.size()) {
            return targets.get(i);
        } else {
            return null;
        }
    }
    
    public static String getInput(int i) {
        return INPUT+'['+i+']';
    }
    public static String getOutput(int i) {
        return OUTPUT+'['+i+']';
    }
    
}
