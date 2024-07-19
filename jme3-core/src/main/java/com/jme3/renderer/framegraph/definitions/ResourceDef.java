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
import com.jme3.renderer.framegraph.ResourceView;

/**
 * Manages the behavior of a {@link ResourceView}, especially for creation,
 * reallocation, and disposal of related raw resources.
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
     * Checks if the resource can be directly allocated to this definition's
     * {@link ResourceView} as is.
     * <p>
     * For reallocation, direct "as is" resources are preferred over indirect
     * resources. Usually because direct resources are specifically designed
     * for whatever task.
     * 
     * @param resource
     * @return the resource if approved, otherwise null
     */
    public T applyDirectResource(Object resource);
    
    /**
     * Repurposes the given resource for allocation to this definition's
     * {@link ResourceView}.
     * <p>
     * An indirect resource usually does not exactly match the type of this
     * definition, but does contain the necessary components.
     * 
     * @param resource
     * @return repurposed resource, or null if the given resource is not usable.
     */
    public T applyIndirectResource(Object resource);
    
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
    
    /**
     * Returns true if the resource can be read concurrently.
     * 
     * @return 
     */
    public default boolean isReadConcurrent() {
        return true;
    }
    
    /**
     * Disposes the resource using the disposal method, if not null.
     * 
     * @param resource 
     */
    public default void dispose(T resource) {
        Consumer<T> d = getDisposalMethod();
        if (d != null) d.accept(resource);
    }
    
}
