/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.function.Function;

/**
 *
 * @author codex
 * @param <T>
 */
public abstract class ValueDef <T> extends ResourceDef<T> {

    public ValueDef() {
        super(null);
    }

    @Override
    public boolean applyRecycled(T resource) {
        return false;
    }
    
    @Override
    public boolean isRecycleable() {
        return false;
    }
    
    public static <T> ValueDef<T> create(Function<Object, T> func) {
        return new ValueDefImpl(func);
    }
    
    private static class ValueDefImpl <T> extends ValueDef<T> {
        
        private final Function<Object, T> func;

        public ValueDefImpl(Function<Object, T> func) {
            this.func = func;
        }
        
        @Override
        public T create() {
            return func.apply(null);
        }
        
    }
    
}
