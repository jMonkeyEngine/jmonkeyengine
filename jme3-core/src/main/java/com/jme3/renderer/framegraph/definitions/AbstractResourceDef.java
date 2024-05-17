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
    private boolean survivesReferenceCull = false;
    
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

    public void setDisposalMethod(Consumer<T> disposalMethod) {
        this.disposalMethod = disposalMethod;
    }
    public void setStaticTimeout(int staticTimout) {
        this.staticTimeout = staticTimout;
    }
    public void setUseExisting(boolean useExisting) {
        this.useExisting = useExisting;
    }
    public void setDisposeOnRelease(boolean disposeOnRelease) {
        this.disposeOnRelease = disposeOnRelease;
    }
    
}
