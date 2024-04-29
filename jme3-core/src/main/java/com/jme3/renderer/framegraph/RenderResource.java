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
    private int index;
    private T resource;
    private int refs = 0;

    public RenderResource(RenderPass producer, ResourceDef<T> def) {
        this.producer = producer;
        this.def = def;
    }
    
    public void create() {
        resource = def.create();
    }
    public void setResource(T resource) {
        this.resource = resource;
    }
    public void setIndex(int index) {
        this.index = index;
    }
    
    public void reference() {
        refs++;
    }
    public void release() {
        refs--;
    }
    
    public RenderPass getProducer() {
        return producer;
    }
    public ResourceDef<T> getDefinition() {
        return def;
    }
    public int getIndex() {
        return index;
    }
    public T getResource() {
        return resource;
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
    
}
