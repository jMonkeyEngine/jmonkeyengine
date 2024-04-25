/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.parameters;

/**
 *
 * @author codex
 * @param <T>
 */
public class WorldRenderParam <T> implements RenderParameter<T> {
    
    private final String name;
    private ParameterSpace space;
    private String target;
    private boolean pull;

    public WorldRenderParam(String name, ParameterSpace space, String target) {
        this(name, space, target, true);
    }
    public WorldRenderParam(String name, ParameterSpace space, String target, boolean pull) {
        this.name = name;
        this.space = space;
        this.target = target;
        this.pull = pull;
    }
    
    @Override
    public String getParameterName() {
        return name;
    }
    @Override
    public void set(T value) {
        RenderParameter<T> p = space.getParameter(target, false);
        if (p != null) {
            p.set(value);
        }
    }
    @Override
    public T get() {
        RenderParameter<T> p = space.getParameter(target, false);
        if (p != null) {
            return p.get();
        } else {
            return null;
        }
    }

    public void setSpace(ParameterSpace space) {
        this.space = space;
    }
    public void setTarget(String target) {
        this.target = target;
    }
    public void setPull(boolean pull) {
        this.pull = pull;
    }

    public ParameterSpace getSpace() {
        return space;
    }
    public String getTarget() {
        return target;
    }
    public boolean isPull() {
        return pull;
    }
    
}
