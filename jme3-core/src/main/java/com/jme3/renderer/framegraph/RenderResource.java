/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author codex
 * @param <T>
 */
public class RenderResource <T> {
    
    private final ResourceProducer producer;
    private final ResourceDef<T> def;
    private final ResourceTicket<T> ticket;
    private T resource;
    private int refs = 0;
    private int timeout = 0;
    private boolean watched = false;

    public RenderResource(ResourceProducer producer, ResourceDef<T> def, ResourceTicket<T> ticket) {
        this.producer = producer;
        this.def = def;
        this.ticket = ticket;
    }
    
    public void create() {
        resource = def.create();
    }
    
    public void reference() {
        refs++;
    }
    public void release() {
        refs--;
    }
    public boolean tickTimeout() {
        return timeout-- > 0;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    public void setResource(T resource) {
        this.resource = resource;
    }
    public void setWatched(boolean watched) {
        this.watched = watched;
    }
    
    public ResourceProducer getProducer() {
        return producer;
    }
    public ResourceDef<T> getDefinition() {
        return def;
    }
    public ResourceTicket<T> getTicket() {
        return ticket;
    }
    public T getResource() {
        return resource;
    }
    public int getIndex() {
        return ticket.getIndex();
    }
    public int getNumReferences() {
        return refs;
    }
    
    public boolean isVirtual() {
        return resource == null;
    }
    public boolean isReferenced() {
        return refs > 0;
    }
    public boolean isUsed() {
        return refs >= 0;
    }
    public boolean isWatched() {
        return watched;
    }
    
    @Override
    public String toString() {
        return "RenderResource[index="+ticket.getIndex()+", resource="
                +(resource != null ? resource.getClass().getName() : "null")+"]";
    }
    
}
