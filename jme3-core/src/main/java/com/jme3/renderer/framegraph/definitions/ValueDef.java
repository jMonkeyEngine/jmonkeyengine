/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.definitions;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 * @author codex
 * @param <T>
 */
public class ValueDef <T> implements ResourceDef<T> {

    private final Class<T> type;
    private Function<Object, T> create;
    private Consumer<T> reset;

    public ValueDef(Class<T> type, Function<Object, T> create) {
        this.type = type;
        this.create = create;
    }
    
    @Override
    public T createResource() {
        return create.apply(null);
    }
    @Override
    public T applyResource(Object resource) {
        if (reset != null && type.isAssignableFrom(resource.getClass())) {
            T res = (T)resource;
            reset.accept(res);
            return res;
        }
        return null;
    }

    public void setCreate(Function<Object, T> create) {
        this.create = create;
    }
    public void setReset(Consumer<T> reset) {
        this.reset = reset;
    }

    public Class<T> getType() {
        return type;
    }
    public Function<Object, T> getCreate() {
        return create;
    }
    public Consumer<T> getReset() {
        return reset;
    }
    
}
