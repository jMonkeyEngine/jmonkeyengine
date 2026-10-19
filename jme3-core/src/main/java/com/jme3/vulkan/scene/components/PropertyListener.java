package com.jme3.vulkan.scene.components;

public interface PropertyListener <T> {

    void onPropertyChanged(Property<T> property, int node);

}
