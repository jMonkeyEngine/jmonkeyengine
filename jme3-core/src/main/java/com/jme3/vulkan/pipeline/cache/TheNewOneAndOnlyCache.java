package com.jme3.vulkan.pipeline.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TheNewOneAndOnlyCache <E> {

    private final Map<E, Entry<E>> entries = new ConcurrentHashMap<>();
    private long idleTimeout = 2000;

    public <T extends E> T allocate(T element, Runnable build) {
        Entry e = entries.get(element);
        if (e != null) {
            return (T)e.acquire();
        }
        entries.put(element, new Entry<>(element));
        build.run();
        return element;
    }

    public void flush() {
        long current = System.currentTimeMillis();
        entries.values().removeIf(e -> e.flush(current, idleTimeout));
    }

    public void clear() {
        entries.clear();
    }

    public void setIdleTimeout(long idleTimeout) {
        assert idleTimeout >= 0;
        this.idleTimeout = idleTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    private static class Entry <E> {

        private final E element;
        private long lastUsed = System.currentTimeMillis();

        private Entry(E element) {
            this.element = element;
        }

        public E acquire() {
            lastUsed = System.currentTimeMillis();
            return element;
        }

        public boolean flush(long current, long timeout) {
            return current - lastUsed >= timeout;
        }

    }

}
