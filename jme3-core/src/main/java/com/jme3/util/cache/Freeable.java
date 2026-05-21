package com.jme3.util.cache;

public interface Freeable {

    void onCacheEviction(InlineTimedCache<?, ?> cache, Object key);

}
