package com.jme3.asset.cache;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetProcessor;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

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
                //System.out.println("WeakRefAssetCache: The asset " + ref.assetKey + " was purged from the cache");
            }
        }
        if (removedAssets >= 1) {
//            System.out.println("WeakRefAssetCache: " + removedAssets + " assets were purged from the cache.");
        }
    }
    
    public <T> void addToCache(AssetKey<T> key, T obj) {
        removeCollectedAssets();
        
        // NOTE: Some thread issues can hapen if another
        // thread is loading an asset with the same key ..
        AssetRef ref = new AssetRef(key, obj, refQueue);
        assetCache.put(key, ref);
        
//        Texture t = (Texture) obj;
//        Image i = t.getImage();
//        System.out.println("add to cache "  + System.identityHashCode(i));
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
//        Texture t = (Texture) clone;
//        System.out.println("clonable asset " + System.identityHashCode(t.getImage()));
        //throw new UnsupportedOperationException("Cannot use this cache for cloneable assets");
    }
}
