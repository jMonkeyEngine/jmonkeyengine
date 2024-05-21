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
