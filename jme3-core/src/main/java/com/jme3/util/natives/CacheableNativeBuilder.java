package com.jme3.util.natives;

import com.jme3.util.cache.InlineTimedCache;
import org.lwjgl.system.MemoryStack;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class CacheableNativeBuilder<C, T extends C> {

    private static final Logger logger = Logger.getLogger(CacheableNativeBuilder.class.getName());

    protected final MemoryStack stack = MemoryStack.stackPush();
    protected InlineTimedCache<C> cache;

    public T build() {
        T obj = getBuildTarget();
        if (cache != null) {
            obj = cache.computeIfAbsent(obj, this::construct);
        } else {
            logger.log(Level.WARNING, "Building {0} without a cache.", getClass().getName());
            construct();
        }
        stack.pop();
        return obj;
    }

    protected abstract void construct();

    protected abstract T getBuildTarget();

    public void setCache(InlineTimedCache<C> cache) {
        this.cache = cache;
    }

    public InlineTimedCache<C> getCache() {
        return cache;
    }

}
