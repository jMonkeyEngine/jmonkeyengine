package com.jme3.util.natives;

import com.jme3.vulkan.pipeline.cache.TheNewOneAndOnlyCache;
import org.lwjgl.system.MemoryStack;

public abstract class CacheableNativeBuilder<C, T extends C> {

    protected final MemoryStack stack = MemoryStack.stackPush();
    protected TheNewOneAndOnlyCache<C> cache;

    public T build() {
        T obj = getBuildTarget();
        if (cache != null) {
            obj = cache.allocate(obj, this::construct);
        } else {
            construct();
        }
        stack.pop();
        return obj;
    }

    protected abstract void construct();

    protected abstract T getBuildTarget();

    public void setCache(TheNewOneAndOnlyCache<C> cache) {
        this.cache = cache;
    }

    public TheNewOneAndOnlyCache<C> getCache() {
        return cache;
    }

}
