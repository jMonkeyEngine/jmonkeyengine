package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;

import java.util.logging.Level;

import static com.jme3.renderer.vulkan.VulkanUtils.getLong;
import static com.jme3.renderer.vulkan.VulkanUtils.verifyExtensionMethod;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.VK_FALSE;

public class VulkanLogger implements Native<Long> {

    private final VulkanInstance instance;
    private final Level exceptionThreshold;
    private final NativeReference ref;
    private final long id;
    private final VkDebugUtilsMessengerCallbackEXT callback = new VkDebugUtilsMessengerCallbackEXT() {
        @Override
        public int invoke(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
            return message(messageSeverity, messageTypes, pCallbackData, pUserData);
        }
    };

    public VulkanLogger(VulkanInstance instance, Level exceptionThreshold) {
        this.instance = instance;
        this.exceptionThreshold = exceptionThreshold;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDebugUtilsMessengerCreateInfoEXT create = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                    .messageSeverity(
                              VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                            | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
                            | VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
                            | VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
                    ).messageType(
                              VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
                            | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
                            | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                    ).pfnUserCallback(callback);
            verifyExtensionMethod(instance.getNativeObject(), "vkCreateDebugUtilsMessengerEXT");
            id = getLong(stack, ptr -> vkCreateDebugUtilsMessengerEXT(instance.getNativeObject(), create, null, ptr));
        }
        ref = Native.get().register(this);
        instance.getNativeReference().addDependent(ref);
    }

    public int message(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
        VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
        Level lvl = getLoggingLevel(messageSeverity);
        System.err.println(lvl.getName() + "  " + data.pMessageString());
        if (exceptionThreshold != null && lvl.intValue() >= exceptionThreshold.intValue()) {
            throw new RuntimeException(lvl.getName() + ": " + data.pMessageString());
        }
        return VK_FALSE; // always return false, true is only really used for testing validation layers
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            verifyExtensionMethod(instance.getNativeObject(), "vkDestroyDebugUtilsMessengerEXT");
            vkDestroyDebugUtilsMessengerEXT(instance.getNativeObject(), id, null);
            callback.close();
        };
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    private Level getLoggingLevel(int messageSeverity) {
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
