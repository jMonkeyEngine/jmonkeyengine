package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.devices.DeviceFilter;
import com.jme3.vulkan.devices.PhysicalDevice;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;

import java.nio.IntBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;

public class Surface implements Native<Long>, DeviceFilter {

    private final VulkanInstance instance;
    private final NativeReference ref;
    private final long window;
    private long id;

    public Surface(VulkanInstance instance, long window) {
        this.instance = instance;
        this.window = window;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            id = getLong(stack, ptr -> check(GLFWVulkan.glfwCreateWindowSurface(
                    instance.getNativeObject(), window, null, ptr),
                    "Failed to create surface for GLFW window."));
            ref = Native.get().register(this);
            instance.getNativeReference().addDependent(ref);
        }
    }

    @Override
    public Float evaluateDevice(PhysicalDevice device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device.getPhysicalDevice(), id, count, null);
            if (count.get(0) == 0) {
                System.out.println("Reject device by surface support (formats)");
                return null;
            }
            KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device.getPhysicalDevice(), id, count, null);
            if (count.get(0) == 0) {
                System.out.println("Reject device by surface support (present modes)");
                return null;
            }
            return 0f;
        }
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> KHRSurface.vkDestroySurfaceKHR(instance.getNativeObject(), id, null);
    }

    @Override
    public void prematureNativeDestruction() {
        id = MemoryUtil.NULL;
    }

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public long getWindowHandle() {
        return window;
    }

}
