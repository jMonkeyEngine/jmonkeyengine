package com.jme3.vulkan.util;

import com.jme3.scene.Spatial;

public interface SceneIterationListener {

    void push(Spatial spatial);

    void pop();

}
