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
public class ParameterBinding <T> {
    
    private final RenderParameter<T> output, input;

    public ParameterBinding(RenderParameter<T> output, RenderParameter<T> input) {
        this.output = output;
        this.input = input;
    }
    
    /**
     * Applies the output parameter to the input parameter.
     */
    public void applyOutputToInput() {
        input.set(output.get());
    }
    
    /**
     * Returns true if the given parameter is the input parameter of this binding.
     * 
     * @param input
     * @return 
     */
    public boolean isInput(RenderParameter input) {
        return this.input == input;
    }
    
    /**
     * Returns true if the given parameter is the output parameter of this binding.
     * 
     * @param output
     * @return 
     */
    public boolean isOutput(RenderParameter output) {
        return this.output == output;
    }
    
    /**
     * Returns true if the given parameter is a member of this binding.
     * 
     * @param param
     * @return 
     */
    public boolean contains(RenderParameter param) {
        return input == param || output == param;
    }
    
    /**
     * Returns true if the named parameter is a member of this binding.
     * 
     * @param name
     * @return 
     */
    public boolean containsNamed(String name) {
        return input.isPubliclyNamed(name) || output.isPubliclyNamed(name);
    }
    
    /**
     * Returns true if this binding is violating the given binding.
     * 
     * @param binding
     * @return 
     */
    public boolean isViolating(ParameterBinding binding) {
        return input == binding.input;
    }
    
}
