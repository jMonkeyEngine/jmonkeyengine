package com.jme3.vulkan.allocate;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class StaticAllocator <T> {

    private final Collection<CacheEntry<? extends T>> cache = new ConcurrentLinkedQueue<>();

    public ResourceWrapper<? extends T> allocate(ResourceTicket<T> ticket) {
        ResourceWrapper<? extends T> selected = null;
        float eval = Float.POSITIVE_INFINITY;
        for (CacheEntry<? extends T> e : cache) {
            Float s = ticket.selectResource(e.get());
            if (s != null && s < eval && e.acquire()) {
                if (selected != null) {
                    selected.release();
                }
                if (s <= 0f) {
                    return e;
                }
                selected = e;
                eval = s;
            }
        }
        if (selected != null) {
            return selected;
        }
        CacheEntry e = new CacheEntry(ticket.createResource());
        if (!e.acquire()) {
            throw new IllegalStateException("New entry cannot reject initial acquire.");
        }
        cache.add(e);
        return e;
    }

    public <E extends T> ResourceWrapper<E> submit(E resource) {
        CacheEntry<E> e = new CacheEntry<>(resource);
        e.acquire();
        cache.add(e);
        return e;
    }

    public void flush(long idleMillis) {
        long time = System.currentTimeMillis();
        cache.removeIf(e -> e.free(time, idleMillis));
    }

    public void clear() {
        cache.clear();
    }

    private static class CacheEntry <T> implements ResourceWrapper<T> {

        private final T buffer;
        private final AtomicBoolean allocated = new AtomicBoolean(false);
        private long lastUsed = System.currentTimeMillis();

        private CacheEntry(T buffer) {
            this.buffer = buffer;
        }

        @Override
        public T get() {
            return buffer;
        }

        @Override
        public void release() {
            if (!allocated.getAndSet(false)) {
                throw new IllegalStateException("Buffer has not been acquired.");
            }
            lastUsed = System.currentTimeMillis();
        }

        public boolean acquire() {
            return !allocated.getAndSet(true);
        }

        public boolean free(long time, long idle) {
            return Math.abs(time - lastUsed) >= idle && acquire();
        }

    }

}
