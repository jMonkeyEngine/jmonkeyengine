/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.parameters;

import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class ParameterSpace {
    
    private final LinkedList<RenderParameter> parameters = new LinkedList<>();
    private final LinkedList<ParameterBinding> localBindings = new LinkedList<>();
    
    /**
     * Returns the named parameter, or null if none exists.
     * 
     * @param name
     * @param failOnMiss
     * @return named parameter
     * @throws NullPointerException if the named parameter does not exist and failOnMiss is true
     */
    public RenderParameter getParameter(String name, boolean failOnMiss) {
        for (RenderParameter p : parameters) {
            if (p.isPubliclyNamed(name)) {
                return p;
            }
        }
        if (failOnMiss) {
            throw new NullPointerException("Input parameter \""+name+"\" does not exist or is private.");
        }
        return null;
    }
    
    /**
     * Returns the named parameter of the given type, or null if none exists.
     * 
     * @param <T>
     * @param name
     * @param type
     * @param failOnMiss
     * @return named parameter
     * @throws NullPointerException if the named parameter does not exist and failOnMiss is true
     */
    public <T extends RenderParameter> T getParameter(String name, Class<T> type, boolean failOnMiss) {
        for (RenderParameter p : parameters) {
            if (p.isPubliclyNamed(name) && type.isAssignableFrom(p.getClass())) {
                return (T)p;
            }
        }
        if (failOnMiss) {
            throw new NullPointerException("Input parameter \""+name+"\" does not exist, or is private, "
                    + "or is not of type "+type.getName()+".");
        }
        return null;
    }
    
    /**
     * Registers and returns the parameter.
     * 
     * @param <T>
     * @param param
     * @return 
     */
    public <T extends RenderParameter> T register(T param) {
        parameters.add(param);
        return param;
    }
    
    /**
     * Registers all given parameters.
     * 
     * @param params 
     */
    public void registerAll(RenderParameter... params) {
        for (RenderParameter p : params) {
            register(p);
        }
    }
    
    /**
     * Registers all parameters in the group.
     * 
     * @param group 
     */
    public void register(RenderParameterGroup group) {
        for (RenderParameter p : group.getRenderParameters()) {
            register(p);
        }
    }
    
    /**
     * Removes the parameter from this space and breaks all related bindings.
     * 
     * @param param 
     */
    public void remove(RenderParameter param) {
        parameters.remove(param);
        for (Iterator<ParameterBinding> it = localBindings.iterator(); it.hasNext();) {
            if (it.next().contains(param)) {
                it.remove();
            }
        }
    }
    
    /**
     * Removes all parameters in the group from this space.
     * 
     * @param group 
     * @see #remove(com.jme3.renderer.framegraph.parameters.RenderParameter)
     */
    public void remove(RenderParameterGroup group) {
        for (RenderParameter p : group.getRenderParameters()) {
            remove(p);
        }
    }
    
    /**
     * Binds the named parameter and the input parameter locally, with the named parameter
     * as the output, and returns the resulting binding.
     * <p>
     * The input parameter should already be registered with this space.
     * 
     * @param target
     * @param input
     * @return 
     */
    public ParameterBinding bindToOutput(String target, RenderParameter input) {
        return createBinding(getParameter(target, false), input);
    }
    
    /**
     * Binds the named parameter and the output parameter locally, with the named parameter
     * as the input, and returns the resulting binding.
     * <p>
     * The output parameter should already be registered with this space.
     * 
     * @param target
     * @param output
     * @return 
     */
    public ParameterBinding bindToInput(String target, RenderParameter output) {
        return createBinding(output, getParameter(target, false));
    }
    
    /**
     * Binds the two parameters and returns the resulting binding.
     * <p>
     * Both parameters should already be registered with this space.
     * 
     * @param output
     * @param input
     * @return 
     */
    public ParameterBinding bind(RenderParameter output, RenderParameter input) {
        return createBinding(output, input);
    }
    
    /**
     * Registers the binding with this space.
     * 
     * @param binding
     * @return 
     */
    public ParameterBinding registerBinding(ParameterBinding binding) {
        breakViolatingBindings(binding);
        localBindings.add(binding);
        return binding;
    }
    
    private ParameterBinding createBinding(RenderParameter output, RenderParameter input) {
        if (output == null || input == null) {
            return null;
        }
        ParameterBinding binding = new ParameterBinding(output, input);
        breakViolatingBindings(binding);
        localBindings.add(binding);
        return binding;
    }
    
    private void breakViolatingBindings(ParameterBinding binding) {
        for (Iterator<ParameterBinding> it = localBindings.iterator(); it.hasNext();) {
            if (it.next().isViolating(binding)) {
                it.remove();
            }
        }
    }
    
    /**
     * Pulls values from parameters bound as output to the given parameter.
     * 
     * @param input 
     */
    public void pull(RenderParameter input) {
        for (ParameterBinding b : localBindings) {
            if (b.isInput(input)) {
                b.applyOutputToInput();
                break;
            }
        }
    }
    
    /**
     * Pulls values from parameters bound as output to parameters in the given group.
     * 
     * @param group 
     */
    public void pull(RenderParameterGroup group) {
        for (RenderParameter p : group.getRenderParameters()) {
            pull(p);
        }
    }
    
    /**
     * Pushes values to parameters bound as input to the given parameter.
     * 
     * @param output 
     */
    public void push(RenderParameter output) {
        for (ParameterBinding b : localBindings) {
            if (b.isOutput(output)) {
                b.applyOutputToInput();
            }
        }
    }
    
    /**
     * Pushes values to parameters bound as input to parameters in the given group.
     * 
     * @param group 
     */
    public void push(RenderParameterGroup group) {
        for (RenderParameter p : group.getRenderParameters()) {
            push(p);
        }
    }
    
    /**
     * Clears this space of all parameters and bindings.
     */
    public void clear() {
        parameters.clear();
        localBindings.clear();
    }
    
    /**
     * Returns the list of parameters.
     * <p>
     * Do not modify the returned list.
     * 
     * @return 
     */
    public LinkedList<RenderParameter> getParameterList() {
        return parameters;
    }
    
}
