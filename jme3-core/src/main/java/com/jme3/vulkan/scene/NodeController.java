package com.jme3.vulkan.scene;

import com.jme3.math.Transform;

public interface NodeController {

    void update(Scene.NodeData node, double tpf);

    void onTransformComputed(Scene.NodeData node, Transform worldTransform);

}
