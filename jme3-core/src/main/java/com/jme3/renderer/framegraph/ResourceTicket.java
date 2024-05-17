/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 * References a {@link RenderResource} by either name or index.
 * <p>
 If a resource is referenced by name, the index of the resource
 will be assigned to the ticket, in order to make future references
 faster.
 * 
 * @author codex
 * @param <T>
 */
public class ResourceTicket <T> {
    
    private String name;
    private int localIndex;
    private long objectId = -1;
    private ResourceTicket<T> source;
    
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
        this.name = name;
        this.localIndex = index;
    }
    
    public ResourceTicket<T> copyIndexTo(ResourceTicket<T> target) {
        if (target == null) {
            target = new ResourceTicket();
        }
        return target.setLocalIndex(localIndex);
    }
    public ResourceTicket<T> copyObjectTo(ResourceTicket<T> target) {
        if (target == null) {
            target = new ResourceTicket();
        }
        target.setObjectId(objectId);
        return target;
    }
    
    public void setSource(ResourceTicket<T> source) {
        this.source = source;
    }
    public ResourceTicket<T> setName(String name) {
        this.name = name;
        return this;
    }
    protected ResourceTicket<T> setLocalIndex(int index) {
        this.localIndex = index;
        return this;
    }
    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }
    
    public String getName() {
        return name;
    }
    public int getWorldIndex() {
        if (source != null) {
            int i = source.getWorldIndex();
            if (i >= 0) return i;
        }
        return localIndex;
    }
    public int getLocalIndex() {
        return localIndex;
    }
    public long getObjectId() {
        return objectId;
    }
    public ResourceTicket<T> getSource() {
        return source;
    }
    public boolean hasSource() {
        return source != null;
    }
    
    @Override
    public String toString() {
        return "Ticket[name="+name+", index="+localIndex+"]";
    }
    
}
