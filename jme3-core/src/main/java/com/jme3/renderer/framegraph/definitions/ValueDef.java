/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.definitions;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * General resource definition implementation.
 * 
 * @author codex
 * @param <T>
 */
public class ValueDef <T> extends AbstractResourceDef<T> {

    private final Class<T> type;
    private Function<Object, T> builder;
    private Consumer<T> reviser;
    
    public ValueDef(Class<T> type, Function<Object, T> create) {
        this.type = type;
        this.builder = create;
    }
    
    @Override
    public T createResource() {
        return builder.apply(null);
    }
    @Override
    public T applyDirectResource(Object resource) {
        if (reviser != null && type.isAssignableFrom(resource.getClass())) {
            T res = (T)resource;
            reviser.accept(res);
            return res;
        }
        return null;
    }
    @Override
    public T applyIndirectResource(Object resource) {
        return null;
    }
    
    /**
     * Sets the builder function that constructs new objects.
     * 
     * @param builder 
     */
    public void setBuilder(Function<Object, T> builder) {
        this.builder = builder;
    }
    /**
     * Sets the consumer that alters objects for reallocation.
     * 
     * @param reviser 
     */
    public void setReviser(Consumer<T> reviser) {
        this.reviser = reviser;
    }
    
    /**
     * Gets the object type handled by this definition.
     * 
     * @return 
     */
    public Class<T> getType() {
        return type;
    }
    /**
     * 
     * @return 
     */
    public Function<Object, T> getBuilder() {
        return builder;
    }
    /**
     * 
     * @return 
     */
    public Consumer<T> getReviser() {
        return reviser;
    }
    
}
