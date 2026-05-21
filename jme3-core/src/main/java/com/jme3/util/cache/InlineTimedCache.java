package com.jme3.util.cache;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InlineTimedCache<K, E> implements Cache<K, E> {

    private final Map<K, Entry<K, E>> entries;
    private final Queue<Entry<K, E>> evictionQueue = new ArrayDeque<>();
    private long duration;

    public InlineTimedCache(long durationMillis) {
        this(new HashMap<>(), durationMillis);
    }

    protected InlineTimedCache(Map<K, Entry<K, E>> entryMap, long durationMillis) {
        this.entries = entryMap;
        this.duration = durationMillis;
    }

    public static <K, E> InlineTimedCache<K, E> identityCache(long durationMillis) {
        return new InlineTimedCache<>(new IdentityHashMap<>(), durationMillis);
    }

    @Override
    public E put(K key, E value) {
        Entry<K, E> e = new Entry<>(key, value, duration);
        evictionQueue.add(e);
        Entry<K, E> prev = entries.put(key, e);
        if (prev != null) {
            prev.evict(this);
            return prev.getValue();
        }
        return null;
    }

    @Override
    public E get(Object key) {
        Entry<K, E> e = entries.get(key);
        if (e != null) {
            e.refresh(duration);
            evictionQueue.add(e);
            e.duplicated++;
        }
        evict();
        return e.getValue();
    }

    @Override
    public E computeIfAbsent(K key, Function<? super K, ? extends E> compute) {
        E e = get(key);
        if (e == null) {
            Entry<K, E> newEntry = new Entry<>(key, compute.apply(key), duration);
            evictionQueue.add(newEntry);
            entries.put(key, newEntry);
            e = newEntry.getValue();
        }
        return e;
    }

    @Override
    public void clear() {
        for (Entry<K, E> e : evictionQueue) {
            e.evict(this);
        }
        evictionQueue.clear();
        entries.clear();
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return entries.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        for (Entry<K, E> e : evictionQueue) {
            if (e.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public E remove(Object key) {
        Entry<K, E> rem = entries.remove(key);
        if (rem != null) {
            rem.evict(this);
        }
        return rem.getValue();
    }

    @Override
    public void putAll(Map<? extends K, ? extends E> m) {
        for (Map.Entry<? extends K, ? extends E> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public Set<K> keySet() {
        return entries.keySet();
    }

    @Override
    public Collection<E> values() {
        return entries.values().stream().map(Entry::getValue).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Set<Map.Entry<K, E>> entrySet() {
        return new HashSet<>(entries.values());
    }

    /**
     * Evicts expired values from this cache until an unexpired value is encountered.
     */
    public void evict() {
        long time = System.currentTimeMillis();
        while (!evictionQueue.isEmpty()) {
            if (evictionQueue.peek().expired(time)) {
                Entry<K, E> e = evictionQueue.poll();
                if (e.duplicated <= 0 && !e.dead) {
                    entries.remove(e.getKey());
                    e.evict(this);
                } else {
                    e.duplicated--;
                }
            } else break;
        }
    }

    /**
     * Evicts all expired values from this cache.
     */
    @Override
    public void flush() {
        long time = System.currentTimeMillis();
        for (int i = 0, l = evictionQueue.size(); i < l; i++) {
            Entry<K, E> e = evictionQueue.poll();
            if (!e.expired(time)) {
                evictionQueue.add(e);
            }
            if (e.duplicated-- == 0) {
                e.evict(this);
            }
        }
    }

    /**
     * Sets the duration in milliseconds since the last interaction that
     * a value is considered expired.
     *
     * @param durationMillis duration in milliseconds
     */
    public void setDuration(long durationMillis) {
        this.duration = durationMillis;
    }

    /**
     * Gets the duration in milliseconds.
     *
     * @return duration
     * @see #setDuration(long)
     */
    public long getDuration() {
        return duration;
    }

    protected static class Entry <K, E> implements Map.Entry<K, E> {

        private final K key;
        private E value;
        private long evictionTime;
        private int duplicated = 0;
        private boolean dead = false;

        protected Entry(K key, E value, long duration) {
            this.key = key;
            this.value = value;
            this.evictionTime = System.currentTimeMillis() + duration;
        }

        public void refresh(long duration) {
            evictionTime = System.currentTimeMillis() + duration;
        }

        public boolean expired(long time) {
            return time >= evictionTime || duplicated > 0 || dead;
        }

        public void evict(InlineTimedCache<?, ?> cache) {
            if (!dead && value instanceof Freeable) {
                ((Freeable)value).onCacheEviction(cache, key);
            }
            dead = true;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public E getValue() {
            return value;
        }

        @Override
        public E setValue(E value) {
            E old = this.value;
            this.value = value;
            return old;
        }

    }

}
