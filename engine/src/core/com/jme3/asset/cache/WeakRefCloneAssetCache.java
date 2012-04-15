package com.jme3.asset.cache;

import com.jme3.asset.AssetKey;
import com.jme3.asset.CloneableSmartAsset;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.WeakHashMap;

/**
 * <codeWeakRefCloneAssetCache</code> caches cloneable assets in a weak-key
 * cache, allowing them to be collected when memory is low.
 * The cache stores weak references to the asset keys, so that
 * when all clones of the original asset are collected, will cause the 
 * asset to be automatically removed from the cache.
 * 
* @author Kirill Vainer
 */
public class WeakRefCloneAssetCache implements AssetCache {

    private static final class SmartCachedAsset {

        WeakReference<AssetKey> key;
        CloneableSmartAsset asset;

        public SmartCachedAsset(CloneableSmartAsset originalAsset, AssetKey originalKey) {
            this.key = new WeakReference<AssetKey>(originalKey);
            this.asset = originalAsset;
        }
    }

    private final WeakHashMap<AssetKey, SmartCachedAsset> smartCache
            = new WeakHashMap<AssetKey, SmartCachedAsset>();
    
    private final ThreadLocal<ArrayDeque<AssetKey>> assetLoadStack 
            = new ThreadLocal<ArrayDeque<AssetKey>>() {
        @Override
        protected ArrayDeque<AssetKey> initialValue() {
            return new ArrayDeque<AssetKey>();
        }
    };
    
    public <T> void addToCache(AssetKey<T> key, T obj) {
        CloneableSmartAsset asset = (CloneableSmartAsset) obj;
        
        // No circular references, since the original asset is 
        // strongly referenced, we don't want the key strongly referenced.
        asset.setKey(null); 
        
        // Place the asset in the cache with a weak ref to the key.
        synchronized (smartCache) {
            smartCache.put(key, new SmartCachedAsset(asset, key));
        }
        
        // Push the original key used to load the asset
        // so that it can be set on the clone later
        ArrayDeque<AssetKey> loadStack = assetLoadStack.get();
        loadStack.push(key);
    }

    public <T> void registerAssetClone(AssetKey<T> key, T clone) {
        ArrayDeque<AssetKey> loadStack = assetLoadStack.get();
        ((CloneableSmartAsset)clone).setKey(loadStack.pop());
    }
    
    public void notifyNoAssetClone() {
        ArrayDeque<AssetKey> loadStack = assetLoadStack.get();
        loadStack.pop();
    }

    public <T> T getFromCache(AssetKey<T> key) {
        SmartCachedAsset smartInfo;
        synchronized (smartCache){
            smartInfo = smartCache.get(key);
        }
        
        if (smartInfo == null) {
            return null;
        } else {
            // NOTE: Optimization so that registerAssetClone()
            // can check this and determine that the asset clone
            // belongs to the asset retrieved here.
            AssetKey keyForTheClone = smartInfo.key.get();
            if (keyForTheClone == null){
                // The asset was JUST collected by GC
                // (between here and smartCache.get)
                return null;
            }
            
            // Prevent original key from getting collected
            // while an asset is loaded for it.
            ArrayDeque<AssetKey> loadStack = assetLoadStack.get();
            loadStack.push(keyForTheClone);
            
            return (T) smartInfo.asset;
        }
    }

    public boolean deleteFromCache(AssetKey key) {
        ArrayDeque<AssetKey> loadStack = assetLoadStack.get();
        
        if (!loadStack.isEmpty()){
            throw new UnsupportedOperationException("Cache cannot be modified"
                                                  + "while assets are being loaded");
        }
        synchronized (smartCache) {
            return smartCache.remove(key) != null;
        }
    }
    
    public void clearCache() {
        ArrayDeque<AssetKey> loadStack = assetLoadStack.get();
        
        if (!loadStack.isEmpty()){
            throw new UnsupportedOperationException("Cache cannot be modified"
                                                  + "while assets are being loaded");
        }
        synchronized (smartCache) {
            smartCache.clear();
        }
    }
}
