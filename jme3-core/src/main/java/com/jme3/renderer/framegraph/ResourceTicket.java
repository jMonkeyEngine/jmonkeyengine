/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 * References a {@link RenderResource} by either name or index.
 * <p>
 * If a resource is referenced by name, the index of the resource
 * will be assigned to the ticket, in order to make future references
 * faster.
 * 
 * @author codex
 * @param <T>
 */
public class ResourceTicket <T> {
    
    private static long nextId = 0;
    
    private final long id;
    private String name;
    private int index;
    
    public ResourceTicket() {
        this(null, -1);
    }
    public ResourceTicket(String name) {
        this(name, -1);
    }
    public ResourceTicket(int index) {
        this(null, index);
    }
    public ResourceTicket(String name, int index) {
        this.id = nextId++;
        this.name = name;
        this.index = index;
    }
    
    /**
     * Copies this ticket's info to the target.
     * <p>
     * If the target is null, a new instance will be created and
     * written to.
     * 
     * @param target copy target (can be null)
     * @return target
     */
    public ResourceTicket<T> copyIndexTo(ResourceTicket<T> target) {
        if (target == null) {
            target = new ResourceTicket();
        }
        target.index = index;
        return target;
    }
    
    /**
     * Sets the name of this ticket.
     * <p>
     * Names are used to locate a particular ticket among a group.
     * 
     * @param name 
     * @return this ticket instance
     */
    public ResourceTicket<T> setName(String name) {
        this.name = name;
        return this;
    }
    
    /**
     * Sets the index.
     * 
     * @param index
     * @return this instance
     */
    protected ResourceTicket<T> setIndex(int index) {
        this.index = index;
        return this;
    }
    
    /**
     * Gets the id unique to this ticket.
     * 
     * @return 
     */
    public long getId() {
        return id;
    }
    
    /**
     * Gets the name of this ticket.
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the resource index.
     * 
     * @return 
     */
    public int getIndex() {
        return index;
    }
    
    @Override
    public String toString() {
        return "Ticket[id="+id+", name="+name+", index="+index+"]";
    }
    
}
