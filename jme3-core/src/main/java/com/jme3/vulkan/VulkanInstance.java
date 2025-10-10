package com.jme3.vulkan;

import com.jme3.system.JmeVersion;
import com.jme3.util.AbstractBuilder;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Native;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import java.util.*;
import java.util.logging.Level;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanInstance extends AbstractNative<VkInstance> {

    public static final String ENGINE_NAME = "jMonkeyEngine";
    public static final String LUNARG_LAYER = "VK_LAYER_KHRONOS_validation";

    private final Set<String> extensions = new HashSet<>();
    private final Set<String> layers = new HashSet<>();
    private String appName = "Unnamed App";
    private int appVersion = VK_MAKE_VERSION(0, 0, 0);
    private int apiVersion;
    private VulkanLogger logger;

    public VulkanInstance() {
        this(VK_API_VERSION_1_0);
    }

    public VulkanInstance(int apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyInstance(object, null);
    }

    public VulkanLogger createLogger(Level exceptionThreshold) {
        return logger = new VulkanLogger(this, exceptionThreshold);
    }

    public VulkanLogger getLogger() {
        return logger;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public Set<String> getLayers() {
        return layers;
    }

    public Builder build() {
        return new Builder();
    }

    public class Builder extends AbstractBuilder {

        @Override
        protected void build() {
            String[] ver = JmeVersion.VERSION_NUMBER.split("\\.", 3);
            VkApplicationInfo info = VkApplicationInfo.calloc(stack)
                    .apiVersion(apiVersion)
                    .pEngineName(stack.UTF8(ENGINE_NAME))
                    .engineVersion(VK_MAKE_VERSION(
                            Integer.parseInt(ver[0]),
                            Integer.parseInt(ver[1]),
                            Integer.parseInt(ver[2])))
                    .pApplicationName(stack.UTF8(appName))
                    .applicationVersion(appVersion);
            VkInstanceCreateInfo create = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(info);
            if (!extensions.isEmpty()) {
                PointerBuffer exts = stack.mallocPointer(extensions.size());
                for (String e : extensions) {
                    exts.put(stack.UTF8(e));
                }
                create.ppEnabledExtensionNames(exts.flip());
            }
            if (!layers.isEmpty()) {
                PointerBuffer lyrs = stack.mallocPointer(layers.size());
                for (String l : layers) {
                    lyrs.put(stack.UTF8(l));
                }
                create.ppEnabledLayerNames(lyrs.flip());
            }
            PointerBuffer ptr = stack.mallocPointer(1);
            check(vkCreateInstance(create, null, ptr), "Failed to create instance.");
            object = new VkInstance(ptr.get(0), create);
            ref = Native.get().register(VulkanInstance.this);
        }

        public void setApplicationName(String name) {
            VulkanInstance.this.appName = name;
        }

        public void setApplicationVersion(int major, int minor, int patch) {
            VulkanInstance.this.appVersion = VK_MAKE_VERSION(major, minor, patch);
        }

        public void setApiVersion(int version) {
            VulkanInstance.this.apiVersion = version;
        }

        public void addGlfwExtensions() {
            PointerBuffer exts = Objects.requireNonNull(GLFWVulkan.glfwGetRequiredInstanceExtensions(),
                    "Vulkan extensions for GLFW are not available.");
            for (int i = 0; i < exts.limit(); i++) {
                extensions.add(MemoryUtil.memUTF8(exts.get(i)));
            }
        }

        public void addDebugExtension() {
            extensions.add(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
        }

        public void addLunarGLayer() {
            layers.add(LUNARG_LAYER);
        }

        public void addExtension(String ext) {
            extensions.add(ext);
        }

        public void addLayer(String layer) {
            layers.add(layer);
        }

    }

}
