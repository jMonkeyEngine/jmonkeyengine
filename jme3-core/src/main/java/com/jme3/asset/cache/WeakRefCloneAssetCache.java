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
import com.jme3.asset.CloneableSmartAsset;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>WeakRefCloneAssetCache</code> caches cloneable assets in a weak-key
 * cache, allowing them to be collected when memory is low.
 * The cache stores weak references to the asset keys, so that
 * when all clones of the original asset are collected, will cause the 
 * asset to be automatically removed from the cache.
 * 
* @author Kirill Vainer
 */
public class WeakRefCloneAssetCache implements AssetCache {

    private static final Logger logger = Logger.getLogger(WeakRefAssetCache.class.getName());
    
    private final ReferenceQueue<AssetKey> refQueue = new ReferenceQueue<AssetKey>();
    
    /**
     * Maps cloned key to AssetRef which has a weak ref to the original 
     * key and a strong ref to the original asset.
     */
    private final ConcurrentHashMap<AssetKey, AssetRef> smartCache 
            = new ConcurrentHashMap<AssetKey, AssetRef>();
    
    /**
     * Stored in the ReferenceQueue to find out when originalKey is collected
     * by GC. Once collected, the clonedKey is used to remove the asset
     * from the cache.
     */
    private static final class KeyRef extends PhantomReference<AssetKey> {
        
        AssetKey clonedKey;
        
        public KeyRef(AssetKey originalKey, ReferenceQueue<AssetKey> refQueue) {
            super(originalKey, refQueue);
            clonedKey = originalKey.clone();
        }
    }
    
    /**
     * Stores the original key and original asset.
     * The asset info contains a cloneable asset (e.g. the original, from
     * which all clones are made). Also a weak reference to the 
     * original key which is used when the clones are produced.
     */
    private static final class AssetRef extends WeakReference<AssetKey> {

        CloneableSmartAsset asset;

        public AssetRef(CloneableSmartAsset originalAsset, AssetKey originalKey) {
            super(originalKey);
            this.asset = originalAsset;
        }
    }

    private final ThreadLocal<ArrayList<AssetKey>> assetLoadStack 
            = new ThreadLocal<ArrayList<AssetKey>>() {
        @Override
        protected ArrayList<AssetKey> initialValue() {
            return new ArrayList<AssetKey>();
        }
    };
    
    private void removeCollectedAssets(){
        int removedAssets = 0;
        for (KeyRef ref; (ref = (KeyRef)refQueue.poll()) != null;){
            // (Cannot use ref.get() since it was just collected by GC!)
            AssetKey key = ref.clonedKey;
            
            // Asset was collected, note that at this point the asset cache 
            // might not even have this asset anymore, it is OK.
            if (smartCache.remove(key) != null){
                removedAssets ++;
                //System.out.println("WeakRefAssetCache: The asset " + ref.assetKey + " was purged from the cache");
            }
        }
        if (removedAssets >= 1) {
            logger.log(Level.FINE, "WeakRefAssetCache: {0} assets were purged from the cache.", removedAssets);
        }
    }
    
    public <T> void addToCache(AssetKey<T> originalKey, T obj) {
        // Make room for new asset
        removeCollectedAssets();
        
        CloneableSmartAsset asset = (CloneableSmartAsset) obj;
        
        // No circular references, since the original asset is 
        // strongly referenced, we don't want the key strongly referenced.
        asset.setKey(null); 
        
        // Start tracking the collection of originalKey
        // (this adds the KeyRef to the ReferenceQueue)
        KeyRef ref = new KeyRef(originalKey, refQueue);
        
        // Place the asset in the cache, but use a clone of 
        // the original key.
        smartCache.put(ref.clonedKey, new AssetRef(asset, originalKey));
        
        // Push the original key used to load the asset
        // so that it can be set on the clone later
        ArrayList<AssetKey> loadStack = assetLoadStack.get();
        loadStack.add(originalKey);
    }

    public <T> void registerAssetClone(AssetKey<T> key, T clone) {
        ArrayList<AssetKey> loadStack = assetLoadStack.get();
        ((CloneableSmartAsset)clone).setKey(loadStack.remove(loadStack.size() - 1));
    }
    
    public void notifyNoAssetClone() {
        ArrayList<AssetKey> loadStack = assetLoadStack.get();
        loadStack.remove(loadStack.size() - 1);
    }

    public <T> T getFromCache(AssetKey<T> key) {
        AssetRef smartInfo = smartCache.get(key);
        if (smartInfo == null) {
            return null;
        } else {
            // NOTE: Optimization so that registerAssetClone()
            // can check this and determine that the asset clone
            // belongs to the asset retrieved here.
            AssetKey keyForTheClone = smartInfo.get();
            if (keyForTheClone == null){
                // The asset was JUST collected by GC
                // (between here and smartCache.get)
                return null;
            }
            
            // Prevent original key from getting collected
            // while an asset is loaded for it.
            ArrayList<AssetKey> loadStack = assetLoadStack.get();
            loadStack.add(keyForTheClone);
            
            return (T) smartInfo.asset;
        }
    }

    public boolean deleteFromCache(AssetKey key) {
        ArrayList<AssetKey> loadStack = assetLoadStack.get();
        
        if (!loadStack.isEmpty()){
            throw new UnsupportedOperationException("Cache cannot be modified"
                                                  + "while assets are being loaded");
        }
        
        return smartCache.remove(key) != null;
    }
    
    public void clearCache() {
        ArrayList<AssetKey> loadStack = assetLoadStack.get();
        
        if (!loadStack.isEmpty()){
            throw new UnsupportedOperationException("Cache cannot be modified"
                                                  + "while assets are being loaded");
        }
        
        smartCache.clear();
    }
}
