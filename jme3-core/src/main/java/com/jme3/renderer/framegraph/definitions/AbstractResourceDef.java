/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    private boolean readConcurrent = true;
    
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
    
    @Override
    public boolean isReadConcurrent() {
        return readConcurrent;
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
    
    /**
     * Sets the resource as able to be read concurrently.
     * 
     * @param readConcurrent 
     */
    public void setReadConcurrent(boolean readConcurrent) {
        this.readConcurrent = readConcurrent;
    }
    
}
