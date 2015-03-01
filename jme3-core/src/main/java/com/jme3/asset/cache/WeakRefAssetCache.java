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
import com.jme3.asset.AssetProcessor;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A garbage collector bound asset cache that handles non-clonable objects.
 * This cache assumes that the asset given to the user is the same asset
 * that has been stored in the cache, in other words, 
 * {@link AssetProcessor#createClone(java.lang.Object) } for that asset
 * returns the same object as the argument.
 * This implementation will remove the asset from the cache 
 * once the asset is no longer referenced in user code and memory is low,
 * e.g. the VM feels like purging the weak references for that asset.
 * 
 * @author Kirill Vainer
 */
public class WeakRefAssetCache implements AssetCache {

    private static final Logger logger = Logger.getLogger(WeakRefAssetCache.class.getName());
    
    private final ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>();
    
    private final ConcurrentHashMap<AssetKey, AssetRef> assetCache 
            = new ConcurrentHashMap<AssetKey, AssetRef>();

    private static class AssetRef extends WeakReference<Object> {
        
        private final AssetKey assetKey;
        
        public AssetRef(AssetKey assetKey, Object originalAsset, ReferenceQueue<Object> refQueue){
            super(originalAsset, refQueue);
            this.assetKey = assetKey;
        }
    }
    
    private void removeCollectedAssets(){
        int removedAssets = 0;
        for (AssetRef ref; (ref = (AssetRef)refQueue.poll()) != null;){
            // Asset was collected, note that at this point the asset cache 
            // might not even have this asset anymore, it is OK.
            if (assetCache.remove(ref.assetKey) != null){
                removedAssets ++;
            }
        }
        if (removedAssets >= 1) {
            logger.log(Level.FINE, "WeakRefAssetCache: {0} assets were purged from the cache.", removedAssets);
        }
    }
    
    public <T> void addToCache(AssetKey<T> key, T obj) {
        removeCollectedAssets();
        
        // NOTE: Some thread issues can hapen if another
        // thread is loading an asset with the same key ..
        AssetRef ref = new AssetRef(key, obj, refQueue);
        assetCache.put(key, ref);
    }

    public <T> T getFromCache(AssetKey<T> key) {
        AssetRef ref = assetCache.get(key);
        if (ref != null){
            return (T) ref.get();
        }else{
            return null;
        }
    }

    public boolean deleteFromCache(AssetKey key) {
        return assetCache.remove(key) != null;
    }

    public void clearCache() {
        assetCache.clear();
    }
    
    public <T> void registerAssetClone(AssetKey<T> key, T clone) {
    }
    
    public void notifyNoAssetClone() {
    }
}
