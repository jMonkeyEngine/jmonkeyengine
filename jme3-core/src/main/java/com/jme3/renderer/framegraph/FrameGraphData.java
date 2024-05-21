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
package com.jme3.renderer.framegraph;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.framegraph.passes.RenderPass;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Holds framegraph data ready for import or export.
 * <p>
 * Each individual data instance should only be affiliated with one framegraph.
 * 
 * @author codex
 */
public class FrameGraphData implements Savable {
    
    private final boolean export;
    private String name;
    private RenderPass[] passes;
    private SavablePassConnection[] connections;
    
    public FrameGraphData() {
        export = false;
    }
    public FrameGraphData(FrameGraph fg, Collection<RenderPass> passes) {
        this.name = fg.getName();
        this.passes = passes.toArray(RenderPass[]::new);
        export = true;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        if (!export) {
            throw new IllegalStateException("Data is import only.");
        }
        if (passes == null) {
            throw new IllegalStateException("Data is already consumed.");
        }
        final HashMap<Integer, Integer> idMap = new HashMap<>();
        final LinkedList<SavablePassConnection> list = new LinkedList<>();
        int nextId = 0;
        // remap ids
        for (RenderPass p : passes) {
            p.setExportId(nextId++);
            idMap.put(p.getId(), p.getExportId());
        }
        // extract connections
        for (RenderPass p : passes) for (ResourceTicket t : p.getInputTickets()) {
            if (t.hasSource()) {
                int outId = idMap.get(t.getSource().getPassId());
                list.add(new SavablePassConnection(p.getExportId(), outId, t.getName(), t.getSource().getName()));
            }
        }
        OutputCapsule out = ex.getCapsule(this);
        out.write(name, "name", "FrameGraph");
        out.write(passes, "passes", new RenderPass[0]);
        out.write(list.toArray(SavablePassConnection[]::new), "connections", new SavablePassConnection[0]);
        // reset export ids
        for (RenderPass p : passes) {
            p.setExportId(-1);
        }
        idMap.clear();
        list.clear();
        passes = null;
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        if (export) {
            throw new IllegalStateException("Data is export only.");
        }
        InputCapsule in = im.getCapsule(this);
        name = in.readString("name", "FrameGraph");
        int baseId = RenderPass.getNextId();
        Savable[] array = in.readSavableArray("passes", new RenderPass[0]);
        passes = new RenderPass[array.length];
        for (int i = 0; i < array.length; i++) {
            RenderPass p = passes[i] = (RenderPass)array[i];
            p.shiftId(baseId);
        }
        array = in.readSavableArray("connections", new SavablePassConnection[0]);
        connections = new SavablePassConnection[array.length];
        for (int i = 0; i < array.length; i++) {
            SavablePassConnection c = connections[i] = (SavablePassConnection)array[i];
            c.shiftIds(baseId);
        }
    }
    
    /**
     * Applies internal data to the framegraph.
     * <p>
     * This operation consumes the data.
     * 
     * @param fg 
     */
    public void apply(FrameGraph fg) {
        if (export) {
            throw new IllegalStateException("Data is export only.");
        }
        if (passes == null) {
            throw new IllegalStateException("Data has already been consumed.");
        }
        fg.setName(name);
        for (RenderPass p : passes) {
            fg.add(p);
        }
        // cache passes by id
        HashMap<Integer, RenderPass> cache = new HashMap<>();
        for (RenderPass p : passes) {
            cache.put(p.getId(), p);
        }
        // read connections
        for (SavablePassConnection c : connections) {
            RenderPass input = cache.get(c.getInputId());
            RenderPass output = cache.get(c.getOutputId());
            input.makeInput(output, c.getOutputTicket(), c.getInputTicket());
        }
        cache.clear();
        passes = null;
        connections = null;
    }
    
    public boolean isExportOnly() {
        return export;
    }
    public boolean isConsumed() {
        return passes == null;
    }
    
}
