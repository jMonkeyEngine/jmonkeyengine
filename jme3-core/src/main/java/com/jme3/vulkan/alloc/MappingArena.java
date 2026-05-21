package com.jme3.vulkan.alloc;

import java.util.IdentityHashMap;
import java.util.Map;

public class MappingArena implements AutoCloseable {

    private final Map<Memory, Runnable> mappings = new IdentityHashMap<>();

    public MappingArena(Memory... memory) {
        for (Memory m : memory) {
            m.map(this);
        }
    }

    @Override
    public void close() {
        for (Runnable u : mappings.values()) {
            u.run();
        }
        mappings.clear();
    }

    public boolean register(Memory mem, Runnable unmap) {
        return mappings.putIfAbsent(mem, unmap) == null;
    }

}
