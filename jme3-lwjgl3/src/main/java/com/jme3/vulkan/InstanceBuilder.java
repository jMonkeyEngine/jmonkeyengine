package com.jme3.vulkan;

import com.jme3.system.JmeVersion;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public class InstanceBuilder implements AutoCloseable {

    public static final String LUNARG_LAYER = "VK_LAYER_KHRONOS_validation";

    private final MemoryStack stack;
    private final VkApplicationInfo info;
    private final Collection<PointerBuffer> extPointers = new ArrayList<>();
    private final Collection<String> extensions = new ArrayList<>();
    private final Collection<String> layers = new ArrayList<>();

    public InstanceBuilder() {
        this(VK_API_VERSION_1_0);
    }

    public InstanceBuilder(int vulkanApi) {
        stack = MemoryStack.stackPush();
        String[] ver = JmeVersion.VERSION_NUMBER.split("\\.", 3);
        info = VkApplicationInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pEngineName(stack.UTF8("jMonkeyEngine"))
                .engineVersion(VK_MAKE_VERSION(
                        Integer.parseInt(ver[0]),
                        Integer.parseInt(ver[1]),
                        Integer.parseInt(ver[2])))
                .apiVersion(vulkanApi);
    }

    @Override
    public void close() {
        stack.pop();
    }

    public VulkanInstance build() {
        return new VulkanInstance(info, getExtensions(), getLayers());
    }

    public void setApplicationName(String name) {
        info.pApplicationName(stack.UTF8(name));
    }

    public void setApplicationVersion(int major, int minor, int patch) {
        info.applicationVersion(VK_MAKE_VERSION(major, minor, patch));
    }

    public void addGlfwExtensions() {
        addExtensions(Objects.requireNonNull(GLFWVulkan.glfwGetRequiredInstanceExtensions()));
    }

    public void addDebugExtension() {
        addExtension(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
    }

    public void addLunarGLayer() {
        addLayer(LUNARG_LAYER);
    }

    public void addExtensions(PointerBuffer exts) {
        extPointers.add(exts);
    }

    public void addExtension(String ext) {
        extensions.add(ext);
    }

    public void addLayer(String layer) {
        layers.add(layer);
    }

    public VkApplicationInfo getInfo() {
        return info;
    }

    public PointerBuffer getExtensions() {
        return getExtensions(stack.mallocPointer(getNumExtensions()));
    }

    public PointerBuffer getExtensions(PointerBuffer exts) {
        for (PointerBuffer ptr : extPointers) {
            for (int i = 0; i < ptr.limit(); i++) {
                if (exts.hasRemaining()) {
                    exts.put(ptr.get(i));
                } else return exts.rewind();
            }
        }
        for (String e : extensions) {
            if (exts.hasRemaining()) {
                exts.put(stack.UTF8(e));
            } else break;
        }
        return exts.rewind();
    }

    public Collection<PointerBuffer> getUnnamedExtensions() {
        return extPointers;
    }

    public Collection<String> getNamedExtensions() {
        return extensions;
    }

    public int getNumExtensions() {
        int size = extensions.size();
        for (PointerBuffer exts : extPointers) {
            size += exts.limit();
        }
        return size;
    }

    public PointerBuffer getLayers() {
        return getLayers(stack.mallocPointer(layers.size()));
    }

    public PointerBuffer getLayers(PointerBuffer lyrs) {
        for (String l : layers) {
            if (lyrs.hasRemaining()) {
                lyrs.put(stack.UTF8(l));
            }
        }
        return lyrs.rewind();
    }

    public Collection<String> getNamedLayers() {
        return layers;
    }

    public int getNumLayers() {
        return layers.size();
    }

    public MemoryStack getStack() {
        return stack;
    }

}
