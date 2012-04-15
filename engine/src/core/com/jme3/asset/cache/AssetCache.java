package com.jme3.asset.cache;

import com.jme3.asset.AssetKey;

/**
 * <code>AssetCache</code> is an interface for asset caches. 
 * Allowing storage of loaded resources in order to improve their access time 
 * if they are requested again in a short period of time.
 * Depending on the asset type and how it is used, a specialized 
 * caching method can be selected that is most appropriate for that asset type.
 * The asset cache must be thread safe.
 * <p>
 * Some caches are used to manage cloneable assets, which track reachability
 * based on a shared key in all instances exposed in user code. 
 * E.g. {@link WeakRefCloneAssetCache} uses this approach.
 * For those particular caches, either {@link #registerAssetClone(com.jme3.asset.AssetKey, java.lang.Object) }
 * or {@link #notifyNoAssetClone() } <b>MUST</b> be called to avoid memory 
 * leaking following a successful {@link #addToCache(com.jme3.asset.AssetKey, java.lang.Object) }
 * or {@link #getFromCache(com.jme3.asset.AssetKey) } call!
 * 
 * @author Kirill Vainer
 */
public interface AssetCache {
    /**
     * Adds an asset to the cache.
     * Once added, it should be possible to retrieve the asset
     * by using the {@link #getFromCache(com.jme3.asset.AssetKey) } method.
     * However the caching criteria may at some point choose that the asset
     * should be removed from the cache to save memory, in that case, 
     * {@link #getFromCache(com.jme3.asset.AssetKey) } will return null.
     * <p><font color="red">Thread-Safe</font>
     * 
     * @param <T> The type of the asset to cache.
     * @param key The asset key that can be used to look up the asset.
     * @param obj The asset data to cache.
     */
    public <T> void addToCache(AssetKey<T> key, T obj);
    
    /**
     * This should be called by the asset manager when it has successfully
     * acquired a cached asset (with {@link #getFromCache(com.jme3.asset.AssetKey) })
     * and cloned it for use. 
     * <p><font color="red">Thread-Safe</font>
     * 
     * @param <T> The type of the asset to register.
     * @param key The asset key of the loaded asset (used to retrieve from cache)
     * @param clone The <strong>clone</strong> of the asset retrieved from
     * the cache.
     */
    public <T> void registerAssetClone(AssetKey<T> key, T clone);
    
    /**
     * Notifies the cache that even though the methods {@link #addToCache(com.jme3.asset.AssetKey, java.lang.Object) }
     * or {@link #getFromCache(com.jme3.asset.AssetKey) } were used, there won't
     * be a call to {@link #registerAssetClone(com.jme3.asset.AssetKey, java.lang.Object) }
     * for some reason. For example, if an error occurred during loading
     * or if the addToCache/getFromCache were used from user code.
     */
    public void notifyNoAssetClone();
    
    /**
     * Retrieves an asset from the cache.
     * It is possible to add an asset to the cache using
     * {@link #addToCache(com.jme3.asset.AssetKey, java.lang.Object) }. 
     * The asset may be removed from the cache automatically even if
     * it was added previously, in that case, this method will return null.
     * <p><font color="red">Thread-Safe</font>
     * 
     * @param <T> The type of the asset to retrieve
     * @param key The key used to lookup the asset.
     * @return The asset that was previously cached, or null if not found.
     */
    public <T> T getFromCache(AssetKey<T> key);
    
    /**
     * Deletes an asset from the cache.
     * <p><font color="red">Thread-Safe</font>
     * 
     * @param key The asset key to find the asset to delete.
     * @return True if the asset was successfully found in the cache
     * and removed.
     */
    public boolean deleteFromCache(AssetKey key);
    
    /**
     * Deletes all assets from the cache.
     * <p><font color="red">Thread-Safe</font>
     */
    public void clearCache();
}
