package com.jme3.vulkan.app;

import com.jme3.app.SimpleApplication;
import com.jme3.vulkan.VulkanInstance;
import org.lwjgl.system.MemoryStack;

public class VulkanApplication extends SimpleApplication {

    protected VulkanInstance instance;

    @Override
    public void simpleInitApp() {
        try (MemoryStack stack = MemoryStack.stackPush()) {

        }
    }

}
