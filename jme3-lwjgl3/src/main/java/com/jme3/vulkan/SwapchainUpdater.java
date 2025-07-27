package com.jme3.vulkan;

public interface SwapchainUpdater {

    boolean swapchainOutOfDate(Swapchain swapchain, int imageAcquireCode);

}
