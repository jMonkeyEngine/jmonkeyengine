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
package com.jme3.asset.cache;

import com.jme3.asset.AssetKey;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <code>SimpleAssetCache</code> is an asset cache
 * that caches assets without any automatic removal policy. The user
 * is expected to manually call {@link #deleteFromCache(com.jme3.asset.AssetKey) }
 * to delete any assets.
 * 
 * @author Kirill Vainer
 */
public class SimpleAssetCache implements AssetCache {

    private final ConcurrentHashMap<AssetKey, Object> keyToAssetMap = new ConcurrentHashMap<AssetKey, Object>();
    
    public <T> void addToCache(AssetKey<T> key, T obj) {
        keyToAssetMap.put(key, obj);
    }

    public <T> void registerAssetClone(AssetKey<T> key, T clone) {
    }

    public <T> T getFromCache(AssetKey<T> key) {
        return (T) keyToAssetMap.get(key);
    }

    public boolean deleteFromCache(AssetKey key) {
        return keyToAssetMap.remove(key) != null;
    }

    public void clearCache() {
        keyToAssetMap.clear();
    }

    public void notifyNoAssetClone() {
    }
    
}
