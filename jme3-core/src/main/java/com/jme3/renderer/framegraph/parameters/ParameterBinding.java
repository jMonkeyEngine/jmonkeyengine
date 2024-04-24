/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.parameters;

import com.jme3.renderer.framegraph.parameters.RenderParameter;
import java.util.Objects;

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
    
    public void applyOutputToInput() {
        input.accept(output.produce());
    }
    
    public boolean isInput(RenderParameter input) {
        return this.input == input;
    }
    
    public boolean isOutput(RenderParameter output) {
        return this.output == output;
    }
    
    public boolean contains(RenderParameter p) {
        return input == p || output == p;
    }
    
    public boolean isViolating(ParameterBinding pb) {
        return input == pb.input;
    }
    
}
