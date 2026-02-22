package com.jme3.vulkan.devices;

import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.commands.CommandQueue;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.FormatFeature;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.surface.Surface;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static com.jme3.renderer.vulkan.VulkanUtils.enumerateBuffer;
import static org.lwjgl.vulkan.VK10.*;

public abstract class AbstractPhysicalDevice implements PhysicalDevice, Disposable {

    private final VulkanInstance instance;
    private final VkPhysicalDevice physicalDevice;
    private final DisposableReference ref;
    private final DeviceInfo info = new DeviceInfo();

    public AbstractPhysicalDevice(VulkanInstance instance, long id) {
        this.instance = instance;
        this.physicalDevice = new VkPhysicalDevice(id, instance.getNativeObject());
        ref = DisposableManager.reference(this);
    }

    @Override
    public VulkanInstance getInstance() {
        return instance;
    }

    @Override
    public VkPhysicalDevice getDeviceHandle() {
        return physicalDevice;
    }

    @Override
    public VkQueueFamilyProperties.Buffer getQueueFamilyProperties() {
        return info.getQueueFamilyProperties(this);
    }

    @Override
    public VkPhysicalDeviceProperties getProperties() {
        return info.getDeviceProperties(this);
    }

    @Override
    public VkPhysicalDeviceFeatures getFeatures(VkPhysicalDeviceFeatures features) {
        vkGetPhysicalDeviceFeatures(physicalDevice, features);
        return features;
    }

    @Override
    public VkPhysicalDeviceFeatures2 getFeatures(VkPhysicalDeviceFeatures2 features) {
        VK11.vkGetPhysicalDeviceFeatures2(physicalDevice, features);
        return features;
    }

    @Override
    public VkExtensionProperties.Buffer getExtensionProperties() {
        return info.getExtensionProperties(this);
    }

    @Override
    public VkPhysicalDeviceMemoryProperties getMemoryProperties() {
        return info.getMemoryProperties(this);
    }

    @Override
    public int findSupportedMemoryType(int types, Flag<MemoryProp> flags) {
        VkPhysicalDeviceMemoryProperties mem = getMemoryProperties();
        for (int i = 0; i < mem.memoryTypeCount(); i++) {
            if ((types & (1 << i)) != 0 && (mem.memoryTypes().get(i).propertyFlags() & flags.bits()) != 0) {
                return i;
            }
        }
        throw new NullPointerException("Suitable memory type not found.");
    }

    @Override
    public Format findSupportedFormat(VulkanImage.Tiling tiling, Flag<FormatFeature> features, Format... candidates) {
        VkFormatProperties props = VkFormatProperties.create();
        for (Format f : candidates) {
            vkGetPhysicalDeviceFormatProperties(physicalDevice, f.getEnum(), props);
            if ((tiling == VulkanImage.Tiling.Linear && features.contains(props.linearTilingFeatures()))
                    || (tiling == VulkanImage.Tiling.Optimal && features.contains(props.optimalTilingFeatures()))) {
                return f;
            }
        }
        throw new NullPointerException("Failed to find supported format.");
    }

    @Override
    public boolean querySwapchainSupport(Surface surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.getNativeObject(), count, null);
            if (count.get(0) <= 0) return false;
            KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.getNativeObject(), count, null);
            return count.get(0) > 0;
        }
    }

    @Override
    public Runnable createDestroyer() {
        return info;
    }

    @Override
    public DisposableReference getReference() {
        return ref;
    }

    protected VkDeviceQueueCreateInfo.Buffer createQueueFamilyInfo(MemoryStack stack, QueueInfo... queues) {
        Map<Integer, List<QueueInfo>> families = new HashMap<>();
        for (QueueInfo q : queues) {
            families.computeIfAbsent(q.getFamilyIndex(), k -> new LinkedList<>()).add(q);
        }
        VkDeviceQueueCreateInfo.Buffer familyInfo = VkDeviceQueueCreateInfo.calloc(families.size(), stack);
        for (Map.Entry<Integer, List<QueueInfo>> f : families.entrySet()) {
            FloatBuffer priorities = stack.mallocFloat(f.getValue().size());
            int queueIndex = 0;
            for (QueueInfo q : f.getValue()) {
                priorities.put(q.getPriority());
                q.setQueueIndex(queueIndex++);
            }
            familyInfo.get().sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(f.getKey())
                    .pQueuePriorities(priorities.flip());
        }
        return familyInfo.flip();
    }

    private static class DeviceInfo implements Runnable {

        private final IntBuffer intBuf = MemoryUtil.memAllocInt(1);
        private VkQueueFamilyProperties.Buffer familyProps;
        private VkPhysicalDeviceProperties properties;
        private VkExtensionProperties.Buffer extProps;
        private VkPhysicalDeviceMemoryProperties memProps;

        @Override
        public void run() {
            MemoryUtil.memFree(intBuf);
            if (familyProps != null) {
                familyProps.free();
            }
            if (properties != null) {
                properties.free();
            }
            if (extProps != null) {
                extProps.free();
            }
            if (memProps != null) {
                memProps.free();
            }
        }

        public VkQueueFamilyProperties.Buffer getQueueFamilyProperties(PhysicalDevice device) {
            if (familyProps == null) {
                vkGetPhysicalDeviceQueueFamilyProperties(device.getDeviceHandle(), intBuf, null);
                familyProps = VkQueueFamilyProperties.malloc(intBuf.get(0));
                vkGetPhysicalDeviceQueueFamilyProperties(device.getDeviceHandle(), intBuf, familyProps);
            }
            return familyProps;
        }

        public VkPhysicalDeviceProperties getDeviceProperties(PhysicalDevice device) {
            if (properties == null) {
                properties = VkPhysicalDeviceProperties.malloc();
                vkGetPhysicalDeviceProperties(device.getDeviceHandle(), properties);
            }
            return properties;
        }

        public VkExtensionProperties.Buffer getExtensionProperties(PhysicalDevice device) {
            if (extProps == null) {
                vkEnumerateDeviceExtensionProperties(device.getDeviceHandle(), (ByteBuffer)null, intBuf, null);
                extProps = VkExtensionProperties.malloc(intBuf.get(0));
                vkEnumerateDeviceExtensionProperties(device.getDeviceHandle(), (ByteBuffer)null, intBuf, extProps);
            }
            return extProps;
        }

        public VkPhysicalDeviceMemoryProperties getMemoryProperties(PhysicalDevice device) {
            if (memProps == null) {
                memProps = VkPhysicalDeviceMemoryProperties.malloc();
                vkGetPhysicalDeviceMemoryProperties(device.getDeviceHandle(), memProps);
            }
            return memProps;
        }

    }

    protected static class QueueInfo {

        private final float priority;
        private CommandQueue queue;
        private Integer familyIndex;
        private Integer queueIndex;

        public QueueInfo(float priority) {
            this.priority = priority;
        }

        public CommandQueue generate(LogicalDevice<?> device) {
            if (familyIndex == null || queueIndex == null) {
                throw new IllegalStateException("Indices not provided.");
            }
            return queue = new CommandQueue(device, familyIndex, queueIndex);
        }

        public void setFamilyIndex(int familyIndex) {
            this.familyIndex = familyIndex;
        }

        public void setQueueIndex(int queueIndex) {
            this.queueIndex = queueIndex;
        }

        public float getPriority() {
            return priority;
        }

        public CommandQueue getQueue() {
            return queue;
        }

        public Integer getFamilyIndex() {
            return familyIndex;
        }

        public int getQueueIndex() {
            return queueIndex;
        }

        public boolean hasFamily() {
            return familyIndex != null;
        }

    }

}
