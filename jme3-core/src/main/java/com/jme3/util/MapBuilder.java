package com.jme3.util;

import java.util.HashMap;
import java.util.Map;

public class MapBuilder <K, E, T extends Map<K, E>> {

    private final T map;

    public MapBuilder(T map) {
        this.map = map;
    }

    public static <K, E, T extends Map<K, E>> MapBuilder<K, E, T> build(T map) {
        map.clear();
        return new MapBuilder<>(map);
    }

    public static <K, E> MapBuilder<K, E, Map<K, E>> build() {
        return new MapBuilder<>(new HashMap<>());
    }

    public MapBuilder<K, E, T> put(K key, E value) {
        map.put(key, value);
        return this;
    }

    public T get() {
        return map;
    }

}
