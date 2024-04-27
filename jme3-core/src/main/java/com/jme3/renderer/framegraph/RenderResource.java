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
    
    private final RenderPass producer;
    private final ResourceDef<T> def;
    private final ResourceTicket<T> ticket;
    private T resource;
    private int refs = 0;
    private int users = 0;

    public RenderResource(RenderPass producer, ResourceDef<T> def, ResourceTicket<T> ticket) {
        this.producer = producer;
        this.def = def;
        this.ticket = ticket;
    }
    
    public void create() {
        if (resource == null) {
            resource = def.create();
        }
    }
    public void setResource(T resource) {
        this.resource = resource;
    }
    
    public void acquire() {
        users++;
    }
    public boolean release() {
        users--;
        return isUsed();
    }
    public boolean isUsed() {
        return users > 0;
    }
    
    public void reference() {
        refs++;
    }
    public boolean dereference() {
        refs--;
        return isReferenced();
    }
    public boolean isReferenced() {
        return refs > 0;
    }
    
    public RenderPass getProducer() {
        return producer;
    }
    public ResourceDef<T> getDefinition() {
        return def;
    }
    public ResourceTicket getTicket() {
        return ticket;
    }
    public T getResource() {
        return resource;
    }
    public boolean isVirtual() {
        return resource == null;
    }
    
}
