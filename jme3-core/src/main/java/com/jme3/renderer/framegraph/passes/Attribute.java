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
    
    private final Class<T> paramType;
    private String paramName;
    private T value;
    private ResourceTicket<T> ticket;
    private ValueDef<T> def;

    public Attribute(Class<T> paramType) {
        this.paramType = paramType;
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        ticket = addOutput("Value");
        def = new ValueDef<>(null, this);
        def.setDisposeOnRelease(true);
        def.setUseExisting(false);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(def, ticket);
    }
    @Override
    protected void execute(FGRenderContext context) {
        if (paramName != null) {
            value = context.getBlackboard().get(paramName, paramType);
        }
        if (value != null) {
            resources.acquire(ticket);
            resources.setConstant(ticket);
        } else {
            resources.setUndefined(ticket);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public T apply(Object t) {
        return value;
    }
    
    public void setParamName(String name) {
        this.paramName = name;
    }
    public void setValue(T value) {
        this.value = value;
    }
    
    public Class<T> getParamType() {
        return paramType;
    }
    public String getParamName() {
        return paramName;
    }
    public T getValue() {
        return value;
    }
    
}
