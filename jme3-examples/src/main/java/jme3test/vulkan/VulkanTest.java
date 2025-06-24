package jme3test.vulkan;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3.system.vulkan.DeviceEvaluator;
import com.jme3.system.vulkan.LwjglVulkanContext;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK14.*;

public class VulkanTest extends SimpleApplication {

    private static final Logger LOG = Logger.getLogger(VulkanTest.class.getName());

    private VkInstance instance;
    private VkPhysicalDevice device;
    private QueueFamilies queues;
    private final Collection<PointerBuffer> extensions = new ArrayList<>();
    private final List<String> layers = new ArrayList<>();
    private final Collection<DeviceEvaluator> deviceEvaluators = new ArrayList<>();
    private final VkDebugUtilsMessengerCallbackEXT debugCallback = new VulkanDebugCallback();
    private long debugMessenger = NULL;

    public static void main(String[] args) {
        VulkanTest app = new VulkanTest();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(800);
        settings.setHeight(800);
        settings.setRenderer("CUSTOM" + LwjglVulkanContext.class.getName());
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // basic validation layer from the Vulkan SDK
        //layers.add("VK_LAYER_KHRONOS_validation");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            createInstance(stack);
            createDebugMessenger(stack);
            device = findPhysicalDevice(stack);
            queues = populateQueueFamily(stack, device);
        }
    }

    @Override
    public void stop() {
        LOG.info("Destroying vulkan instance.");
        if (debugMessenger != NULL) {
            verifyExtensionMethod(instance, "vkDestroyDebugUtilsMessengerEXT");
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        }
        if (instance != null) {
            vkDestroyInstance(instance, null);
        }
        super.stop();
    }

    private void createInstance(MemoryStack stack) {
        VkApplicationInfo app = VkApplicationInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(stack.ASCII(context.getSettings().getTitle()))
                .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                .pEngineName(stack.ASCII("JMonkeyEngine"))
                .engineVersion(VK_MAKE_VERSION(3, 9, 0))
                .apiVersion(VK_API_VERSION_1_4);
        VkInstanceCreateInfo create = VkInstanceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pNext(createDebugger(stack, debugCallback))
                .pApplicationInfo(app);
        if (!layers.isEmpty()) {
            verifyValidationLayerSupport(stack);
            create.ppEnabledLayerNames(toPointers(stack, layers.stream(), layers.size(), stack::UTF8));
        }
        addExtension(Objects.requireNonNull(GLFWVulkan.glfwGetRequiredInstanceExtensions()));
        addExtension(stack, VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
        create.ppEnabledExtensionNames(gatherPointers(stack, extensions));
        instance = new VkInstance(getPointer(stack,
                ptr -> check(vkCreateInstance(create, null, ptr), "Failed to create instance.")), create);
    }

    private void createDebugMessenger(MemoryStack stack) {
        verifyExtensionMethod(instance, "vkCreateDebugUtilsMessengerEXT");
        debugMessenger = getLong(stack, ptr -> vkCreateDebugUtilsMessengerEXT(instance, createDebugger(stack, debugCallback), null, ptr));
    }

    private VkPhysicalDevice findPhysicalDevice(MemoryStack stack) {
        PointerBuffer devices = enumerateBuffer(stack, stack::mallocPointer, (count, buffer) -> vkEnumeratePhysicalDevices(instance, count, buffer));
        VkPhysicalDevice device = null;
        float score = 0f;
        for (VkPhysicalDevice d : iteratePointers(devices, p -> new VkPhysicalDevice(p, instance))) {
            Float s = evaluateDevice(stack, d);
            if (s != null && (device == null || s > score) && populateQueueFamily(stack, d).isComplete()) {
                device = d;
                score = s;
            }
        }
        if (device == null) {
            throw new NullPointerException("Failed to find suitable GPU.");
        }
        return device;
    }

    private Float evaluateDevice(MemoryStack stack, VkPhysicalDevice device) {
        if (deviceEvaluators.isEmpty()) {
            return 0f;
        }
        VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.malloc(stack);
        VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.malloc(stack);
        vkGetPhysicalDeviceProperties(device, props);
        vkGetPhysicalDeviceFeatures(device, features);
        float score = 0f;
        for (DeviceEvaluator e : deviceEvaluators) {
            Float s = e.evaluateDevice(device, props, features);
            if (s == null) {
                return null;
            }
            score += s;
        }
        return score;
    }

    private QueueFamilies populateQueueFamily(MemoryStack stack, VkPhysicalDevice device) {
        QueueFamilies fams = new QueueFamilies();
        VkQueueFamilyProperties.Buffer props = enumerateBuffer(stack, VkQueueFamilyProperties::malloc,
                (count, buffer) -> vkGetPhysicalDeviceQueueFamilyProperties(device, count, buffer));
        int i = 0;
        for (VkQueueFamilyProperties p : props) {
            if (isBitSet(p.queueFlags(), VK_QUEUE_GRAPHICS_BIT)) {
                fams.graphics = i;
            }
            i++;
        }
        return fams;
    }

    private void verifyValidationLayerSupport(MemoryStack stack) {
        VkLayerProperties.Buffer supported = enumerateBuffer(stack, n -> VkLayerProperties.malloc(n, stack),
                VK10::vkEnumerateInstanceLayerProperties);
        requestLoop: for (String r : layers) {
            for (VkLayerProperties l : supported) {
                if (r.equals(l.layerNameString())) {
                    continue requestLoop;
                }
            }
            throw new NullPointerException("Validation layer " + r + " is not available.");
        }
    }

    private VkDebugUtilsMessengerCreateInfoEXT createDebugger(MemoryStack stack, VkDebugUtilsMessengerCallbackEXT callback) {
        return VkDebugUtilsMessengerCreateInfoEXT.malloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
                        | VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT)
                .messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                .pfnUserCallback(callback);
    }

    private void addExtension(MemoryStack stack, String ext) {
        extensions.add(stack.mallocPointer(1).put(stack.UTF8(ext)).rewind());
    }

    private void addExtension(PointerBuffer ext) {
        extensions.add(ext);
    }

    private static class QueueFamilies {

        public Integer graphics;

        public boolean isComplete() {
            return graphics != null;
        }

    }

    private static class VulkanDebugCallback extends VkDebugUtilsMessengerCallbackEXT {

        @Override
        public int invoke(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
            try (VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)) {
                LOG.log(getLoggingLevel(messageSeverity), data.pMessageString());
            }
            return VK_FALSE; // always return false, true is only really used for testing validation layers
        }

        public Level getLoggingLevel(int messageSeverity) {
            switch (messageSeverity) {
                case EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT:
                    return Level.SEVERE;
                case EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT:
                    return Level.WARNING;
                case EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT:
                    return Level.INFO;
                case EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT:
                    return Level.FINE;
                default: throw new UnsupportedOperationException("Unsupported severity bit: "
                        + Integer.numberOfTrailingZeros(Integer.highestOneBit(messageSeverity)));
            }
        }

    }

}
