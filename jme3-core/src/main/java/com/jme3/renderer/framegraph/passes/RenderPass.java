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
import com.jme3.export.Savable;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceList;
import com.jme3.renderer.framegraph.ResourceProducer;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.debug.GraphEventCapture;
import com.jme3.renderer.framegraph.definitions.ResourceDef;
import com.jme3.texture.FrameBuffer;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

/**
 * Performs rendering operations for a framegraph.
 * 
 * @author codex
 */
public abstract class RenderPass implements ResourceProducer, Savable {
    
    private static int nextId = 0;
    
    private int id = nextId++;
    private int exportId = -1;
    private String name;
    private int index = -1;
    private int refs = 0;
    private final LinkedList<ResourceTicket> inputs = new LinkedList<>();
    private final LinkedList<ResourceTicket> outputs = new LinkedList<>();
    private final LinkedList<PassFrameBuffer> frameBuffers = new LinkedList<>();
    private final HashMap<String, ResourceTicket[]> groups = new HashMap<>();
    protected ResourceList resources;
    protected boolean autoTicketRelease = true;
    
    /**
     * Initializes the pass to the framegraph.
     * 
     * @param frameGraph
     * @param index execution index
     */
    public void initializePass(FrameGraph frameGraph, int index) {
        this.index = index;
        this.resources = frameGraph.getResources();
        if (name == null) {
            name = getClass().getSimpleName();
        }
        initialize(frameGraph);
    }
    /**
     * Prepares the pass for rendering.
     * 
     * @param context 
     */
    public void prepareRender(FGRenderContext context) {
        if (index < 0) {
            throw new IllegalStateException("Pass is not properly initialized for rendering.");
        }
        prepare(context);
    }
    /**
     * Executes the pass.
     * 
     * @param context 
     */
    public void executeRender(FGRenderContext context) {
        execute(context);
        if (autoTicketRelease) {
            releaseAll();
        }
    }
    /**
     * Resets the pass from rendering.
     * 
     * @param context 
     */
    public void resetRender(FGRenderContext context) {
        reset(context);
    }
    /**
     * Cleans up the pass's participation in the framegraph.
     * 
     * @param frameGraph 
     */
    public void cleanupPass(FrameGraph frameGraph) {
        cleanup(frameGraph);
        for (ResourceTicket t : inputs) {
            t.setSource(null);
            t.setPassId(-1);
        }
        for (ResourceTicket t : outputs) {
            t.setSource(null);
            t.setPassId(-1);
        }
        inputs.clear();
        outputs.clear();
        groups.clear();
        index = -1;
    }
    
    /**
     * Initializes the pass.
     * <p>
     * Tickets should be created add registered here.
     * 
     * @param frameGraph 
     */
    protected abstract void initialize(FrameGraph frameGraph);
    /**
     * Prepares the pass.
     * <p>
     * Resource should be declared, referenced, and reserved here.
     * 
     * @param context 
     */
    protected abstract void prepare(FGRenderContext context);
    /**
     * Executes the pass.
     * <p>
     * All declared and referenced resources should be acquired here. Resources
     * must also be released, but that occurs automatically.
     * 
     * @param context 
     */
    protected abstract void execute(FGRenderContext context);
    /**
     * Resets the pass.
     * 
     * @param context 
     */
    protected abstract void reset(FGRenderContext context);
    /**
     * Cleans up the pass.
     * 
     * @param frameGraph 
     */
    protected abstract void cleanup(FrameGraph frameGraph);
    
    /**
     * Called when all rendering is complete in a rendering frame this pass
     * participated in.
     */
    public void renderingComplete() {
        for (Iterator<PassFrameBuffer> it = frameBuffers.iterator(); it.hasNext();) {
            PassFrameBuffer fb = it.next();
            if (!fb.used) {
                fb.dispose();
                it.remove();
            } else {
                fb.used = false;
            }
        }
    }
    /**
     * Called on the pre-rendering step.
     * 
     * @param context 
     */
    public void preFrame(FGRenderContext context) {}
    /**
     * Called after render queues have been assembled.
     * 
     * @param context 
     */
    public void postQueue(FGRenderContext context) {}
    
    /**
     * Declares a new resource.
     * 
     * @param <T>
     * @param def
     * @param ticket ticket to store resulting index
     * @return given ticket
     */
    protected <T> ResourceTicket<T> declare(ResourceDef<T> def, ResourceTicket<T> ticket) {
        return resources.declare(this, def, ticket);
    }
    /**
     * Declares a new resource that is locally used only.
     * 
     * @param <T>
     * @param def
     * @param ticket
     * @return 
     */
    protected <T> ResourceTicket<T> declareLocal(ResourceDef<T> def, ResourceTicket<T> ticket) {
        ticket = resources.declare(this, def, ticket);
        resources.setSurvivesReferenceCull(ticket);
        return ticket;
    }
    /**
     * Reserves the resource associated with the ticket.
     * 
     * @param ticket 
     */
    protected void reserve(ResourceTicket ticket) {
        resources.reserve(index, ticket);
    }
    /**
     * Reserves each resource associated with the tickets.
     * 
     * @param tickets 
     */
    protected void reserve(ResourceTicket... tickets) {
        resources.reserve(index, tickets);
    }
    /**
     * References the resource associated with the ticket.
     * 
     * @param ticket 
     */
    protected void reference(ResourceTicket ticket) {
        resources.reference(index, ticket);
    }
    /**
     * References each resource associated with the tickets.
     * 
     * @param tickets 
     */
    protected void reference(ResourceTicket... tickets) {
        resources.reference(index, tickets);
    }
    /**
     * References the resource associated with the ticket if the ticket is not
     * null and contains a non-negative world index.
     * 
     * @param ticket
     * @return 
     */
    protected boolean referenceOptional(ResourceTicket ticket) {
        return resources.referenceOptional(index, ticket);
    }
    /**
     * Optionally references each resource associated with the tickets.
     * 
     * @param tickets 
     */
    protected void referenceOptional(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            referenceOptional(t);
        }
    }
    
    /**
     * Acquires a set of resources from a ticket group and stores them in
     * the array.
     * 
     * @param <T>
     * @param name
     * @param array
     * @return 
     */
    protected <T> T[] acquireGroup(String name, T[] array) {
        ResourceTicket<T>[] tickets = Objects.requireNonNull(getGroup(name), "Ticket group cannot be null.");
        int n = Math.min(array.length, tickets.length);
        for (int i = 0; i < n; i++) {
            array[i] = resources.acquire(tickets[i]);
        }
        return array;
    }
    /**
     * Acquires a set of resources from a ticket group and stores them in
     * the array.
     * <p>
     * Tickets that are invalid will acquire {@code val}.
     * 
     * @param <T>
     * @param name
     * @param array
     * @param val
     * @return 
     */
    protected <T> T[] acquireGroupOrElse(String name, T[] array, T val) {
        ResourceTicket<T>[] tickets = Objects.requireNonNull(getGroup(name), "Ticket group cannot be null.");
        int n = Math.min(array.length, tickets.length);
        for (int i = 0; i < n; i++) {
            array[i] = resources.acquireOrElse(tickets[i], val);
        }
        return array;
    }
    /**
     * Releases all reasources associated with any registered ticket.
     * <p>
     * Called automatically.
     */
    protected void releaseAll() {
        for (ResourceTicket t : inputs) {
            resources.releaseOptional(t);
        }
        for (ResourceTicket t : outputs) {
            resources.releaseOptional(t);
        }
    }
    
    /**
     * Adds the ticket as input.
     * 
     * @param <T>
     * @param input
     * @return given ticket
     */
    protected <T> ResourceTicket<T> addInput(ResourceTicket<T> input) {
        inputs.add(input);
        input.setPassId(id);
        return input;
    }
    /**
     * Adds the ticket as output.
     * 
     * @param <T>
     * @param output
     * @return given ticket
     */
    protected <T> ResourceTicket<T> addOutput(ResourceTicket<T> output) {
        outputs.add(output);
        output.setPassId(id);
        return output;
    }
    /**
     * Creates and registers a new ticket as input.
     * 
     * @param <T>
     * @param name
     * @return created ticket
     */
    protected <T> ResourceTicket<T> addInput(String name) {
        return addInput(new ResourceTicket<>(name));
    }
    /**
     * Creates and registers a new ticket as output.
     * 
     * @param <T>
     * @param name
     * @return created ticket
     */
    protected <T> ResourceTicket<T> addOutput(String name) {
        return addOutput(new ResourceTicket<>(name));
    }
    /**
     * Adds the ticket array as a group input under the given name.
     * 
     * @param <T>
     * @param name
     * @param array
     * @return ticket array
     */
    protected <T> ResourceTicket<T>[] addInputGroup(String name, ResourceTicket<T>... array) {
        for (ResourceTicket<T> t : array) {
            if (t == null) {
                throw new NullPointerException("Ticket cannot be null in group.");
            }
            addInput(t);
        }
        groups.put(name, array);
        return array;
    }
    /**
     * Adds the ticket array as a group output under the given name.
     * 
     * @param <T>
     * @param name
     * @param array
     * @return ticket array
     */
    protected <T> ResourceTicket<T>[] addOutputGroup(String name, ResourceTicket<T>... array) {
        for (ResourceTicket<T> t : array) {
            if (t == null) {
                throw new NullPointerException("Ticket cannot be null in group.");
            }
            addOutput(t);
        }
        groups.put(name, array);
        return array;
    }
    /**
     * Creates and adds a ticket array as a group input of the specified length under the given name.
     * <p>
     * Each ticket's name is formatted as the {@code groupName+"["+index+"]"}.
     * 
     * @param <T>
     * @param name
     * @param length
     * @return created ticket array
     */
    protected <T> ResourceTicket<T>[] addInputGroup(String name, int length) {
        ResourceTicket<T>[] array = new ResourceTicket[length];
        for (int i = 0; i < length; i++) {
            addInput(array[i] = new ResourceTicket<>(name+"["+i+"]"));
        }
        groups.put(name, array);
        return array;
    }
    /**
     * Creates and adds a ticket array as a group output of the specified length under the given name.
     * <p>
     * Each ticket's name is formatted as the {@code groupName+"["+index+"]"}.
     * 
     * @param <T>
     * @param name
     * @param length
     * @return create ticket array
     */
    protected <T> ResourceTicket<T>[] addOutputGroup(String name, int length) {
        ResourceTicket<T>[] array = new ResourceTicket[length];
        for (int i = 0; i < length; i++) {
            addOutput(array[i] = new ResourceTicket<>(name+"["+i+"]"));
        }
        groups.put(name, array);
        return array;
    }
    
    /**
     * Removes all members of the named group from the input and output lists.
     * 
     * @param <T>
     * @param name
     * @return 
     */
    protected <T> ResourceTicket<T>[] removeGroup(String name) {
        ResourceTicket<T>[] array = groups.remove(name);
        if (array == null) {
            return null;
        }
        // Once we determine which list group members were added to, we only
        // need to remove from that list for future members.
        byte state = 0;
        for (ResourceTicket<T> t : array) {
            if (state >= 0 && inputs.remove(t)) {
                state = 1;
            }
            if (state <= 0 && outputs.remove(t)) {
                state = -1;
            }
        }
        return array;
    }
    
    /**
     * Gets the named input ticket, or null if none exists.
     * 
     * @param name
     * @return 
     */
    public ResourceTicket getInput(String name) {
        for (ResourceTicket t : inputs) {
            if (name.equals(t.getName())) {
                return t;
            }
        }
        return null;
    }
    /**
     * Gets the named output ticket, or null if none exists.
     * 
     * @param name
     * @return 
     */
    public ResourceTicket getOutput(String name) {
        for (ResourceTicket t : outputs) {
            if (name.equals(t.getName())) {
                return t;
            }
        }
        return null;
    }
    /**
     * Gets the ticket array registered under the name.
     * 
     * @param name
     * @return 
     */
    public ResourceTicket[] getGroup(String name) {
        return groups.get(name);
    }
    
    /**
     * Makes the named source (output) ticket belonging to the given pass the source of
     * the named target (input) ticket belonging to this pass.
     * <p>
     * If both the source name and target name correspond to ticket groups, the
     * groups will be connected.
     * 
     * @param sourcePass
     * @param sourceTicket
     * @param targetTicket 
     * @param start start index (inclusive) for connecting groups
     * @param end end index (exclusive) for connecting groups
     */
    public void makeInput(RenderPass sourcePass, String sourceTicket, String targetTicket, int start, int end) {
        ResourceTicket[] sourceArray = sourcePass.getGroup(sourceTicket);
        if (sourceArray != null) {
            ResourceTicket[] targetArray = getGroup(targetTicket);
            if (targetArray != null) {
                int n = Math.min(end, Math.min(targetArray.length, sourceArray.length));
                for (int i = start; i < n; i++) {
                    targetArray[i].setSource(sourceArray[i]);
                }
                return;
            }
        }
        ResourceTicket source = Objects.requireNonNull(sourcePass.getOutput(sourceTicket), "Source ticket cannot be null.");
        ResourceTicket target = Objects.requireNonNull(getInput(targetTicket), "Target ticket cannot be null.");
        target.setSource(source);
    }
    /**
     * Makes the named source (output) ticket belonging to the given pass the source of
     * the named target (input) ticket belonging to this pass.
     * <p>
     * If both the source name and target name correspond to ticket groups, the
     * groups will be connected.
     * 
     * @param sourcePass
     * @param sourceTicket
     * @param targetTicket
     * @param start start index (inclusive) for connecting groups
     */
    public void makeInput(RenderPass sourcePass, String sourceTicket, String targetTicket, int start) {
        makeInput(sourcePass, sourceTicket, targetTicket, start, Integer.MAX_VALUE);
    }
    /**
     * Makes the named source (output) ticket belonging to the given pass the source of
     * the named target (input) ticket belonging to this pass.
     * <p>
     * If both the source name and target name correspond to ticket groups, the
     * groups will be connected.
     * 
     * @param pass
     * @param sourceTicket
     * @param targetTicket 
     */
    public void makeInput(RenderPass pass, String sourceTicket, String targetTicket) {
        makeInput(pass, sourceTicket, targetTicket, 0, Integer.MAX_VALUE);
    }
    /**
     * Nullifies all sources belonging to the given pass.
     * 
     * @param pass 
     */
    public void disconnectFrom(RenderPass pass) {
        for (ResourceTicket in : inputs) {
            if (pass.getOutputTickets().contains(in.getSource())) {
                in.setSource(null);
            }
        }
    }
    
    /**
     * Gets an existing framebuffer that matches the given properties.
     * <p>
     * If no existing framebuffer matches, a new framebuffer will be created
     * and returned.
     * 
     * @param cap graph event capturer, for debugging, may be null.
     * @param width
     * @param height
     * @param samples
     * @return 
     */
    protected FrameBuffer getFrameBuffer(GraphEventCapture cap, int width, int height, int samples) {
        for (PassFrameBuffer fb : frameBuffers) {
            if (fb.qualifies(width, height, samples)) {
                return fb.use();
            }
        }
        PassFrameBuffer fb = new PassFrameBuffer(width, height, samples);
        frameBuffers.add(fb);
        if (cap != null) cap.createFrameBuffer(fb.frameBuffer);
        return fb.use();
    }
    /**
     * Gets an existing framebuffer that matches the given properties.
     * <p>
     * In no existing framebuffer matches, a new framebuffer will be created
     * and returned.
     * 
     * @param width
     * @param height
     * @param samples
     * @return 
     */
    protected FrameBuffer getFrameBuffer(int width, int height, int samples) {
        return getFrameBuffer(null, width, height, samples);
    }
    /**
     * Gets an existing framebuffer that matches the context.
     * <p>
     * If no existing framebuffer matches, a new framebuffer will be created
     * and returned. Uses event capturing if available.
     * 
     * @param context
     * @param samples
     * @return 
     */
    protected FrameBuffer getFrameBuffer(FGRenderContext context, int samples) {
        return getFrameBuffer(context.getGraphCapture(), context.getWidth(), context.getHeight(), samples);
    }
    
    /**
     * Counts the number of potential references to this pass.
     * <p>
     * Called automatically. Do not use.
     */
    public void countReferences() {
        refs = outputs.size();
    }
    /**
     * Shifts the execution index if this pass's index is greater than the
     * specified threshold.
     * <p>
     * Called automatically. Do not use.
     * 
     * @param threshold
     * @param positive 
     */
    public void shiftExecutionIndex(int threshold, boolean positive) {
        if (index > threshold) {
            index += (positive ? 1 : -1);
        }
    }
    /**
     * Shifts the id of this pass.
     * <p>
     * Called automatically. Do not use.
     * 
     * @param shift 
     */
    public void shiftId(int shift) {
        id += shift;
    }
    /**
     * Sets the name of this pass.
     * 
     * @param name 
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * Sets the id of this pass.
     * <p>
     * Called automatically. Do not use.
     * 
     * @param id 
     */
    public void setId(int id) {
        this.id = id;
    }
    /**
     * Sets the id used when exporting.
     * <p>
     * Called automatically. Do not use.
     * 
     * @param id 
     */
    public void setExportId(int id) {
        this.exportId = id;
    }
    
    /**
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    /**
     * Gets the name used for profiling.
     * 
     * @return 
     */
    public String getProfilerName() {
        return getName();
    }
    /**
     * 
     * @return 
     */
    public int getId() {
        return id;
    }
    /**
     * 
     * @return 
     */
    public int getExportId() {
        return exportId;
    }
    /**
     * 
     * @return 
     */
    public int getIndex() {
        return index;
    }
    /**
     * Returns true if this pass is assigned to a framegraph.
     * 
     * @return 
     */
    public boolean isAssigned() {
        return index >= 0;
    }
    /**
     * 
     * @return 
     */
    protected HashMap<String, ResourceTicket[]> getGroups() {
        return groups;
    }
    
    @Override
    public int getExecutionIndex() {
        return index;
    }
    @Override
    public boolean dereference() {
        refs--;
        return isUsed();
    }
    @Override
    public boolean isUsed() {
        return refs > 0;
    }
    @Override
    public LinkedList<ResourceTicket> getInputTickets() {
        return inputs;
    }
    @Override
    public LinkedList<ResourceTicket> getOutputTickets() {
        return outputs;
    }
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write((exportId >= 0 ? exportId : id), "id", -1);
        out.write(name, "name", "RenderPass");
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        id = in.readInt("id", -1);
        name = in.readString("name", "RenderPass");
    }
    
    /**
     * Gets the next generated id.
     * 
     * @return 
     */
    public static int getNextId() {
        return nextId;
    }
    
    private static class PassFrameBuffer {
        
        public final FrameBuffer frameBuffer;
        public boolean used = false;
        
        public PassFrameBuffer(int width, int height, int samples) {
            frameBuffer = new FrameBuffer(width, height, samples);
        }
        
        public FrameBuffer use() {
            used = true;
            return frameBuffer;
        }
        
        public boolean qualifies(int width, int height, int samples) {
            return frameBuffer.getWidth()   == width
                && frameBuffer.getHeight()  == height
                && frameBuffer.getSamples() == samples;
        }
        
        public void dispose() {
            frameBuffer.dispose();
        }
        
    }
    
}
