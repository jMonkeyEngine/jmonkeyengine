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
package com.jme3.renderer.framegraph.export;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.framegraph.Connectable;
import com.jme3.renderer.framegraph.modules.RenderModule;
import com.jme3.renderer.framegraph.ResourceTicket;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * Holds savable data for a root module and its descendents.
 * 
 * @author codex
 */
public class ModuleGraphData implements Savable {
    
    private static final ArrayList<SavableConnection> DEF_CONNECTIONS = new ArrayList<>();
    
    private RenderModule rootModule;
    
    public ModuleGraphData() {}
    public ModuleGraphData(RenderModule root) {
        this.rootModule = root;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        if (rootModule == null) {
            throw new NullPointerException("Root module cannot be null.");
        }
        // extract connections
        final ArrayList<SavableConnection> connections = new ArrayList<>();
        final LinkedList<RenderModule> members = new LinkedList<>();
        rootModule.traverse(new ModuleTreeExtractor(members));
        // descend the queue, so that output ids can be reset in the same pass
        for (Iterator<RenderModule> it = members.descendingIterator(); it.hasNext();) {
            RenderModule m = it.next();
            for (ResourceTicket t : m.getInputTickets()) {
                if (t.hasSource()) {
                    int sourceId = t.getSource().getExportGroupId();
                    if (sourceId < 0) {
                        connections.add(new SavableConnection(m.getId(), sourceId,
                                t.getName(), t.getSource().getName()));
                    }
                }
            }
            // reset output ids, since they will no longer be used
            for (ResourceTicket t : m.getOutputTickets()) {
                t.setExportGroupId(-1);
            }
        }
        // write
        OutputCapsule out = ex.getCapsule(this);
        out.write(rootModule, "root", null);
        out.writeSavableArrayList(connections, "connections", DEF_CONNECTIONS);
        connections.clear();
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        rootModule = in.readSavable("root", RenderModule.class, null);
        final ArrayList<SavableConnection> connections = in.readSavableArrayList("connections", DEF_CONNECTIONS);
        final HashMap<Integer, RenderModule> registry = new HashMap<>();
        rootModule.traverse(m -> { if (registry.put(m.getId(), m) != null)
                throw new IllegalStateException("Modules with duplicate ids imported."); });
        for (SavableConnection c : connections) {
            Connectable source = registry.get(c.getSourceId());
            if (source == null) {
                throw new NullPointerException("Source of connection not found.");
            }
            Connectable target = registry.get(c.getTargetId());
            if (target == null) {
                throw new NullPointerException("Target of connection not found.");
            }
            target.makeInput(source, c.getSourceTicket(), c.getTargetTicket());
        }
        connections.clear();
        registry.clear();
    }
    
    public void setRootModule(RenderModule rootModule) {
        this.rootModule = rootModule;
    }
    public RenderModule getRootModule() {
        return rootModule;
    }
    public <T extends RenderModule> T getRootModule(Class<T> requiredType) {
        if (rootModule != null && !requiredType.isAssignableFrom(rootModule.getClass())) {
            throw new ClassCastException("Module tree root is a "+rootModule.getClass().getName()
                    + " when required as a "+requiredType.getName());
        }
        return (T)rootModule;
    }
    
    private static class ModuleTreeExtractor implements Consumer<RenderModule> {
        
        private final LinkedList<RenderModule> members;
        
        public ModuleTreeExtractor(LinkedList<RenderModule> members) {
            this.members = members;
        }
        
        @Override
        public void accept(RenderModule m) {
            members.add(m);
            // only need to apply id to export tickets, since we will have the
            // correct id handy for input tickets when we extract connections.
            for (ResourceTicket t : m.getOutputTickets()) {
                t.setExportGroupId(m.getId());
            }
        }
        
        public LinkedList<RenderModule> getMembers() {
            return members;
        }
    
    }
    
}
