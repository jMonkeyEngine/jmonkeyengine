package com.jme3.vulkan.surface;

import com.jme3.app.Application;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.devices.DeviceFilter;
import com.jme3.vulkan.devices.PhysicalDevice;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;

import java.nio.IntBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;

public class Surface extends AbstractNative<Long> implements DeviceFilter {

    private final VulkanInstance instance;
    private final long window;

    public Surface(VulkanInstance instance, Application app) {
        this(instance, (long)app.getContext().getWindowHandle());
    }

    public Surface(VulkanInstance instance, long window) {
        this.instance = instance;
        this.window = window;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            object = getLong(stack, ptr -> check(GLFWVulkan.glfwCreateWindowSurface(
                    instance.getNativeObject(), window, null, ptr),
                    "Failed to create surface for GLFW window."));
            ref = DisposableManager.reference(this);
            instance.getReference().addDependent(ref);
        }
    }

    @Override
    public Float evaluateDevice(PhysicalDevice device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device.getDeviceHandle(), object, count, null);
            if (count.get(0) == 0) {
                System.out.println("Reject device by surface support (formats)");
                return null;
            }
            KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device.getDeviceHandle(), object, count, null);
            if (count.get(0) == 0) {
                System.out.println("Reject device by surface support (present modes)");
                return null;
            }
            return 0f;
        }
    }

    @Override
    public Runnable createDestroyer() {
        return () -> KHRSurface.vkDestroySurfaceKHR(instance.getNativeObject(), object, null);
    }

    public long getWindowHandle() {
        return window;
    }

}
