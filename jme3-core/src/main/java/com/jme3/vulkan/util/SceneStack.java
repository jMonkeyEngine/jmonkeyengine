package com.jme3.vulkan.util;

import com.jme3.scene.Spatial;

public interface SceneStack <T> {

    T push(Spatial spatial);

    T pop();

    T peek();

    void clear();

}
