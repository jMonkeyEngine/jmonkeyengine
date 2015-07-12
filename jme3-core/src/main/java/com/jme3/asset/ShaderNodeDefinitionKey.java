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

import com.jme3.asset.cache.AssetCache;
import com.jme3.shader.ShaderNodeDefinition;
import java.util.List;

/**
 * Used for loading {@link ShaderNodeDefinition shader nodes definition}
 *
 * Tells if the defintion has to be loaded with or without its documentation
 */
public class ShaderNodeDefinitionKey extends AssetKey<List<ShaderNodeDefinition>> {

    private boolean loadDocumentation = false;

    /**
     * creates a ShaderNodeDefinitionKey
     *
     * @param name the name of the asset to load
     */
    public ShaderNodeDefinitionKey(String name) {
        super(name);
    }

    /**
     * creates a ShaderNodeDefinitionKey
     */
    public ShaderNodeDefinitionKey() {
        super();
    }

    @Override
    public Class<? extends AssetCache> getCacheType() {
        return null;
    }

    /**
     *
     * @return true if the asset loaded with this key will contain its
     * documentation
     */
    public boolean isLoadDocumentation() {
        return loadDocumentation;
    }

    /**
     * sets to true to load the documentation along with the
     * ShaderNodeDefinition
     *
     * @param loadDocumentation true to load the documentation along with the
     * ShaderNodeDefinition
     */
    public void setLoadDocumentation(boolean loadDocumentation) {
        this.loadDocumentation = loadDocumentation;
    }
}
