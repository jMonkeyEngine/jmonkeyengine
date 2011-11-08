/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import java.io.InputStream;

/**
 * The result of locating an asset through an AssetKey. Provides
 * a means to read the asset data through an InputStream.
 *
 * @author Kirill Vainer
 */
public abstract class AssetInfo {

    protected AssetManager manager;
    protected AssetKey key;

    public AssetInfo(AssetManager manager, AssetKey key) {
        this.manager = manager;
        this.key = key;
    }

    public AssetKey getKey() {
        return key;
    }

    public AssetManager getManager() {
        return manager;
    }

    @Override
    public String toString(){
        return getClass().getName() + "[" + "key=" + key + "]";
    }

    /**
     * Implementations of this method should return an {@link InputStream}
     * allowing access to the data represented by the {@link AssetKey}.
     * <p>
     * Each invocation of this method should return a new stream to the
     * asset data, starting at the beginning of the file.
     * 
     * @return The asset data.
     */
    public abstract InputStream openStream();

}
