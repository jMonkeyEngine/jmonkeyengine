package com.jme3.vulkan.util;

import static org.lwjgl.vulkan.VK14.*;

public class ErrorCodes {

    public static String findName(int errorCode) {
        switch (errorCode) {
            case VK_ERROR_OUT_OF_DEVICE_MEMORY: return "VK_ERROR_OUT_OF_DEVICE_MEMORY";
            case VK_ERROR_OUT_OF_HOST_MEMORY: return "VK_ERROR_OUT_OF_HOST_MEMORY";
            case VK_ERROR_INITIALIZATION_FAILED: return "VK_ERROR_INITIALIZATION_FAILED";
            case VK_ERROR_EXTENSION_NOT_PRESENT: return "VK_ERROR_EXTENSION_NOT_PRESENT";
            case VK_ERROR_UNKNOWN: return "VK_ERROR_UNKNOWN";
            default: return Integer.toString(errorCode);
        }
    }

}
