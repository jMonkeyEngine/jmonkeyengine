/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.modules;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.framegraph.Connectable;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.PassIndex;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.ResourceUser;
import com.jme3.renderer.framegraph.TicketGroup;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author codex
 */
public abstract class RenderModule implements Connectable, ResourceUser, Savable {
    
    protected FrameGraph frameGraph;
    protected String name = "RenderModule";
    protected RenderContainer parent;
    protected final PassIndex index = new PassIndex();
    protected final LinkedList<ResourceTicket> inputs = new LinkedList<>();
    protected final LinkedList<ResourceTicket> outputs = new LinkedList<>();
    protected final HashMap<String, TicketGroup> groups = new HashMap<>();
    private boolean interrupted = false;
    private int refs = 0;
    private int id = -1;
    
    @Override
    public LinkedList<ResourceTicket> getInputTickets() {
        return inputs;
    }
    @Override
    public LinkedList<ResourceTicket> getOutputTickets() {
        return outputs;
    }
    @Override
    public PassIndex getIndex() {
        return index;
    }
    @Override
    public void countReferences() {
        refs = outputs.size();
    }
    @Override
    public void dereference() {
        refs--;
    }
    @Override
    public boolean isUsed() {
        return refs > 0;
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(name, "name", "RenderModule");
        out.write(id, "exportId", -1);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        name = in.readString("name", "RenderModule");
        id = in.readInt("exportId", -1);
    }
    
    @Override
    public ResourceTicket getInput(String name) {
        return getTicketFromStream(inputs.stream(), name);
    }
    @Override
    public ResourceTicket getOutput(String name) {
        return getTicketFromStream(outputs.stream(), name);
    }
    @Override
    public TicketGroup getGroup(String name) {
        return groups.get(name);
    }
    @Override
    public ResourceTicket addListEntry(String groupName) {
        TicketGroup g = getGroup(name, true);
        g.requireAsList(true);
        return addInput(g.add());
    }
    
    public int getId() {
        return id;
    }
    
    /**
     * Adds the ticket as input.
     * 
     * @param <T>
     * @param input
     * @return given ticket
     */
    public <T> ResourceTicket<T> addInput(ResourceTicket<T> input) {
        getInputTickets().add(input);
        return input;
    }
    /**
     * Adds the ticket as output.
     * 
     * @param <T>
     * @param output
     * @return given ticket
     */
    public <T> ResourceTicket<T> addOutput(ResourceTicket<T> output) {
        getOutputTickets().add(output);
        return output;
    }
    /**
     * Creates and registers a new ticket as input.
     * 
     * @param <T>
     * @param name name assigned to the new ticket
     * @return created ticket
     */
    public <T> ResourceTicket<T> addInput(String name) {
        ResourceTicket.validateUserTicketName(name);
        return addInput(new ResourceTicket<>(name));
    }
    /**
     * Creates and registers a new ticket as output.
     * 
     * @param <T>
     * @param name name assigned to the new ticket
     * @return created ticket
     */
    public <T> ResourceTicket<T> addOutput(String name) {
        ResourceTicket.validateUserTicketName(name);
        return addOutput(new ResourceTicket<>(name));
    }
    /**
     * Creates and adds a ticket array as a group input of the specified length under the given name.
     * <p>
     * A group bundles several tickets together so that they can easily be used together.
     * Each individual ticket is handled just like any other, it is just registered with the group as well.
     * Names are formatted as {@code groupName+"["+index+"]"}.
     * 
     * @param <T>
     * @param name
     * @param length
     * @return created ticket array
     */
    public <T> ResourceTicket<T>[] addInputGroup(String name, int length) {
        ResourceTicket.validateUserTicketName(name);
        TicketGroup group = new TicketGroup(name, length);
        for (int i = 0; i < length; i++) {
            group.getArray()[i] = addInput(group.create(i));
        }
        groups.put(name, group);
        return group.getArray();
    }
    /**
     * Creates and adds a ticket array as a group output of the specified length under the given name.
     * <p>
     * A group bundles several tickets together so that they can easily be used together.
     * Each individual ticket is handled just like any other, it is just registered with the group as well.
     * Names are formatted as {@code groupName+"["+index+"]"}.
     * 
     * @param <T>
     * @param name
     * @param length
     * @return create ticket array
     */
    public <T> ResourceTicket<T>[] addOutputGroup(String name, int length) {
        ResourceTicket.validateUserTicketName(name);
        TicketGroup group = new TicketGroup(name, length);
        for (int i = 0; i < length; i++) {
            group.getArray()[i] = addOutput(group.create(i));
        }
        groups.put(name, group);
        return group.getArray();
    }
    /**
     * Creates an input ticket list.
     * <p>
     * A ticket list is an extension of a ticket group where the size is indefinite, meaning connections
     * can be added or removed at will. The order of connections is not guaranteed, especially
     * when a ticket list is loaded from a save file.
     * <p>
     * Each addition or removal from a ticket list requires an array resize, so it a ticket
     * list should remain static where possible.
     * 
     * @param name 
     */
    public void addInputList(String name) {
        ResourceTicket.validateUserTicketName(name);
        groups.put(name, new TicketGroup(name));
    }
    
    private static ResourceTicket getTicketFromStream(Stream<ResourceTicket> stream, String name) {
        return stream.filter(t -> name.equals(t.getName())).findFirst().orElse(null);
    }
    public ResourceTicket[] getGroupArray(String name) {
        return getGroup(name, true).getArray();
    }
    
    /**
     * Interrupts execution of this module.
     */
    public void interrupt() {
        interrupted = true;
    }
    
    public void initializeModule(FrameGraph frameGraph) {
        if (this.frameGraph != null) {
            throw new IllegalStateException("Module already initialized.");
        }
        this.frameGraph = frameGraph;
        id = this.frameGraph.getNextId();
        initModule(this.frameGraph);
    }
    public void prepareModuleRender(FGRenderContext context, PassIndex index) {
        this.index.set(index);
        prepareRender(context);
    }
    public void executeModuleRender(FGRenderContext context) {
        if (!isUsed()) {
            return;
        }
        executeRender(context);
    }
    public void cleanupModule() {
        id = -1;
        if (frameGraph != null) {
            cleanupModule(frameGraph);
            frameGraph = null;
        }
    }
    
    /**
     * Initializes the RenderModule implementation.
     * <p>
     * For most cases, use {@link #initializeModule(com.jme3.renderer.framegraph.FrameGraph)}
     * instead.
     * 
     * @param frameGraph 
     */
    protected abstract void initModule(FrameGraph frameGraph);
    /**
     * Prepares the RenderModule implementation.
     * <p>
     * For most cases, use {@link #prepareModuleRender(com.jme3.renderer.framegraph.FGRenderContext, com.jme3.renderer.framegraph.PassIndex)}
     * instead.
     * 
     * @param context 
     */
    protected abstract void prepareRender(FGRenderContext context);
    /**
     * Executes the RenderModule implementation.
     * <p>
     * For most cases, use {@link #executeModuleRender(com.jme3.renderer.framegraph.FGRenderContext)}
     * instead.
     * 
     * @param context 
     */
    protected abstract void executeRender(FGRenderContext context);
    /**
     * Resets the RenderModule after execution.
     * 
     * @param context 
     */
    protected abstract void resetRender(FGRenderContext context);
    /**
     * Cleans up the RenderModule implementation.
     * <p>
     * For most cases, use {@link #cleanupModule()} instead.
     * 
     * @param frameGraph 
     */
    protected abstract void cleanupModule(FrameGraph frameGraph);
    
    protected abstract void renderingComplete();
    public abstract void traverse(Consumer<RenderModule> traverser);
    
    public void setName(String name) {
        this.name = name;
    }
    protected boolean setParent(RenderContainer parent) {
        this.parent = parent;
        return true;
    }
    
    public String getName() {
        return name;
    }
    public RenderContainer getParent() {
        return parent;
    }
    public boolean isAssigned() {
        return frameGraph != null;
    }
    public boolean isInterrupted() {
        return interrupted;
    }
    public boolean isAsync() {
        return !index.isMainThread();
    }
    public boolean isAncenstor(RenderModule ancestor) {
        RenderModule p = parent;
        while (p != null) {
            if (p == ancestor) {
                return true;
            }
            p = p.parent;
        }
        return false;
    }
    
}
