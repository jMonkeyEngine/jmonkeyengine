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
public abstract class AbstractResourceDef <T> implements ResourceDef<T> {
    
    private Consumer<T> disposalMethod;
    private int staticTimeout = -1;
    private boolean useExisting = true;
    private boolean disposeOnRelease = false;
    
    @Override
    public int getStaticTimeout() {
        return staticTimeout;
    }
    @Override
    public Consumer<T> getDisposalMethod() {
        return disposalMethod;
    }
    @Override
    public boolean isUseExisting() {
        return useExisting;
    }
    @Override
    public boolean isDisposeOnRelease() {
        return disposeOnRelease;
    }
    
    /**
     * Sets the consumer responsible for disposing the resource.
     * <p>
     * default=null
     * 
     * @param disposalMethod disposal consumer, or null to use defaults
     */
    public void setDisposalMethod(Consumer<T> disposalMethod) {
        this.disposalMethod = disposalMethod;
    }
    /**
     * Sets the number of frames the resource can be static before being
     * disposed.
     * <p>
     * If less than zero, the default value will be used instead.
     * 
     * @param staticTimout 
     */
    public void setStaticTimeout(int staticTimout) {
        this.staticTimeout = staticTimout;
    }
    /**
     * Sets this definition to allow for use of reallocated objects.
     * <p>
     * default=true
     * 
     * @param useExisting 
     */
    public void setUseExisting(boolean useExisting) {
        this.useExisting = useExisting;
    }
    /**
     * Sets the resource to be disposed when it is unused.
     * <p>
     * default=false
     * 
     * @param disposeOnRelease 
     */
    public void setDisposeOnRelease(boolean disposeOnRelease) {
        this.disposeOnRelease = disposeOnRelease;
    }
    
}
