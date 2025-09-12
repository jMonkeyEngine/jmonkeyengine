package com.jme3.vulkan.surface;

public interface SwapchainUpdater {

    boolean swapchainOutOfDate(Swapchain swapchain, int imageAcquireCode);

}
