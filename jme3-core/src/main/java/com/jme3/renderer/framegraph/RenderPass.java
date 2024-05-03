/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.texture.FrameBuffer;
import java.util.Arrays;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public abstract class RenderPass implements ResourceProducer {
    
    protected ResourceList resources;
    private final LinkedList<ResourceTicket> inputs = new LinkedList<>();
    private final LinkedList<ResourceTicket> outputs = new LinkedList<>();
    private final CameraSize camSize = new CameraSize();
    private int refs = 0;
    protected FrameBuffer frameBuffer;
    
    public void initializePass(FrameGraph frameGraph) {
        this.resources = frameGraph.getResources();
        initialize(frameGraph);
    }
    public void prepareRender(FGRenderContext context) {
        prepare(context);
    }
    public void executeRender(FGRenderContext context) {
        if (camSize.update(context.getCameraSize()) || frameBuffer == null) {
            if (frameBuffer != null) {
                destroyFrameBuffer(context, frameBuffer);
            }
            frameBuffer = createFrameBuffer(context);
        }
        execute(context);
        context.popRenderSettings();
        releaseAll();
    }
    public void resetRender(FGRenderContext context) {
        reset(context);
        inputs.clear();
        outputs.clear();
    }
    public void cleanupPass(FrameGraph frameGraph) {
        cleanup(frameGraph);
    }
    
    protected abstract void initialize(FrameGraph frameGraph);
    protected abstract void prepare(FGRenderContext context);
    protected abstract void execute(FGRenderContext context);
    protected abstract void reset(FGRenderContext context);
    protected abstract void cleanup(FrameGraph frameGraph);
    
    protected FrameBuffer createFrameBuffer(FGRenderContext context) {
        return null;
    }
    protected void destroyFrameBuffer(FGRenderContext context, FrameBuffer fbo) {
        fbo.dispose();
        //fbo.deleteObject(context.getRenderer());
    }
    
    protected <T> ResourceTicket<T> register(ResourceDef<T> def, ResourceTicket<T> ticket) {
        ticket = resources.register(this, def, ticket);
        addOutput(ticket);
        return ticket;
    }
    protected void reference(ResourceTicket ticket) {
        resources.reference(ticket);
        addInput(ticket);
    }
    protected void reference(ResourceTicket... tickets) {
        resources.reference(tickets);
        addInputs(tickets);
    }
    protected boolean referenceOptional(ResourceTicket ticket) {
        if (resources.referenceOptional(ticket)) {
            addInput(ticket);
            return true;
        }
        return false;
    }
    protected void referenceOptional(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            referenceOptional(t);
        }
    }
    private void releaseAll() {
        for (ResourceTicket t : inputs) {
            resources.release(t);
        }
        for (ResourceTicket t : outputs) {
            resources.release(t);
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
    
    public void countReferences() {
        refs = outputs.size();
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
    
}
