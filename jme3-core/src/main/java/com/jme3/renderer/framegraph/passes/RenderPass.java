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
import com.jme3.renderer.framegraph.modules.RenderModule;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceList;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.TicketGroup;
import com.jme3.renderer.framegraph.debug.GraphEventCapture;
import com.jme3.renderer.framegraph.definitions.ResourceDef;
import com.jme3.texture.FrameBuffer;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Modular rendering process for a {@link FrameGraph}.
 * 
 * @author codex
 */
public abstract class RenderPass extends RenderModule implements Savable {
    
    private final LinkedList<PassFrameBuffer> frameBuffers = new LinkedList<>();
    protected ResourceList resources;
    protected boolean autoTicketRelease = true;
    
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        write(ex.getCapsule(this));
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        read(im.getCapsule(this));
    }
    @Override
    public void traverse(Consumer<RenderModule> traverser) {
        traverser.accept(this);
    }
    
    @Override
    public void initModule(FrameGraph frameGraph) {
        initialize(frameGraph);
    }
    @Override
    public void prepareRender(FGRenderContext context) {
        resources = context.getResources();
        prepare(context);
    }
    @Override
    public void executeRender(FGRenderContext context) {
        if (context.isAsync()) {
            waitToExecute();
        }
        execute(context);
        if (autoTicketRelease) {
            releaseAll();
        }
        if (index.isMainThread()) {
            context.popRenderSettings();
        }
    }
    @Override
    public void resetRender(FGRenderContext context) {
        reset(context);
    }
    @Override
    public void cleanupModule(FrameGraph frameGraph) {
        cleanup(frameGraph);
        inputs.clear();
        outputs.clear();
        groups.clear();
        this.frameGraph = null;
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
    @Override
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
     * Declares a new resource using a registered ticket.
     * 
     * @param <T>
     * @param def definition for new resource
     * @param ticket ticket to store resulting index
     * @return given ticket
     * @see #declareTemporary(com.jme3.renderer.framegraph.definitions.ResourceDef, com.jme3.renderer.framegraph.ResourceTicket)
     */
    protected <T> ResourceTicket<T> declare(ResourceDef<T> def, ResourceTicket<T> ticket) {
        return resources.declare(this, def, ticket);
    }
    /**
     * Declares a resource using an unregistered ticket.
     * 
     * @param <T>
     * @param def
     * @param ticket
     * @return 
     * @see #declare(com.jme3.renderer.framegraph.definitions.ResourceDef, com.jme3.renderer.framegraph.ResourceTicket)
     */
    protected <T> ResourceTicket<T> declareTemporary(ResourceDef<T> def, ResourceTicket<T> ticket) {
        return resources.declareTemporary(this, def, ticket);
    }
    /**
     * Reserves the {@link com.jme3.renderer.framegraph.RenderObject RenderObject} associated with the ticket.
     * 
     * @param ticket 
     */
    protected void reserve(ResourceTicket ticket) {
        resources.reserve(index, ticket);
    }
    /**
     * Reserves each RenderObject associated with the tickets.
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
     */
    protected void referenceOptional(ResourceTicket ticket) {
        resources.referenceOptional(index, ticket);
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
     * Forces this thread to wait until all inputs are available for this pass.
     * <p>
     * An incoming resource is deemed ready when {@link com.jme3.renderer.framegraph.ResourceView#claimReadPermissions() read
     * permissions are claimed}.
     */
    public void waitToExecute() {
        for (ResourceTicket t : inputs) {
            resources.waitForResource(t, index.threadIndex);
        }
    }
    
    /**
     * Acquires a set of resources from a ticket group and stores them in the array.
     * 
     * @param <T>
     * @param name
     * @param array
     * @return 
     */
    protected <T> T[] acquireArray(String name, T[] array) {
        ResourceTicket<T>[] tickets = getGroup(name).getArray();
        int n = Math.min(array.length, tickets.length);
        for (int i = 0; i < n; i++) {
            array[i] = resources.acquire(tickets[i]);
        }
        return array;
    }
    /**
     * Acquires a set of resources from a ticket group and stores them in an
     * array created by the function.
     * 
     * @param <T>
     * @param name
     * @param func
     * @return 
     */
    protected <T> T[] acquireArray(String name, Function<Integer, T[]> func) {
        ResourceTicket<T>[] tickets = getGroup(name).getArray();
        T[] array = func.apply(tickets.length);
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
    protected <T> T[] acquireArrayOrElse(String name, T[] array, T val) {
        ResourceTicket<T>[] tickets = getGroup(name).getArray();
        int n = Math.min(array.length, tickets.length);
        for (int i = 0; i < n; i++) {
            array[i] = resources.acquireOrElse(tickets[i], val);
        }
        return array;
    }
    /**
     * Acquires a set of resources from a ticket group and stores them in an
     * array created by the function.
     * <p>
     * Tickets that are invalid will acquire {@code val}.
     * 
     * @param <T>
     * @param name
     * @param func
     * @param val
     * @return 
     */
    protected <T> T[] acquireArrayOrElse(String name, Function<Integer, T[]> func, T val) {
        ResourceTicket<T>[] tickets = getGroup(name).getArray();
        T[] array = func.apply(tickets.length);
        int n = Math.min(array.length, tickets.length);
        for (int i = 0; i < n; i++) {
            array[i] = resources.acquireOrElse(tickets[i], val);
        }
        return array;
    }
    /**
     * Acquires a list of resources from a ticket group and stores them in the given list.
     * 
     * @param <T>
     * @param name group name
     * @param list list to store resources in (or null to create a new {@link LinkedList}).
     * @return given list
     */
    protected <T> List<T> acquireList(String name, List<T> list) {
        if (list == null) {
            list = new LinkedList<>();
        }
        ResourceTicket<T>[] tickets = getGroup(name).getArray();
        for (ResourceTicket<T> t : tickets) {
            T res = resources.acquireOrElse(t, null);
            if (res != null) list.add(res);
        }
        return list;
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
     * Removes all members of the named group from the input and output lists.
     * 
     * @param <T>
     * @param name
     * @return 
     */
    protected <T> ResourceTicket<T>[] removeGroup(String name) {
        TicketGroup<T> group = groups.remove(name);
        if (group == null) {
            return null;
        }
        // Once we determine which list group members were added to, we only
        // need to remove from that list for future members.
        byte state = 0;
        if (group.isList()) state = 1;
        for (ResourceTicket<T> t : group.getArray()) {
            if (state >= 0 && inputs.remove(t)) {
                state = 1;
            }
            if (state <= 0 && outputs.remove(t)) {
                state = -1;
            }
        }
        return group.getArray();
    }
    
    /**
     * Gets an existing {@link FrameBuffer} that matches the given properties.
     * <p>
     * If no existing FrameBuffer matches, a new framebuffer will be created
     * and returned. FrameBuffers that are not used during pass execution
     * are disposed.
     * <p>
     * If the event capturer is not null, an event will be logged for debugging.
     * 
     * @param cap graph event capturer for debugging (may be null)
     * @param tag tag (name) requirement for returned FrameBuffer (may be null)
     * @param width width requirement for returned FrameBuffer
     * @param height height requirement for returned FrameBuffer
     * @param samples samples requirement for returned FrameBuffer
     * @return FrameBuffer matching given width, height, and samples
     */
    protected FrameBuffer getFrameBuffer(GraphEventCapture cap, String tag, int width, int height, int samples) {
        if (tag == null) {
            tag = PassFrameBuffer.DEF_TAG;
        }
        for (PassFrameBuffer fb : frameBuffers) {
            if (fb.qualifies(tag, width, height, samples)) {
                return fb.use();
            }
        }
        PassFrameBuffer fb = new PassFrameBuffer(tag, width, height, samples);
        frameBuffers.add(fb);
        if (cap != null) cap.createFrameBuffer(fb.frameBuffer);
        return fb.use();
    }
    /**
     * 
     * @param cap
     * @param width
     * @param height
     * @param samples
     * @return 
     * @see #getFrameBuffer(com.jme3.renderer.framegraph.debug.GraphEventCapture, java.lang.String, int, int, int) 
     */
    protected FrameBuffer getFrameBuffer(GraphEventCapture cap, int width, int height, int samples) {
        return getFrameBuffer(cap, null, width, height, samples);
    }
    /**
     * 
     * @param width
     * @param height
     * @param samples
     * @return 
     * @see #getFrameBuffer(com.jme3.renderer.framegraph.debug.GraphEventCapture, java.lang.String, int, int, int) 
     */
    protected FrameBuffer getFrameBuffer(int width, int height, int samples) {
        return getFrameBuffer(null, null, width, height, samples);
    }
    /**
     * 
     * @param tag
     * @param width
     * @param height
     * @param samples
     * @return 
     * @see #getFrameBuffer(com.jme3.renderer.framegraph.debug.GraphEventCapture, java.lang.String, int, int, int) 
     */
    protected FrameBuffer getFrameBuffer(String tag, int width, int height, int samples) {
        return getFrameBuffer(null, tag, width, height, samples);
    }
    /**
     * Creates a FrameBuffer matching the width and height presented by the {@link FGRenderContext}.
     * 
     * @param context
     * @param samples
     * @return 
     * @see #getFrameBuffer(com.jme3.renderer.framegraph.debug.GraphEventCapture, java.lang.String, int, int, int) 
     */
    protected FrameBuffer getFrameBuffer(FGRenderContext context, int samples) {
        return getFrameBuffer(context.getGraphCapture(), context.getWidth(), context.getHeight(), samples);
    }
    /**
     * Creates a FrameBuffer matching the width and height presented by the {@link FGRenderContext}.
     * 
     * @param context
     * @param tag
     * @param samples
     * @return 
     * @see #getFrameBuffer(com.jme3.renderer.framegraph.debug.GraphEventCapture, java.lang.String, int, int, int) 
     */
    protected FrameBuffer getFrameBuffer(FGRenderContext context, String tag, int samples) {
        return getFrameBuffer(context.getGraphCapture(), tag, context.getWidth(), context.getHeight(), samples);
    }
    
    /**
     * Gets the name given to a profiler, which may be more compact or informative.
     * 
     * @return 
     */
    public String getProfilerName() {
        return getName();
    }
    
    /**
     * Convenience method for writing pass properties to the output capsule.
     * 
     * @param out
     * @throws IOException 
     */
    protected void write(OutputCapsule out) throws IOException {}
    /**
     * Convenience method for reading pass properties from the input capsule.
     * 
     * @param in
     * @throws IOException 
     */
    protected void read(InputCapsule in) throws IOException {}
    
    private static class PassFrameBuffer {
        
        public static final String DEF_TAG = "#DefaultTag";
        
        public final String tag;
        public final FrameBuffer frameBuffer;
        public boolean used = false;
        
        public PassFrameBuffer(int width, int height, int samples) {
            this(DEF_TAG, width, height, samples);
        }
        public PassFrameBuffer(String tag, int width, int height, int samples) {
            this.tag = tag;
            frameBuffer = new FrameBuffer(width, height, samples);
        }
        
        public FrameBuffer use() {
            used = true;
            return frameBuffer;
        }
        
        public boolean qualifies(String tag, int width, int height, int samples) {
            return frameBuffer.getWidth()   == width
                && frameBuffer.getHeight()  == height
                && frameBuffer.getSamples() == samples
                && this.tag.equals(tag);
        }
        
        public void dispose() {
            frameBuffer.dispose();
        }
        
    }
    
}
