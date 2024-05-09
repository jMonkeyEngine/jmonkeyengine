/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.definitions.ResourceDef;

/**
 *
 * @author codex
 * @param <T>
 */
public class RenderResource <T> {
    
    private final ResourceProducer producer;
    private final ResourceDef<T> def;
    private final ResourceTicket<T> ticket;
    private final TimeFrame lifetime;
    private RenderObject<T> object;
    private int refs = 0;
    private int timeout = 0;

    public RenderResource(ResourceProducer producer, ResourceDef<T> def, ResourceTicket<T> ticket) {
        this.producer = producer;
        this.def = def;
        this.ticket = ticket;
        this.lifetime = new TimeFrame(this.producer.getIndex(), 0);
    }
    
    public void reference(int index) {
        lifetime.extendTo(index);
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
    public void setObject(RenderObject<T> object) {
        this.object = object;
        if (this.object != null) {
            ticket.setObjectId(this.object.getId());
        }
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
    public TimeFrame getLifeTime() {
        return lifetime;
    }
    public RenderObject<T> getObject() {
        return object;
    }
    public T getResource() {
        return object.getObject();
    }
    public int getIndex() {
        return ticket.getWorldIndex();
    }
    public int getNumReferences() {
        return refs;
    }
    
    public boolean isVirtual() {
        return object == null;
    }
    public boolean isReferenced() {
        return refs > 0;
    }
    public boolean isUsed() {
        return refs >= 0;
    }
    
    @Override
    public String toString() {
        return "RenderResource[index="+ticket.getWorldIndex()+"]";
    }
    
}
