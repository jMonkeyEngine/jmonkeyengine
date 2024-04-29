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
    
    private int index;
    
    public ResourceTicket() {
        this(-1);
    }
    public ResourceTicket(int index) {
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
    public ResourceTicket<T> copyTo(ResourceTicket<T> target) {
        if (target == null) {
            target = new ResourceTicket();
        }
        target.index = index;
        return target;
    }
    
    /**
     * Sets the resource index.
     * 
     * @param index 
     */
    public void setIndex(int index) {
        this.index = index;
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
        return "ResourceTicket["+index+"]";
    }
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.index;
        return hash;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ResourceTicket<?> other = (ResourceTicket<?>) obj;
        return this.index == other.index;
    }
    
}
