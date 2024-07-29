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
package com.jme3.renderer.framegraph.modules;

import com.jme3.renderer.framegraph.modules.RenderModule;

/**
 * Locates a pass.
 * 
 * @author codex
 * @param <T>
 */
public interface ModuleLocator <T extends RenderModule> {
    
    /**
     * Determines if the module qualifies for this locator.
     * 
     * @param module
     * @return pass, or null if not accepted
     */
    public T accept(RenderModule module);
    
    /**
     * Locates a pass by its type.
     * 
     * @param <R>
     * @param type
     * @return 
     */
    public static <R extends RenderModule> ModuleLocator<R> by(Class<R> type) {
        return pass -> {
            if (type.isAssignableFrom(pass.getClass())) {
                return (R)pass;
            } else {
                return null;
            }
        };
    }
    
    /**
     * Locates a pass by its type and name.
     * 
     * @param <R>
     * @param type
     * @param name
     * @return 
     */
    public static <R extends RenderModule> ModuleLocator<R> by(Class<R> type, String name) {
        return pass -> {
            if (name.equals(pass.getName()) && type.isAssignableFrom(pass.getClass())) {
                return (R)pass;
            } else {
                return null;
            }
        };
    }
    
}
