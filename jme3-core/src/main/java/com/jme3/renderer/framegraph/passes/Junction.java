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
import com.jme3.export.NullSavable;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.framegraph.client.GraphSource;
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
    
    private static final int EXTRA_INPUTS = 0;
    
    private int length;
    private int groupSize;
    private ResourceTicket<T> output;
    private GraphSource<Integer> source;
    private int defaultIndex = 0;
    
    public Junction() {
        this(2);
    }
    public Junction(int length) {
        setLength(length);
    }
    public Junction(int length, int groupSize) {
        setLength(length);
        setGroupSize(groupSize);
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        for (int i = 0; i < length; i++) {
            if (groupSize > 1) {
                addInputGroup(Junction.getInput(i), groupSize);
            } else {
                addInput(Junction.getInput(i));
            }
        }
        if (groupSize > 1) {
            addOutputGroup(Junction.getOutput(), groupSize);
        } else {
            output = addOutput(Junction.getOutput());
        }
    }
    @Override
    protected void prepare(FGRenderContext context) {
        int size;
        if (groupSize > 1) {
            size = getGroups().size()-1;
        } else {
            size = getInputTickets().size()-EXTRA_INPUTS;
        }
        // remove excess tickets
        while (size > length) {
            size--;
            if (groupSize > 1) {
                ResourceTicket[] array = removeGroup(Junction.getInput(size));
                for (ResourceTicket t : array) {
                    t.setSource(null);
                }
            } else {
                getInputTickets().removeLast().setSource(null);
            }
        }
        // add deficit tickets
        while (size < length) {
            if (groupSize > 1) {
                addInputGroup(Junction.getInput(size), groupSize);
            } else {
                addInput(Junction.getInput(size));
            }
            size++;
        }
        // connect output to input
        if (source != null) {
            connect(source.getGraphValue(frameGraph, context.getViewPort()));
        } else {
            connect(defaultIndex);
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
        // This pass will never execute
        return false;
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(length, "length", 2);
        out.write(groupSize, "groupSize", 1);
        out.write(defaultIndex, "defaultIndex", 0);
        if (source != null && source instanceof Savable) {
            out.write((Savable)source, "source", NullSavable.INSTANCE);
        }
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        length = in.readInt("length", 2);
        groupSize = in.readInt("groupSize", 1);
        defaultIndex = in.readInt("defaultIndex", 0);
        source = (GraphSource<Integer>)in.readSavable("source", null);
    }
    
    private void connect(int i) {
        boolean assignNull = i < 0 || i >= length;
        if (groupSize > 1) {
            ResourceTicket[] inArray = getGroupArray(Junction.getInput(i));
            ResourceTicket[] outArray = getGroupArray(Junction.getOutput());
            for (int j = 0; j < groupSize; j++) {
                outArray[j].setSource(assignNull ? null : inArray[j]);
            }
        } else {
            output.setSource(assignNull ? null : getInputTickets().get(i));
        }
    }
    
    public final void setLength(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than zero.");
        }
        this.length = length;
    }
    public final void setGroupSize(int groupSize) {
        if (isAssigned()) {
            throw new IllegalStateException("Cannot alter group size while assigned to a framegraph.");
        }
        if (groupSize <= 0) {
            throw new IllegalArgumentException("Group length must be greater than zero.");
        }
        this.groupSize = groupSize;
    }
    public void setIndexSource(GraphSource<Integer> source) {
        this.source = source;
    }
    public void setDefaultIndex(int defaultIndex) {
        this.defaultIndex = defaultIndex;
    }
    
    public int getLength() {
        return length;
    }
    public int getGroupSize() {
        return groupSize;
    }
    public GraphSource<Integer> getIndexSource() {
        return source;
    }
    public int getDefaultIndex() {
        return defaultIndex;
    }
    
    public static String getInput(int i) {
        return "Input["+i+"]";
    }
    public static String getInput(int i, int j) {
        return "Input["+i+"]["+j+"]";
    }
    public static String getOutput() {
        return "Value";
    }
    public static String getOutput(int i) {
        return "Value["+i+"]";
    }
    
}
