package com.jme3.vulkan.devices;

import com.jme3.util.AbstractNativeBuilder;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.commands.CommandPool;
import com.jme3.vulkan.commands.Queue;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.PNextChain;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;
import org.lwjgl.vulkan.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class LogicalDevice <T extends PhysicalDevice> extends AbstractNative<VkDevice> {

    private final VulkanInstance instance;
    private final Set<String> enabledExtensions = new HashSet<>();
    private final Map<Thread, Collection<CommandPool>> pools = new ConcurrentHashMap<>();
    private PNextChain enabledFeatures;
    private T physical;

    protected LogicalDevice(VulkanInstance instance) {
        this.instance = instance;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> {
            vkDestroyDevice(object, null);
            enabledFeatures.free();
        };
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LogicalDevice<?> that = (LogicalDevice<?>) o;
        return instance == that.instance && object.address() == that.object.address();
    }

    @Override
    public int hashCode() {
        return Objects.hash(instance, object.address());
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

    public PNextChain getEnabledFeatures() {
        return enabledFeatures.getReadOnly();
    }

    public CommandPool getShortTermPool(Queue queue) {
        return getPool(queue, CommandPool.Create.Transient);
    }

    public CommandPool getLongTermPool(Queue queue) {
        return getPool(queue, CommandPool.Create.ResetCommandBuffer);
    }

    public CommandPool getPool(Queue queue, Flag<CommandPool.Create> flags) {
        if (queue.getDevice() != this) {
            throw new IllegalArgumentException("Queue must belong to this device.");
        }
        Collection<CommandPool> p = pools.computeIfAbsent(Thread.currentThread(),
                t -> new ArrayList<>());
        for (CommandPool pool : p) {
            if (pool.getQueue() == queue && pool.getFlags().contains(flags)) {
                return pool;
            }
        }
        CommandPool pool = new CommandPool(queue, flags);
        p.add(pool);
        return pool;
    }

    public static <T extends PhysicalDevice> LogicalDevice<T> build(VulkanInstance instance, Function<Long, T> deviceFactory, Consumer<LogicalDevice<T>.Builder> config) {
        LogicalDevice<T>.Builder b = new LogicalDevice<T>(instance).new Builder(deviceFactory);
        config.accept(b);
        return b.build();
    }

    public class Builder extends AbstractNativeBuilder<LogicalDevice<T>> {

        private final Function<Long, T> deviceFactory;
        private final Set<DeviceExtension> extensions = new HashSet<>();
        private final PNextChain featureChain = new PNextChain();
        private final Collection<DeviceFeature> features = new ArrayList<>();
        private final Collection<DeviceFilter> filters = new ArrayList<>();
        private boolean enableAllFeatures = false;

        public Builder(Function<Long, T> deviceFactory) {
            this.deviceFactory = deviceFactory;
        }

        @Override
        protected LogicalDevice<T> construct() {
            featureChain.add(p -> VkPhysicalDeviceFeatures2.calloc().pNext(p));
            findSuitablePhysicalDevice();
            VkDeviceCreateInfo create = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(physical.createQueueFamilyInfo(stack));
            if (instance.getApiVersion().is(VulkanInstance.Version.v10)) {
                featureChain.free();
                enabledFeatures = new PNextChain();
                VkPhysicalDeviceFeatures2 f2 = enabledFeatures.add(p -> VkPhysicalDeviceFeatures2.calloc().pNext(p));
                if (enableAllFeatures) {
                    physical.getFeatures(f2.features());
                } else {
                    VkPhysicalDeviceFeatures available = physical.getFeatures(VkPhysicalDeviceFeatures.malloc(stack));
                    for (DeviceFeature f : features) if (f instanceof LegacyDeviceFeature) {
                        LegacyDeviceFeature legacy = (LegacyDeviceFeature) f;
                        Float score = legacy.evaluateSupport(available);
                        if (score != null && score > 0f) {
                            legacy.enableFeature(f2.features());
                        }
                    }
                }
                create.pEnabledFeatures(f2.features());
            } else {
                physical.getFeatures(featureChain.get(VkPhysicalDeviceFeatures2.class));
                if (enableAllFeatures) {
                    enabledFeatures = featureChain;
                } else {
                    enabledFeatures = featureChain.copyStructure();
                    for (DeviceFeature f : features) {
                        Float score = f.evaluateSupport(featureChain);
                        if (score != null && score > 0f) {
                            f.enableFeature(enabledFeatures);
                        }
                    }
                    featureChain.free();
                }
                create.pNext(enabledFeatures.get(VkPhysicalDeviceFeatures2.class));
            }
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
            check(vkCreateDevice(physical.getDeviceHandle(), create, null, ptr),
                    "Failed to create logical device.");
            object = new VkDevice(ptr.get(0), physical.getDeviceHandle(), create);
            ref = DisposableManager.get().register(LogicalDevice.this);
            physical.getInstance().getReference().addDependent(ref);
            physical.createQueues(LogicalDevice.this);
            return LogicalDevice.this;
        }

        private void findSuitablePhysicalDevice() {
            PointerBuffer devices = enumerateBuffer(stack, stack::mallocPointer,
                    (count, buffer) -> check(
                            vkEnumeratePhysicalDevices(instance.getNativeObject(), count, buffer),
                            "Failed to enumerate physical devices."));
            physical = null;
            float topWeight = Float.NEGATIVE_INFINITY;
            deviceLoop: for (T device : iteratePointers(devices, deviceFactory::apply)) {
                if (!device.populateQueueFamilyIndices()) {
                    continue;
                }
                float deviceWeight = 0f;
                if (!featureChain.isEmpty()) {
                    if (instance.getApiVersion().is(VulkanInstance.Version.v10)) {
                        device.getFeatures(featureChain.get(VkPhysicalDeviceFeatures2.class).features());
                    } else {
                        device.getFeatures(featureChain.get(VkPhysicalDeviceFeatures2.class));
                    }
                    for (DeviceFeature f : features) {
                        Float weight = f.evaluateSupport(featureChain);
                        if (weight == null) {
                            continue deviceLoop;
                        }
                        deviceWeight += weight;
                    }
                }
                for (DeviceFilter f : filters) {
                    Float weight = f.evaluateDevice(device);
                    if (weight == null) {
                        continue deviceLoop;
                    }
                    deviceWeight += weight;
                }
                if (!extensions.isEmpty()) try (MemoryStack stack = MemoryStack.stackPush()) {
                    Set<String> deviceExts = device.getExtensionProperties(stack).stream()
                            .map(VkExtensionProperties::extensionNameString)
                            .collect(Collectors.toCollection(HashSet::new));
                    for (DeviceExtension e : extensions) {
                        Float weight = e.evaluate(deviceExts);
                        if (weight == null) {
                            continue deviceLoop;
                        }
                        deviceWeight += weight;
                    }
                }
                if (deviceWeight > topWeight) {
                    physical = device;
                    topWeight = deviceWeight;
                }
            }
            if (physical == null) {
                throw new NullPointerException("Failed to find a suitable physical vulkan device.");
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

        public void addFeatureContainer(Function<Long, Struct> generator) {
            featureChain.add(generator);
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

}
