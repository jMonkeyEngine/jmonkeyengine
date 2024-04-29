/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author codex
 * @param <T>
 */
public abstract class ResourceDef <T> {
    
    private final Class<T> resType;

    public ResourceDef(Class<T> resType) {
        this.resType = resType;
    }
    
    /**
     * Creates and returns a new resource.
     * 
     * @return 
     */
    public abstract T create();
    
    /**
     * Returns true if this definition accepts the reallocation of the given
     * resource to the {@link RenderResource} this is assigned to.
     * 
     * @param resource
     * @return 
     */
    public abstract boolean acceptReallocationOf(T resource);
    
    /**
     * Returns true if this definition's resource type
     * {@link Class#isAssignableFrom(java.lang.Class) is assignable from} the
     * given class.
     * 
     * @param type
     * @return 
     */
    public boolean isOfResourceType(Class type) {
        return resType.isAssignableFrom(type);
    }
    
    /**
     * Returns true if the {@link RenderResource} this is assigned to can accept
     * reallocated resources.
     * 
     * @return 
     */
    public boolean isAcceptsReallocated() {
        return true;
    }
    
    /**
     * If true, the existing resource contained in the {@link RenderResource}
     * this is assigned to will be reallocated to another RenderResource, if possible.
     * <p>
     * Reallocation only occurs after all users have released the RenderResource.
     * 
     * @return 
     */
    public boolean isReallocatable() {
        return true;
    }
    
    /**
     * If true, the existing resource contained in the {@link RenderResource}
     * this is assigned to be destroyed during rendering cleanup.
     * 
     * @return 
     */
    public boolean isTransient() {
        return false;
    }
    
    /**
     * Returns true if the {@link RenderResource} this is assigned to should be
     * discarded when it becomes unused.
     * <p>
     * This overrides {@link #isReallocatable()} and {@link #isTransient()}, as the
     * resource will not survive to do either.
     * 
     * @return 
     */
    public boolean isDiscardIfUnused() {
        return false;
    }
    
}
