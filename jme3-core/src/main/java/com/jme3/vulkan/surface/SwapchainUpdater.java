package com.jme3.vulkan.surface;

public interface SwapchainUpdater {

    boolean outOfDate(Swapchain swapchain, int imageAcquireCode);

    void update(Swapchain swapchain);

}
