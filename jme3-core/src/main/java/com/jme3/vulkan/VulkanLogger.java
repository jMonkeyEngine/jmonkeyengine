package com.jme3.vulkan;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.util.Flag;
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

public class VulkanLogger extends AbstractNative<Long> {

    public enum Severity implements Flag<Severity> {

        Error(VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT),
        Warning(VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT),
        Info(VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT),
        Verbose(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT),
        All(VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
            | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
            | VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
            | VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT);

        private final int bits;

        Severity(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

        public static Severity getEnum(int vulkanFlag) {
            for (Severity s : Severity.values()) {
                if (s.bits == vulkanFlag) return s;
            }
            throw new UnsupportedOperationException("Unrecognized severity flag: " + vulkanFlag);
        }

    }

    public enum Type implements Flag<Type> {

        General(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT),
        Validation(VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT),
        Performance(VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT),
        All(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
            | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
            | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);

        private final int bits;

        Type(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

    }

    private final VulkanInstance instance;
    private final VkDebugUtilsMessengerCallbackEXT callback = new VkDebugUtilsMessengerCallbackEXT() {
        @Override
        public int invoke(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
            return message(messageSeverity, messageTypes, pCallbackData, pUserData);
        }
    };

    public VulkanLogger(VulkanInstance instance, Flag<Severity> severity, Flag<Type> type) {
        this.instance = instance;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDebugUtilsMessengerCreateInfoEXT create = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                    .messageSeverity(severity.bits())
                    .messageType(type.bits())
                    .pfnUserCallback(callback);
            verifyExtensionMethod(instance.getNativeObject(), "vkCreateDebugUtilsMessengerEXT");
            object = getLong(stack, ptr -> vkCreateDebugUtilsMessengerEXT(instance.getNativeObject(), create, null, ptr));
        }
        ref = DisposableManager.reference(this);
        instance.getReference().addDependent(ref);
    }

    private int message(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
        VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
        Severity severity = Severity.getEnum(messageSeverity);
        if (severity == Severity.Error) {
            throw new RuntimeException(data.pMessageString());
        } else {
            System.err.println(severity.name() + "  " + data.pMessageString());
        }
        return VK_FALSE; // always return false, true is only really used for testing validation layers
    }

    @Override
    public Runnable createDestroyer() {
        return () -> {
            verifyExtensionMethod(instance.getNativeObject(), "vkDestroyDebugUtilsMessengerEXT");
            vkDestroyDebugUtilsMessengerEXT(instance.getNativeObject(), object, null);
            callback.close();
        };
    }

}
