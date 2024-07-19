/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.client.GraphSource;
import com.jme3.renderer.framegraph.client.GraphTarget;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Accepts and produces and group of inputs and outputs to/from game logic.
 * <p>
 * The size of each group is determine by the specified group size. Each given
 * {@link GraphSource} and {@link GraphTarget} map index-to-index to an individual
 * output or input. Surplus sources and targets are not used.
 * <p>
 * Inputs:
 * <ul>
 *   <li>{@link #INPUT}[n] ({@link Object}): input group of a specified size (optional).
 * </ul>
 * Outputs:
 * <ul>
 *   <li>{@link #OUTPUT}[n] ({@link Object)}: output group of a specified size.
 * </ul>
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
        for (ResourceTicket t : getGroupArray(OUTPUT)) {
            declare(null, t);
        }
        referenceOptional(getGroupArray(INPUT));
    }
    @Override
    protected void execute(FGRenderContext context) {
        ViewPort vp = context.getViewPort();
        ResourceTicket[] inTickets = getGroupArray(INPUT);
        for (int i = 0, n = Math.min(groupSize, targets.size()); i < n; i++) {
            Object value = resources.acquireOrElse(inTickets[i], null);
            GraphTarget t = targets.get(i);
            if (t != null && t.setGraphValue(frameGraph, vp, value)) {
                resources.setConstant(inTickets[i]);
            }
        }
        int i = 0;
        ResourceTicket[] outTickets = getGroupArray(OUTPUT);
        for (int n = Math.min(groupSize, sources.size()); i < n; i++) {
            GraphSource s = sources.get(i);
            if (s != null) {
                Object value = s.getGraphValue(frameGraph, vp);
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
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(groupSize, "groupSize", 2);
        out.writeFromCollection(sources, "sources", true);
        out.writeFromCollection(targets, "targets", true);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        groupSize = in.readInt("groupSize", 2);
        in.readToCollection("sources", sources);
        in.readToCollection("targets", targets);
    }
    
    /**
     * Sets the size of the input and output groups.
     * 
     * @param groupSize 
     * @throws IllegalStateException if called while pass is assigned to a framegraph
     */
    public void setGroupSize(int groupSize) {
        if (isAssigned()) {
            throw new IllegalStateException("Cannot alter group size while assigned to a framegraph.");
        }
        this.groupSize = groupSize;
    }
    /**
     * Sets the source that provides values for the output at the index
     * within the output group.
     * 
     * @param i
     * @param source 
     */
    public void setSource(int i, GraphSource source) {
        while (sources.size() <= i) {
            sources.add(null);
        }
        sources.set(i, source);
    }
    /**
     * Sets the target that recieves values from the input at the index
     * within the input group.
     * 
     * @param i
     * @param target 
     */
    public void setTarget(int i, GraphTarget target) {
        while (targets.size() <= i) {
            targets.add(null);
        }
        targets.set(i, target);
    }
    
    /**
     * 
     * @return 
     */
    public int getGroupSize() {
        return groupSize;
    }
    /**
     * Gets the source at the index.
     * 
     * @param i
     * @return source at the index, or null if no source is assigned at the index
     */
    public GraphSource getSource(int i) {
        if (i < sources.size()) {
            return sources.get(i);
        } else {
            return null;
        }
    }
    /**
     * Gets the target at the index.
     * 
     * @param i
     * @return target at the index, or null if no target is assigned at the index.
     */
    public GraphTarget getTarget(int i) {
        if (i < targets.size()) {
            return targets.get(i);
        } else {
            return null;
        }
    }
    
    /**
     * Gets the name of the input at the index.
     * 
     * @param i
     * @return 
     */
    public static String getInput(int i) {
        return INPUT+'['+i+']';
    }
    /**
     * Gets the name of the output at the index.
     * 
     * @param i
     * @return 
     */
    public static String getOutput(int i) {
        return OUTPUT+'['+i+']';
    }
    
}
