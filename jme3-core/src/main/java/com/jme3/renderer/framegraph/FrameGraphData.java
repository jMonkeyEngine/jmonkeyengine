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
import com.jme3.export.SavableObject;
import com.jme3.renderer.framegraph.passes.RenderPass;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Holds FrameGraph snapshot data ready for import or export.
 * 
 * @author codex
 */
public class FrameGraphData implements Savable {
    
    private static final String DEF_NAME = "FrameGraph";
    private static final ArrayList<PassThread> DEF_QUEUES = new ArrayList<>(0);
    private static final SavablePassConnection[] DEF_CONNECTIONS = new SavablePassConnection[0];
    private static final HashMap<String, Savable> DEF_SETTINGS = new HashMap<>();
    
    private final boolean export;
    private String name;
    private ArrayList<PassThread> queues;
    private SavablePassConnection[] connections;
    private Map<String, SavableObject> settings;
    
    public FrameGraphData() {
        export = false;
    }
    public FrameGraphData(FrameGraph fg, ArrayList<PassThread> queues, Map<String, Object> settings) {
        this.name = fg.getName();
        this.queues = queues;
        this.settings = new HashMap<>();
        for (String key : settings.keySet()) {
            this.settings.put(key, new SavableObject(settings.get(key)));
        }
        export = true;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        if (!export) {
            throw new IllegalStateException("Data is import only.");
        }
        if (queues == null) {
            throw new IllegalStateException("Data is already consumed.");
        }
        final HashMap<Integer, Integer> idMap = new HashMap<>();
        final LinkedList<SavablePassConnection> list = new LinkedList<>();
        int nextId = 0;
        // remap ids
        for (PassThread q : queues) {
            for (RenderPass p : q) {
                p.setExportId(nextId++);
                idMap.put(p.getId(), p.getExportId());
            }
        }
        // extract connections
        for (PassThread q : queues) {
            for (RenderPass p : q) for (ResourceTicket t : p.getInputTickets()) {
                if (t.hasSource()) {
                    int outId = idMap.get(t.getSource().getPassId());
                    list.add(new SavablePassConnection(p.getExportId(), outId, t.getName(), t.getSource().getName()));
                }
            }
        }
        OutputCapsule out = ex.getCapsule(this);
        out.write(name, "name", DEF_NAME);
        out.writeSavableArrayList(queues, "passes", DEF_QUEUES);
        out.write(list.toArray(new SavablePassConnection[0]), "connections", DEF_CONNECTIONS);
        out.writeStringSavableMap(settings, "settings", DEF_SETTINGS);
        // reset export ids
        for (PassThread q : queues) {
            for (RenderPass p : q) {
                p.setExportId(-1);
            }
        }
        idMap.clear();
        list.clear();
        queues = null;
        settings.clear();
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        if (export) {
            throw new IllegalStateException("Data is export only.");
        }
        InputCapsule in = im.getCapsule(this);
        name = in.readString("name", "FrameGraph");
        int baseId = RenderPass.getNextId();
        queues = in.readSavableArrayList("passes", DEF_QUEUES);
        for (PassThread q : queues) {
            for (RenderPass p : q) {
                p.shiftId(baseId);
            }
        }
        Savable[] array = in.readSavableArray("connections", new SavablePassConnection[0]);
        connections = new SavablePassConnection[array.length];
        for (int i = 0; i < array.length; i++) {
            SavablePassConnection c = connections[i] = (SavablePassConnection)array[i];
            c.shiftIds(baseId);
        }
        settings = (Map<String, SavableObject>)in.readStringSavableMap("settings", DEF_SETTINGS);
    }
    
    /**
     * Applies internal data to the FrameGraph.
     * <p>
     * This operation consumes the data.
     * 
     * @param fg 
     */
    public void apply(FrameGraph fg) {
        if (export) {
            throw new IllegalStateException("Data is export only.");
        }
        if (queues == null) {
            throw new IllegalStateException("Data has already been consumed.");
        }
        fg.setName(name);
        // cache passes by id
        HashMap<Integer, RenderPass> cache = new HashMap<>();
        for (PassThread q : queues) {
            for (RenderPass p : q) {
                fg.add(p, new PassIndex().setThreadIndex(q.getIndex()));
                cache.put(p.getId(), p);
            }
        }
        // read connections
        for (SavablePassConnection c : connections) {
            RenderPass input = cache.get(c.getInputId());
            RenderPass output = cache.get(c.getOutputId());
            input.makeInput(output, c.getOutputTicket(), c.getInputTicket());
        }
        // transfer settings
        for (String key : settings.keySet()) {
            fg.setSetting(key, settings.get(key).getObject());
        }
        cache.clear();
        queues = null;
        connections = null;
    }
    
    /**
     * Returns true if this data is export only, otherwise the data is import only.
     * 
     * @return 
     */
    public boolean isExportOnly() {
        return export;
    }
    
    /**
     * Returns true if this data has been consumed by completing an import
     * or export.
     * <p>
     * Attempting to import or export consumed data will result in an exception.
     * 
     * @return 
     */
    public boolean isConsumed() {
        return queues == null;
    }
    
}
