/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
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
import com.jme3.renderer.framegraph.definitions.ResourceDef;
import com.jme3.texture.FrameBuffer;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

/**
 * Performs rendering operations.
 * 
 * @author codex
 */
public abstract class RenderPass implements ResourceProducer, Savable {
    
    private static int nextId = 0;
    
    private int id = nextId++;
    private String name;
    private int index = -1;
    private int refs = 0;
    private final LinkedList<ResourceTicket> inputs = new LinkedList<>();
    private final LinkedList<ResourceTicket> outputs = new LinkedList<>();
    private final LinkedList<PassFrameBuffer> frameBuffers = new LinkedList<>();
    protected ResourceList resources;
    
    public void initializePass(FrameGraph frameGraph, int index) {
        this.index = index;
        this.resources = frameGraph.getResources();
        if (name == null) {
            name = getClass().getSimpleName();
        }
        initialize(frameGraph);
    }
    public void prepareRender(FGRenderContext context) {
        if (index < 0) {
            throw new IllegalStateException("Pass is not properly initialized for rendering.");
        }
        prepare(context);
    }
    public void executeRender(FGRenderContext context) {
        execute(context);
        releaseAll();
    }
    public void resetRender(FGRenderContext context) {
        reset(context);
    }
    public void cleanupPass(FrameGraph frameGraph) {
        cleanup(frameGraph);
        for (ResourceTicket t : inputs) {
            t.setSource(null);
        }
        inputs.clear();
        outputs.clear();
        index = -1;
    }
    
    protected abstract void initialize(FrameGraph frameGraph);
    protected abstract void prepare(FGRenderContext context);
    protected abstract void execute(FGRenderContext context);
    protected abstract void reset(FGRenderContext context);
    protected abstract void cleanup(FrameGraph frameGraph);
    
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
    public void preFrame(FGRenderContext context) {}
    public void postQueue(FGRenderContext context) {}
    
    public void setPassProperty(String name, String value) throws Exception {
        if (!setProperty(name, value)) switch (name) {
            case "id": id = Integer.parseInt(value); break;
            case "name": this.name = value; break;
        }
    }
    protected boolean setProperty(String name, String value) throws Exception {
        return false;
    }
    
    protected <T> ResourceTicket<T> declare(ResourceDef<T> def, ResourceTicket<T> ticket) {
        return resources.declare(this, def, ticket);
    }
    protected <T> ResourceTicket<T> declareLocal(ResourceDef<T> def, ResourceTicket<T> ticket) {
        ticket = resources.declare(this, def, ticket);
        resources.setSurvivesReferenceCull(ticket);
        return ticket;
    }
    protected void reserve(ResourceTicket ticket) {
        resources.reserve(index, ticket);
    }
    protected void reserve(ResourceTicket... tickets) {
        resources.reserve(index, tickets);
    }
    protected void reference(ResourceTicket ticket) {
        resources.reference(index, ticket);
    }
    protected void reference(ResourceTicket... tickets) {
        resources.reference(index, tickets);
    }
    protected boolean referenceOptional(ResourceTicket ticket) {
        return resources.referenceOptional(index, ticket);
    }
    protected void referenceOptional(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            referenceOptional(t);
        }
    }
    protected void releaseAll() {
        for (ResourceTicket t : inputs) {
            resources.releaseOptional(t);
        }
        for (ResourceTicket t : outputs) {
            resources.releaseOptional(t);
        }
    }
    
    protected <T> ResourceTicket<T> addInput(ResourceTicket<T> input) {
        inputs.add(input);
        return input;
    }
    protected <T> ResourceTicket<T> addOutput(ResourceTicket<T> output) {
        outputs.add(output);
        return output;
    }
    protected <T> ResourceTicket<T> addInput(String name) {
        return addInput(new ResourceTicket<>(name));
    }
    protected <T> ResourceTicket<T> addOutput(String name) {
        return addOutput(new ResourceTicket<>(name));
    }
    protected ResourceTicket getInputByName(String name) {
        for (ResourceTicket t : inputs) {
            if (name.equals(t.getName())) {
                return t;
            }
        }
        return null;
    }
    protected ResourceTicket getOutputByName(String name) {
        for (ResourceTicket t : outputs) {
            if (name.equals(t.getName())) {
                return t;
            }
        }
        return null;
    }
    public void makeInput(RenderPass pass, String outTicket, String inTicket) {
        ResourceTicket out = Objects.requireNonNull(pass.getOutputByName(outTicket));
        ResourceTicket in = Objects.requireNonNull(getInputByName(inTicket));
        in.setSource(out);
    }
    public void disconnectFrom(RenderPass pass) {
        for (ResourceTicket in : inputs) {
            if (pass.getOutputTickets().contains(in.getSource())) {
                in.setSource(null);
            }
        }
    }
    
    protected FrameBuffer getFrameBuffer(int width, int height, int samples) {
        for (PassFrameBuffer fb : frameBuffers) {
            if (fb.qualifies(width, height, samples)) {
                return fb.use();
            }
        }
        PassFrameBuffer fb = new PassFrameBuffer(width, height, samples);
        frameBuffers.add(fb);
        return fb.use();
    }
    protected FrameBuffer getFrameBuffer(FGRenderContext context, int samples) {
        return getFrameBuffer(context.getWidth(), context.getHeight(), samples);
    }
    
    public void countReferences() {
        refs = outputs.size();
    }
    public void shiftExecutionIndex(int threshold, boolean positive) {
        if (index > threshold) {
            index += (positive ? 1 : -1);
        }
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    public String getProfilerName() {
        return getName();
    }
    public int getId() {
        return id;
    }
    public boolean isAssigned() {
        return index >= 0;
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
        out.write(id, "id", -1);
        out.write(name, "name", "RenderPass");
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        id = in.readInt("id", -1);
        name = in.readString("name", "RenderPass");
    }
    
    public static void setNextId(int id) {
        nextId = id;
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
