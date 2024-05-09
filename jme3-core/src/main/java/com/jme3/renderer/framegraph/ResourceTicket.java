/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.ArrayList;
import java.util.Objects;

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
    
    private static long nextId = 0;
    
    private final long id;
    private String name;
    private int localIndex;
    private int objectId = -1;
    private int sourceIndex = 0;
    private ArrayList<ResourceTicket<T>> sources;
    
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
        this.localIndex = index;
    }
    
    public int addSource(ResourceTicket<T> source) {
        if (sources == null) {
            sources = new ArrayList<>(1);
        }
        if (sources.add(source)) {
            return sources.size()-1;
        }
        return -1;
    }
    public boolean removeSource(ResourceTicket<T> source) {
        return sources.remove(source);
    }
    public void clearSources() {
        sources.clear();
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
    
    public ResourceTicket<T> setName(String name) {
        this.name = name;
        return this;
    }
    protected ResourceTicket<T> setLocalIndex(int index) {
        this.localIndex = index;
        return this;
    }
    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }
    public boolean setSourceIndex(int sourceIndex) {
        this.sourceIndex = sourceIndex;
        return this.sourceIndex >= 0 && this.sourceIndex < sources.size();
    }
    
    public long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public int getWorldIndex() {
        ResourceTicket src = getCurrentSource();
        if (src != null) {
            int i = src.getWorldIndex();
            if (i >= 0) return i;
        }
        return localIndex;
    }
    public int getLocalIndex() {
        return localIndex;
    }
    public int getObjectId() {
        return objectId;
    }
    public ResourceTicket<T> getCurrentSource() {
        if (sourceIndex >= 0 && sourceIndex < sources.size()) {
            return sources.get(sourceIndex);
        }
        return null;
    }
    public ArrayList<ResourceTicket<T>> getSources() {
        return sources;
    }
    
    @Override
    public String toString() {
        return "Ticket[id="+id+", name="+name+", index="+localIndex+"]";
    }
    
}
