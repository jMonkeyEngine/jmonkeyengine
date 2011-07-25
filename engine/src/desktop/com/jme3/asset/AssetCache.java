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

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * An <code>AssetCache</code> allows storage of loaded resources in order
 * to improve their access time if they are requested again in a short period
 * of time. The AssetCache stores weak references to the resources, allowing
 * Java's garbage collector to request deletion of rarely used resources
 * when heap memory is low.
 */
public class AssetCache {

    public static final class SmartAssetInfo {
        public WeakReference<AssetKey> smartKey;
        public Asset asset;
    }

    private final WeakHashMap<AssetKey, SmartAssetInfo> smartCache
            = new WeakHashMap<AssetKey, SmartAssetInfo>();
    private final HashMap<AssetKey, Object> regularCache = new HashMap<AssetKey, Object>();

    /**
     * Adds a resource to the cache.
     * <br/><br/>
     * <font color="red">Thread-safe.</font>
     * @see #getFromCache(java.lang.String)
     */
    public void addToCache(AssetKey key, Object obj){
        synchronized (regularCache){
            if (obj instanceof Asset && key.useSmartCache()){
                // put in smart cache
                Asset asset = (Asset) obj;
                asset.setKey(null); // no circular references
                SmartAssetInfo smartInfo = new SmartAssetInfo();
                smartInfo.asset = asset;
                // use the original key as smart key
                smartInfo.smartKey = new WeakReference<AssetKey>(key); 
                smartCache.put(key, smartInfo);
            }else{
                // put in regular cache
                regularCache.put(key, obj);
            }
        }
    }

    /**
     * Delete an asset from the cache, returns true if it was deleted successfuly.
     * <br/><br/>
     * <font color="red">Thread-safe.</font>
     */
    public boolean deleteFromCache(AssetKey key){
        synchronized (regularCache){
            if (key.useSmartCache()){
                return smartCache.remove(key) != null;
            }else{
                return regularCache.remove(key) != null;
            }
        }
    }

    /**
     * Gets an object from the cache given an asset key.
     * <br/><br/>
     * <font color="red">Thread-safe.</font>
     * @param key
     * @return
     */
    public Object getFromCache(AssetKey key){
        synchronized (regularCache){
            if (key.useSmartCache()) {
                return smartCache.get(key).asset;
            } else {
                return regularCache.get(key);
            }
        }
    }

    /**
     * Retrieves smart asset info from the cache.
     * @param key
     * @return
     */
    public SmartAssetInfo getFromSmartCache(AssetKey key){
        return smartCache.get(key);
    }

    /**
     * Deletes all the assets in the regular cache.
     */
    public void deleteAllAssets(){
        synchronized (regularCache){
            regularCache.clear();
            smartCache.clear();
        }
    }
}
