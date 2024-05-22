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
        setLength(length);
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
        if (source != null) {
            output.setSource(getInputTickets().get(source.getGraphValue(context.getViewPort())));
        } else {
            output.setSource(getInputTickets().getFirst());
        }
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
    
    public final void setLength(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than zero.");
        }
        this.length = length;
    }
    public void setIndexSource(GraphSource<Integer> source) {
        this.source = source;
    }
    
    public int getLength() {
        return length;
    }
    public GraphSource<Integer> getIndexSource() {
        return source;
    }
    
}
