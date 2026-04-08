package com.jme3.vulkan.descriptors;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CachedDescriptorSet {

    private final DescriptorSet set;
    private final Map<String, DescriptorSetWriter> writers = new HashMap<>();
    private final Map<String, DescriptorSetWriter> changes = new HashMap<>();

    public CachedDescriptorSet(DescriptorSet set) {
        this.set = set;
    }

    public void stageWriter(String name, DescriptorSetWriter writer) {
        if (!Objects.equals(writers.put(name, writer), writer)) {
            changes.put(name, writer);
        }
    }

    public void writeChanges() {
        set.write(changes.values());
        changes.clear();
    }

    public DescriptorSet getSet() {
        return set;
    }

}
