package com.jme3.vulkan.devices;

import com.jme3.util.natives.Native;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.VulkanObject;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.*;
import java.util.function.Function;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class LogicalDevice <T extends PhysicalDevice> extends VulkanObject<VkDevice> {

    private final VulkanInstance instance;
    private final Set<String> enabledExtensions = new HashSet<>();
    private final VkPhysicalDeviceFeatures enabledFeatures = VkPhysicalDeviceFeatures.calloc();
    private T physical;

    public LogicalDevice(VulkanInstance instance) {
        this.instance = instance;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            vkDestroyDevice(object, null);
            enabledFeatures.free();
        };
    }

    public void waitIdle() {
        vkDeviceWaitIdle(object);
    }

    public T getPhysicalDevice() {
        return physical;
    }

    public Set<String> getEnabledExtensions() {
        return Collections.unmodifiableSet(enabledExtensions);
    }

    public VkPhysicalDeviceFeatures getEnabledFeatures() {
        return enabledFeatures;
    }

    public Builder build(Function<Long, T> deviceFactory) {
        return new Builder(deviceFactory);
    }

    public class Builder extends VulkanObject.Builder<LogicalDevice> {

        private final Function<Long, T> deviceFactory;
        private final Set<DeviceExtension> extensions = new HashSet<>();
        private final Collection<DeviceFeature> features = new ArrayList<>();
        private final Collection<DeviceFilter> filters = new ArrayList<>();
        private boolean enableAllFeatures = false;

        public Builder(Function<Long, T> deviceFactory) {
            this.deviceFactory = deviceFactory;
        }

        @Override
        protected void build() {
            findSuitablePhysicalDevice();
            VkPhysicalDeviceFeatures supportedFeatures = physical.getFeatures(stack);
            if (enableAllFeatures) {
                enabledFeatures.set(supportedFeatures);
            } else for (DeviceFeature f : features) {
                if (f.evaluateFeatureSupport(supportedFeatures) != null) {
                    f.enableFeature(enabledFeatures);
                }
            }
            VkDeviceCreateInfo create = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(physical.createQueueFamilyInfo(stack))
                    .pEnabledFeatures(enabledFeatures);
            if (!extensions.isEmpty()) {
                PointerBuffer exts = stack.mallocPointer(extensions.size());
                for (DeviceExtension e : extensions) {
                    enabledExtensions.add(e.getName());
                    exts.put(stack.UTF8(e.getName()));
                }
                create.ppEnabledExtensionNames(exts.flip());
            }
            Set<String> layers = physical.getInstance().getLayers();
            if (!layers.isEmpty()) {
                PointerBuffer lyrs = stack.mallocPointer(layers.size());
                for (String l : layers) {
                    lyrs.put(stack.UTF8(l));
                }
                create.ppEnabledLayerNames(lyrs.flip());
            }
            PointerBuffer ptr = stack.mallocPointer(1);
            check(vkCreateDevice(physical.getPhysicalDevice(), create, null, ptr),
                    "Failed to create logical device.");
            object = new VkDevice(ptr.get(0), physical.getPhysicalDevice(), create);
            ref = Native.get().register(LogicalDevice.this);
            physical.getInstance().getNativeReference().addDependent(ref);
            physical.createQueues(LogicalDevice.this);
        }

        private void findSuitablePhysicalDevice() {
            PointerBuffer devices = enumerateBuffer(stack, stack::mallocPointer,
                    (count, buffer) -> check(
                            vkEnumeratePhysicalDevices(instance.getNativeObject(), count, buffer),
                            "Failed to enumerate physical devices."));
            physical = null;
            float topWeight = Float.NEGATIVE_INFINITY;
            deviceLoop: for (T d : iteratePointers(devices, deviceFactory::apply)) {
                if (!d.populateQueueFamilyIndices()) {
                    continue;
                }
                // attempting to evaluate all devices with only one memory stack
                // results in an out of memory error
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    float deviceWeight = 0f;
                    // extensions
                    VkExtensionProperties.Buffer supportedExts = d.getExtensionProperties(stack);
                    Set<String> extSet = new HashSet<>();
                    supportedExts.stream().forEach(e -> extSet.add(e.extensionNameString()));
                    for (DeviceExtension ext : extensions) {
                        Float weight = ext.evaluate(extSet);
                        if (weight == null) {
                            continue deviceLoop;
                        }
                        deviceWeight += weight;
                    }
                    // features
                    VkPhysicalDeviceFeatures ftrs = d.getFeatures(stack);
                    for (DeviceFeature f : features) {
                        Float weight = f.evaluateFeatureSupport(ftrs);
                        if (weight == null) {
                            continue deviceLoop;
                        }
                        deviceWeight += weight;
                    }
                    // miscellaneous filters
                    for (DeviceFilter f : filters) {
                        Float weight = f.evaluateDevice(d);
                        if (weight == null) {
                            continue deviceLoop;
                        }
                        deviceWeight += weight;
                    }
                    // compare
                    if (deviceWeight > topWeight) {
                        physical = d;
                        topWeight = deviceWeight;
                    }
                }
            }
            if (physical == null) {
                throw new NullPointerException("Failed to find suitable physical device.");
            }
        }

        public void addExtension(DeviceExtension extension) {
            extensions.add(extension);
        }

        public void addCriticalExtension(String name) {
            addExtension(DeviceExtension.critical(name));
        }

        public void addOptionalExtension(String name, float successWeight) {
            addExtension(DeviceExtension.optional(name, successWeight));
        }

        public void addFeature(DeviceFeature feature) {
            features.add(feature);
        }

        public void addFilter(DeviceFilter filter) {
            filters.add(filter);
        }

        public void setEnableAllFeatures(boolean enableAllFeatures) {
            this.enableAllFeatures = enableAllFeatures;
        }

    }

    private static class EvaluatedDevice <T extends PhysicalDevice> {

        public final T device;
        public final Collection<DeviceFeature> features = new ArrayList<>();

        private EvaluatedDevice(T device) {
            this.device = device;
        }

    }

}
