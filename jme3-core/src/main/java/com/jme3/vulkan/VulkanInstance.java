package com.jme3.vulkan;

import com.jme3.system.JmeVersion;
import com.jme3.util.AbstractNativeBuilder;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK14.*;

public class VulkanInstance extends AbstractNative<VkInstance> {

    public static final String ENGINE_NAME = "jMonkeyEngine";
    public static final String LUNARG_LAYER = "VK_LAYER_KHRONOS_validation";

    public enum Version implements IntEnum<Version> {

        v10(VK_API_VERSION_1_0),
        v11(VK_API_VERSION_1_1),
        v12(VK_API_VERSION_1_2),
        v13(VK_API_VERSION_1_3),
        v14(VK_API_VERSION_1_4);

        private final int vk;

        Version(int vk) {
            this.vk = vk;
        }

        @Override
        public int getEnum() {
            return vk;
        }

    }

    private final Set<String> extensions = new HashSet<>();
    private final Set<String> layers = new HashSet<>();
    private String appName = "Unnamed App";
    private int appVersion = VK_MAKE_VERSION(0, 0, 0);
    private IntEnum<Version> apiVersion;
    private VulkanLogger logger;

    protected VulkanInstance(IntEnum<Version> apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyInstance(object, null);
    }

    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) return false;
        VulkanInstance that = (VulkanInstance) other;
        return object.address() == that.object.address();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(object.address());
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

    public static VulkanInstance build(Consumer<Builder> config) {
        return build(Version.v10, config);
    }

    public static VulkanInstance build(IntEnum<Version> apiVersion, Consumer<Builder> config) {
        Builder b = new VulkanInstance(apiVersion).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends AbstractNativeBuilder<VulkanInstance> {

        @Override
        protected VulkanInstance construct() {
            String[] ver = JmeVersion.VERSION_NUMBER.split("\\.", 3);
            VkApplicationInfo info = VkApplicationInfo.calloc(stack)
                    .apiVersion(apiVersion.getEnum())
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
            return VulkanInstance.this;
        }

        public void setApplicationName(String name) {
            VulkanInstance.this.appName = name;
        }

        public void setApplicationVersion(int major, int minor, int patch) {
            VulkanInstance.this.appVersion = VK_MAKE_VERSION(major, minor, patch);
        }

        public void setApiVersion(IntEnum<Version> version) {
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
