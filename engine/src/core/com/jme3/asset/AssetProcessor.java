/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.asset;

import com.jme3.material.Material;
import com.jme3.shader.Shader;

/**
 * <code>AssetProcessor</code> is used to apply processing to assets
 * after they have been loaded. They are assigned to a particular
 * asset type (which is represented by a {@link Class} and any assets
 * loaded that are of that class will be processed by the assigned
 * processor.
 * 
 * @author Kirill Vainer
 */
public interface AssetProcessor {
    /**
     * Applies post processing to an asset.
     * The method may return an object that is not the same
     * instance as the parameter object, and it could be from a different class.
     * 
     * @param obj The asset that was loaded from an {@link AssetLoader}.
     * @return Either the same object with processing applied, or an instance
     * of a new object.
     */
    public Object postProcess(AssetKey key, Object obj);
    
    /**
     * Creates a clone of the given asset.
     * If no clone is desired, then the same instance can be returned,
     * otherwise, a clone should be created.
     * For example, a clone of a {@link Material} should have its own set
     * of unique parameters that can be changed just for that instance,
     * but it may share certain other data if it sees fit (like the {@link Shader}).
     * 
     * @param obj The asset to clone
     * @return The cloned asset, or the same as the given argument if no
     * clone is needed.
     */
    public Object createClone(Object obj);
}
