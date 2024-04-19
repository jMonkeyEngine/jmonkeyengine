/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class ParameterManager {
    
    private final LinkedList<RenderParameter> parameters = new LinkedList<>();
    private final LinkedList<ParameterBinding> bindings = new LinkedList<>();
    
    public RenderParameter getParameter(String name, boolean failOnMiss) {
        for (RenderParameter p : parameters) {
            if (p.getParameterName().equals(name)) {
                return p;
            }
        }
        if (failOnMiss) {
            throw new NullPointerException("Input parameter does not exist: "+name);
        }
        return null;
    }
    
    public <T extends RenderParameter> T register(T param) {
        parameters.add(param);
        return param;
    }
    
    public void registerAll(RenderParameter... params) {
        for (RenderParameter p : params) {
            register(p);
        }
    }
    
    public void register(RenderParameterGroup group) {
        for (RenderParameter p : group.getRenderParameters()) {
            register(p);
        }
    }
    
    public ParameterBinding bindToOutput(String target, RenderParameter input) {
        return createBinding(getParameter(target, false), input);
    }
    
    public ParameterBinding bindToInput(String target, RenderParameter output) {
        return createBinding(output, getParameter(target, false));
    }
    
    public ParameterBinding bind(RenderParameter output, RenderParameter input) {
        return createBinding(output, input);
    }
    
    private ParameterBinding createBinding(RenderParameter output, RenderParameter input) {
        if (output == null || input == null) {
            return null;
        }
        ParameterBinding binding = new ParameterBinding(output, input);
        // remove previous bindings
        for (Iterator<ParameterBinding> it = bindings.iterator(); it.hasNext();) {
            if (it.next().isViolating(binding)) {
                it.remove();
            }
        }
        bindings.add(binding);
        return binding;
    }
    
    public void remove(RenderParameter param) {
        parameters.remove(param);
        for (Iterator<ParameterBinding> it = bindings.iterator(); it.hasNext();) {
            if (it.next().contains(param)) {
                it.remove();
            }
        }
    }
    
    public void remove(RenderParameterGroup group) {
        for (RenderParameter p : group.getRenderParameters()) {
            remove(p);
        }
    }
    
    public void pull(RenderParameter input) {
        for (ParameterBinding b : bindings) {
            if (b.isInput(input)) {
                b.applyOutputToInput();
                break;
            }
        }
    }
    
    public void pull(RenderParameterGroup group) {
        for (RenderParameter p : group.getRenderParameters()) {
            pull(p);
        }
    }
    
    public void push(RenderParameter output) {
        for (ParameterBinding b : bindings) {
            if (b.isOutput(output)) {
                b.applyOutputToInput();
            }
        }
    }
    
    public void push(RenderParameterGroup group) {
        for (RenderParameter p : group.getRenderParameters()) {
            push(p);
        }
    }
    
    public void clear() {
        parameters.clear();
        bindings.clear();
    }
    
}
