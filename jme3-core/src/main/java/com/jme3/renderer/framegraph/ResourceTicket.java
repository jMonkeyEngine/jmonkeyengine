/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 * References a {@link RenderResource} by index.
 * <p>
 * A ticket can be set as another tickets source, in which case the former inherits
 * the resource index of the latter.
 * <p>
 * Tickets can also vaguely point to the last known render object, which is used
 * to prioritize that render object, especially for reservations.
 * 
 * @author codex
 * @param <T>
 */
public class ResourceTicket <T> {
    
    private String name;
    private int localIndex;
    private long objectId = -1;
    private ResourceTicket<T> source;
    
    /**
     * Creates a blank ticket with a negative local index.
     */
    public ResourceTicket() {
        this(null, -1);
    }
    /**
     * Creates a ticket with the name and a negative local index.
     * 
     * @param name 
     */
    public ResourceTicket(String name) {
        this(name, -1);
    }
    /**
     * Creates a ticket with the local index.
     * 
     * @param index 
     */
    public ResourceTicket(int index) {
        this(null, index);
    }
    /**
     * Creates a ticket with the name and local index.
     * 
     * @param name
     * @param index 
     */
    public ResourceTicket(String name, int index) {
        this.name = name;
        this.localIndex = index;
    }
    
    /**
     * Copies this ticket's resource index to the target ticket.
     * 
     * @param target
     * @return 
     */
    public ResourceTicket<T> copyIndexTo(ResourceTicket<T> target) {
        if (target == null) {
            target = new ResourceTicket();
        }
        return target.setLocalIndex(localIndex);
    }
    /**
     * Copies this ticket's object ID to the target ticket.
     * 
     * @param target
     * @return 
     */
    public ResourceTicket<T> copyObjectTo(ResourceTicket<T> target) {
        if (target == null) {
            target = new ResourceTicket();
        }
        target.setObjectId(objectId);
        return target;
    }
    
    /**
     * Sets the source ticket.
     * 
     * @param source 
     */
    public void setSource(ResourceTicket<T> source) {
        this.source = source;
    }
    /**
     * Sets the name of this ticket.
     * 
     * @param name
     * @return 
     */
    public ResourceTicket<T> setName(String name) {
        this.name = name;
        return this;
    }
    /**
     * Sets the local index.
     * <p>
     * The local index is overriden if the source ticket is not null and
     * the source's world index is not negative.
     * 
     * @param index
     * @return 
     */
    protected ResourceTicket<T> setLocalIndex(int index) {
        this.localIndex = index;
        return this;
    }
    /**
     * Sets the object ID.
     * 
     * @param objectId 
     */
    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }
    
    /**
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    /**
     * Gets the world index.
     * <p>
     * If the source ticket is null or its world index is negative, this ticket's
     * local index will be returned.
     * 
     * @return 
     */
    public int getWorldIndex() {
        if (source != null) {
            int i = source.getWorldIndex();
            if (i >= 0) return i;
        }
        return localIndex;
    }
    /**
     * 
     * @return 
     */
    public int getLocalIndex() {
        return localIndex;
    }
    /**
     * 
     * @return 
     */
    public long getObjectId() {
        return objectId;
    }
    /**
     * 
     * @return 
     */
    public ResourceTicket<T> getSource() {
        return source;
    }
    /**
     * Returns true if this source ticket is not null.
     * 
     * @return 
     */
    public boolean hasSource() {
        return source != null;
    }
    
    @Override
    public String toString() {
        return "Ticket[name="+name+", index="+localIndex+"]";
    }
    
}
