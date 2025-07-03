package com.jme3.vulkan;

import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

public interface SwapchainSupport {

    VkSurfaceFormatKHR selectFormat();

    VkExtent2D selectExtent();

    int selectMode();

    int selectImageCount();

    boolean isSupported();

}
