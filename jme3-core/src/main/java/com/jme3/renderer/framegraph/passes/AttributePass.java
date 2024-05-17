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
import com.jme3.renderer.framegraph.GraphTarget;

/**
 *
 * @author codex
 * @param <T>
 */
public class AttributePass <T> extends RenderPass implements Function<Object, T> {
    
    private final Class<T> paramType;
    private String paramName;
    private ResourceTicket<T> in, out;
    private T value;
    private ValueDef<T> def;
    private GraphTarget<T> target;
    private GraphSource<T> source;

    public AttributePass(Class<T> paramType, String paramName) {
        this.paramType = paramType;
        this.paramName = paramName;
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        in = addInput("Value");
        out = addOutput("Value");
        def = new ValueDef<>(null, this);
        def.setDisposeOnRelease(true);
        def.setUseExisting(false);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(def, out);
        referenceOptional(in);
    }
    @Override
    protected void execute(FGRenderContext context) {
        value = resources.acquireOrElse(in, null);
        if (value != null && target != null) {
            target.setGraphValue(context.getViewPort(), value);
            resources.setConstant(in);
        }
        if (source != null) {
            value = source.getGraphValue(context.getViewPort());
        }
        if (value != null) {
            resources.acquire(out);
            resources.setConstant(out);
        } else {
            resources.setUndefined(out);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean isUsed() {
        return super.isUsed() || in.hasSource();
    }
    @Override
    public T apply(Object t) {
        return value;
    }
    
    public void setParamName(String name) {
        this.paramName = name;
    }
    public void setTarget(GraphTarget<T> target) {
        this.target = target;
    }
    public void setSource(GraphSource<T> source) {
        this.source = source;
    }
    
    public Class<T> getParamType() {
        return paramType;
    }
    public String getParamName() {
        return paramName;
    }
    
}
