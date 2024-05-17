/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.definitions.ResourceDef;
import java.util.Objects;

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
    private RenderObject object;
    private T resource;
    private int refs = 0;
    private int timeout = 0;
    private boolean survivesRefCull = false;
    private boolean undefined = false;

    public RenderResource(ResourceProducer producer, ResourceDef<T> def, ResourceTicket<T> ticket) {
        this.producer = producer;
        this.def = def;
        this.ticket = ticket;
        this.lifetime = new TimeFrame(this.producer.getExecutionIndex(), 0);
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
        setObject(object, object.getObject());
    }
    public void setObject(RenderObject object, T resource) {
        Objects.requireNonNull(resource, "Object resource cannot be null.");
        if (undefined) {
            throw new IllegalStateException("Resource is already undefined.");
        }
        if (object.isAcquired()) {
            throw new IllegalStateException("Object is already acquired.");
        }
        this.object = object;
        this.resource = resource;
        if (this.object != null) {
            this.object.acquire();
            ticket.setObjectId(this.object.getId());
        }
    }
    public void setUndefined() {
        if (object != null) {
            throw new IllegalArgumentException("Resource is already defined.");
        }
        undefined = true;
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
    public RenderObject getObject() {
        return object;
    }
    public T getResource() {
        return resource;
    }
    public int getIndex() {
        return ticket.getWorldIndex();
    }
    public int getNumReferences() {
        return refs;
    }
    public void setSurvivesRefCull(boolean survivesRefCull) {
        this.survivesRefCull = survivesRefCull;
    }
    
    public boolean isVirtual() {
        return object == null && !undefined;
    }
    public boolean isReferenced() {
        return refs > 0;
    }
    public boolean isUsed() {
        return refs >= 0;
    }
    public boolean isUndefined() {
        return undefined;
    }
    public boolean isSurvivesRefCull() {
        return survivesRefCull;
    }
    
    @Override
    public String toString() {
        return "RenderResource[index="+ticket.getWorldIndex()+"]";
    }
    
}
