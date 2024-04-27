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
    
    private String name;
    private int index;
    
    public ResourceTicket(String name) {
        this(name, -1);
    }
    public ResourceTicket(int index) {
        this(null, index);
    }
    public ResourceTicket(String name, int index) {
        this.name = name;
        this.index = index;
    }
    
    public ResourceTicket<T> copy() {
        return new ResourceTicket(name, index);
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setIndex(int index) {
        this.index = index;
    }
    
    public String getName() {
        return name;
    }
    public int getIndex() {
        return index;
    }
    public boolean isLocateByName() {
        return index < 0 && name != null;
    }
    
    @Override
    public String toString() {
        return "ResourceTicket[name="+(name != null ? "\""+name+"\"" : null)+", index="+index+"]";
    }
    
}
