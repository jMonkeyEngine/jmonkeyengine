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

import com.jme3.renderer.framegraph.io.GraphTarget;
import com.jme3.renderer.framegraph.io.GraphSource;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.ValueDef;
import java.util.LinkedList;
import java.util.function.Function;

/**
 * Interface pass between the framegraph and game logic, allowing them to communicate.
 * <p>
 * Game logic can listen to framegraph parameters via {@link GraphTarget}s, and/or game logic
 * can communicate parameters to the framegraph via a {@link GraphSource}.
 * <p>
 * Objects handled by this pass are automatically marked as constant, so that future changes
 * do not taint the game logic's resource view.
 * 
 * @author codex
 * @param <T>
 */
public class Attribute <T> extends RenderPass implements Function<Object, T> {
    
    private ResourceTicket<T> in, out;
    private T value;
    private ValueDef<T> def;
    private LinkedList<GraphTarget<T>> targets;
    private GraphSource<T> source;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        in = addInput("Value");
        out = addOutput("Value");
        def = new ValueDef<>(null, this);
        def.setDisposeOnRelease(true);
        def.setUseExisting(false);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(def, out);
        referenceOptional(in);
    }
    @Override
    protected void execute(FGRenderContext context) {
        value = resources.acquireOrElse(in, null);
        if (value != null && !targets.isEmpty()) {
            for (GraphTarget<T> t : targets) {
                t.setGraphValue(context.getViewPort(), value);
            }
            resources.setConstant(in);
        }
        if (source != null) {
            value = source.getGraphValue(context.getViewPort());
        }
        if (value != null) {
            resources.acquire(out);
            resources.setConstant(out);
        } else {
            resources.setUndefined(out);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {
        targets.clear();
        source = null;
    }
    @Override
    public boolean isUsed() {
        return super.isUsed() || in.hasSource();
    }
    @Override
    public T apply(Object t) {
        return value;
    }
    
    /**
     * Adds the graph target.
     * <p>
     * If any targets are recieving from this Attribute, the incoming
     * object will be marked as constant.
     * 
     * @param target target to add (not null)
     */
    public void addTarget(GraphTarget<T> target) {
        targets.add(target);
    }
    /**
     * Removes the graph target.
     * 
     * @param target target to remove (not null)
     */
    public void removeTarget(GraphTarget<T> target) {
        targets.remove(target);
    }
    /**
     * Sets the graph source.
     * 
     * @param source 
     */
    public void setSource(GraphSource<T> source) {
        this.source = source;
    }
    
}
