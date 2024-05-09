/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.ValueDef;
import java.util.function.Function;

/**
 *
 * @author codex
 * @param <T>
 */
public class Attribute <T> extends RenderPass implements Function<Object, T> {

    private ResourceTicket<T> ticket;
    private ValueDef<T> def;
    private T value;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        ticket = addOutput("Attribute");
        def = new ValueDef<>(null, this);
        def.setDispose(true);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(def, ticket);
    }
    @Override
    protected void execute(FGRenderContext context) {
        resources.acquire(ticket);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public T apply(Object t) {
        if (value == null) {
            throw new NullPointerException("Attribute value cannot be null.");
        }
        return value;
    }
    
    public void setValue(T value) {
        this.value = value;
    }
    
    public T getValue() {
        return value;
    }
    
}
