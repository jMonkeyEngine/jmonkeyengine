package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanInstance implements Native<VkInstance> {

    private final NativeReference ref;
    private VkInstance instance;

    public VulkanInstance(VkApplicationInfo appInfo, PointerBuffer extensions, PointerBuffer layers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkInstanceCreateInfo create = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(extensions);
            if (layers != null) {
                create.ppEnabledLayerNames(layers);
            }
            PointerBuffer ptr = stack.mallocPointer(1);
            check(vkCreateInstance(create, null, ptr), "Failed to create instance.");
            instance = new VkInstance(ptr.get(0), create);
        }
        ref = Native.get().register(this);
    }

    public Surface createGlfwSurface(long window) {
        return new Surface(this, window);
    }

    @Override
    public VkInstance getNativeObject() {
        return instance;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyInstance(instance, null);
    }

    @Override
    public void prematureNativeDestruction() {
        instance = null;
    }

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

}
