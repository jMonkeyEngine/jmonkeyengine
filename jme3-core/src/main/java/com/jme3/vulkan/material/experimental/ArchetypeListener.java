package com.jme3.vulkan.material.experimental;

public interface ArchetypeListener <T> {

    T build(int node);

    void discard(T artifact, int node);

}
