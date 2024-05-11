/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.definitions;

import java.util.function.Consumer;

/**
 *
 * @author codex
 * @param <T>
 */
public interface ResourceDef <T> {
    
    /**
     * Creates a new resources from scratch.
     * 
     * @return 
     */
    public T createResource();
    
    /**
     * Repurposes the given resource.
     * 
     * @param resource
     * @return repurposed resource, or null if the given resource is not usable.
     */
    public T applyResource(Object resource);
    
    /**
     * Returns the number of frames which the resource must be
     * static (unused throughout rendering) before it is disposed.
     * <p>
     * If negative, the default timeout value will be used instead.
     * 
     * @return static timeout duration
     */
    public default int getStaticTimeout() {
        return -1;
    }
    
    /**
     * Gets the Consumer used to dispose of a resource.
     * 
     * @return resource disposer, or null
     */
    public default Consumer<T> getDisposalMethod() {
        return null;
    }
    
    /**
     * Returns true if resources can be reallocated to this definition.
     * 
     * @return 
     */
    public default boolean isUseExisting() {
        return true;
    }
    
    /**
     * Returns true if the resource should be disposed after being
     * released and having no users.
     * 
     * @return 
     */
    public default boolean isDisposeOnRelease() {
        return false;
    }
    
}
