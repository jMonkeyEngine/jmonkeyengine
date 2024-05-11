/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.HashMap;

/**
 *
 * @author codex
 */
public class RenderingBlackboard {
    
    private final HashMap<String, Parameter> parameters = new HashMap<>();
    
    public <T> void set(String name, Class<T> type, T value) {
        if (type == null) {
            throw new NullPointerException("Parameter type cannot be null.");
        }
        if (name == null) {
            return;
        }
        if (value == null) {
            delete(name);
            return;
        }
        Parameter param = parameters.get(name);
        if (param != null) {
            param.setValue(value);
        } else {
            parameters.put(name, new Parameter(type, value));
        }
    }
    public <T> T get(String name, Class<T> type) {
        if (name == null) {
            return null;
        }
        Parameter param = parameters.get(name);
        if (param == null || !type.isAssignableFrom(param.type)) {
            return null;
        }
        return (T)param.value;
    }
    
    public boolean delete(String name) {
        return name != null && parameters.remove(name) != null;
    }
    public void clear() {
        parameters.clear();
    }
    
    private static class Parameter <T> {
        
        private final Class<T> type;
        private T value;

        public Parameter(Class<T> type, T value) {
            this.type = type;
            this.value = value;
        }
        
        public void setValue(Object val) {
            if (type.isAssignableFrom(val.getClass())) {
                value = (T)val;
            } else {
                throw new IllegalArgumentException("Value of type "+val.getClass()+" replaces "
                        + "another value of a different type.");
            }
        }
        
    }
    
}
