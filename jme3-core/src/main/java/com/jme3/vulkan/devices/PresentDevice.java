package com.jme3.vulkan.devices;

import com.jme3.vulkan.commands.Queue;

public interface PresentDevice extends PhysicalDevice {

    Queue getPresent();

}
