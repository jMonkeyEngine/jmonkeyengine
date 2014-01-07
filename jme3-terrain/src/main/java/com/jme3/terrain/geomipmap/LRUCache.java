package com.jme3.terrain.geomipmap;

// Copyright 2007 Christian d'Heureuse, Inventec Informatik AG, Zurich,
// Switzerland
// www.source-code.biz, www.inventec.ch/chdh
//
// This module is multi-licensed and may be used under the terms
// of any of the following licenses:
//
// EPL, Eclipse Public License, V1.0 or later, http://www.eclipse.org/legal
// LGPL, GNU Lesser General Public License, V2 or later,
// http://www.gnu.org/licenses/lgpl.html
// GPL, GNU General Public License, V2 or later,
// http://www.gnu.org/licenses/gpl.html
// AL, Apache License, V2.0 or later, http://www.apache.org/licenses
// BSD, BSD License, http://www.opensource.org/licenses/bsd-license.php
//
// Please contact the author if you need another license.
// This module is provided "as is", without warranties of any kind.
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRU cache, based on <code>LinkedHashMap</code>.
 * 
 * <p>
 * This cache has a fixed maximum number of elements (<code>cacheSize</code>).
 * If the cache is full and another entry is added, the LRU (least recently
 * used) entry is dropped.
 * 
 * <p>
 * This class is thread-safe. All methods of this class are synchronized.
 * 
 * <p>
 * Author: Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland<br>
 * Multi-licensed: EPL / LGPL / GPL / AL / BSD.
 */
public class LRUCache<K, V> {

    private static final float hashTableLoadFactor = 0.75f;
    private LinkedHashMap<K, V> map;
    private int cacheSize;

    /**
     * Creates a new LRU cache.
     * 
     * @param cacheSize
     *            the maximum number of entries that will be kept in this cache.
     */
    public LRUCache(int cacheSize) {
        this.cacheSize = cacheSize;
        int hashTableCapacity = (int) Math.ceil(cacheSize / LRUCache.hashTableLoadFactor) + 1;
        this.map = new LinkedHashMap<K, V>(hashTableCapacity, LRUCache.hashTableLoadFactor, true) {
            // (an anonymous inner class)

            private static final long serialVersionUID = 1;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return this.size() > LRUCache.this.cacheSize;
            }
        };
    }

    /**
     * Retrieves an entry from the cache.<br>
     * The retrieved entry becomes the MRU (most recently used) entry.
     * 
     * @param key
     *            the key whose associated value is to be returned.
     * @return the value associated to this key, or null if no value with this
     *         key exists in the cache.
     */
    public synchronized V get(K key) {
        return this.map.get(key);
    }

    /**
     * Adds an entry to this cache.
     * The new entry becomes the MRU (most recently used) entry.
     * If an entry with the specified key already exists in the cache, it is
     * replaced by the new entry.
     * If the cache is full, the LRU (least recently used) entry is removed from
     * the cache.
     * 
     * @param key
     *            the key with which the specified value is to be associated.
     * @param value
     *            a value to be associated with the specified key.
     */
    public synchronized void put(K key, V value) {
        this.map.put(key, value);
    }

    /**
     * Clears the cache.
     */
    public synchronized void clear() {
        this.map.clear();
    }

    /**
     * Returns the number of used entries in the cache.
     * 
     * @return the number of entries currently in the cache.
     */
    public synchronized int usedEntries() {
        return this.map.size();
    }

    /**
     * Returns a <code>Collection</code> that contains a copy of all cache
     * entries.
     * 
     * @return a <code>Collection</code> with a copy of the cache content.
     */
    public synchronized Collection<Map.Entry<K, V>> getAll() {
        return new ArrayList<Map.Entry<K, V>>(this.map.entrySet());
    }
} // end class LRUCache
