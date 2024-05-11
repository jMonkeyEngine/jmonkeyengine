/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.renderer.framegraph.CameraSize;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceList;
import com.jme3.renderer.framegraph.ResourceProducer;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.ResourceDef;
import com.jme3.texture.FrameBuffer;
import java.util.Arrays;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public abstract class RenderPass implements ResourceProducer {
    
    private static int nextId = 0;
    
    private final LinkedList<ResourceTicket> inputs = new LinkedList<>();
    private final LinkedList<ResourceTicket> outputs = new LinkedList<>();
    private final CameraSize camSize = new CameraSize();
    private int id = nextId++;
    private String name = "";
    private int index = -1;
    private int refs = 0;
    protected ResourceList resources;
    protected FrameBuffer frameBuffer;
    
    public void initializePass(FrameGraph frameGraph, int index) {
        this.index = index;
        this.resources = frameGraph.getResources();
        initialize(frameGraph);
    }
    public void prepareRender(FGRenderContext context) {
        if (index < 0) {
            throw new IllegalStateException("Pass is not properly initialized for rendering.");
        }
        prepare(context);
    }
    public void executeRender(FGRenderContext context) {
        if (camSize.update(context.getCameraSize()) || frameBuffer == null) {
            if (frameBuffer != null) {
                disposeFrameBuffer(frameBuffer);
            }
            frameBuffer = createFrameBuffer(context);
        }
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
    
    protected FrameBuffer createFrameBuffer(FGRenderContext context) {
        return null;
    }
    protected void disposeFrameBuffer(FrameBuffer fb) {
        fb.dispose();
    }
    
    protected <T> ResourceTicket<T> declare(ResourceDef<T> def, ResourceTicket<T> ticket) {
        ticket = resources.declare(this, def, ticket);
        //addOutput(ticket);
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
        //addInput(ticket);
    }
    protected void reference(ResourceTicket... tickets) {
        resources.reference(index, tickets);
        //addInputs(tickets);
    }
    protected boolean referenceOptional(ResourceTicket ticket) {
        if (resources.referenceOptional(index, ticket)) {
            //addInput(ticket);
            return true;
        }
        return false;
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
    protected void addInputs(ResourceTicket... inputs) {
        this.inputs.addAll(Arrays.asList(inputs));
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
    
    public void disconnectFrom(RenderPass pass) {
        for (ResourceTicket in : inputs) {
            if (pass.getOutputTickets().contains(in.getSource())) {
                in.setSource(null);
            }
        }
    }
    
    public void countReferences() {
        refs = outputs.size();
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setId(int id) {
        this.id = id;
    }
    public void useNextId() {
        id = nextId++;
    }
    public void shiftExecutionIndex(int base, int amount) {
        if (index > base) {
            index += amount;
        }
    }
    
    public String getName() {
        return name;
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
        return isReferenced();
    }
    @Override
    public boolean isReferenced() {
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
    
    public static void setNextId(int id) {
        nextId = id;
    }
    
}
