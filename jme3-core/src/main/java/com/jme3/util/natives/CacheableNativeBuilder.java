package com.jme3.util.natives;

import com.jme3.vulkan.pipeline.cache.Cache;
import org.lwjgl.system.MemoryStack;

public abstract class CacheableNativeBuilder<C, T extends C> {

    protected final MemoryStack stack = MemoryStack.stackPush();
    protected Cache<C> cache;

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

    public void setCache(Cache<C> cache) {
        this.cache = cache;
    }

    public Cache<C> getCache() {
        return cache;
    }

}
