package com.jme3.vulkan.scene;

public interface TagListener {

    void tagAdded(Scene scene, int node, String tag);

    void tagRemoved(Scene scene, int node, String tag);

}
