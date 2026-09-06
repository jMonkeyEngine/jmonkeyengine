package com.jme3.vulkan.scene;

public interface SceneComponent {

    default InheritMode getInheritMode() {
        return InheritMode.Off;
    }

}
