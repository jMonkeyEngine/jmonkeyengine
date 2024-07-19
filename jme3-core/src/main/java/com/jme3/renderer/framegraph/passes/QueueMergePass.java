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
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.GeometryQueue;
import com.jme3.renderer.framegraph.ResourceTicket;
import java.io.IOException;

/**
 * Merges a specified number of {@link GeometryQueue}s into one output queue.
 * <p>
 * Inputs:
 * <ul>
 *   <li>Queues[n] ({@link GeometryQueue}: queues to merge into one.</li>
 * </ul>
 * Outputs:
 * <ul>
 *   <li>Result ({@link GeometryQueue}): resulting geometry queue.</li>
 * </ul>
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
        System.out.println("acquire queues to merge");
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
