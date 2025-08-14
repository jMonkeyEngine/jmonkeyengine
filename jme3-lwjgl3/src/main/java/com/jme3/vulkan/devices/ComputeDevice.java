package com.jme3.vulkan.devices;

import com.jme3.vulkan.commands.Queue;

public interface ComputeDevice extends PhysicalDevice {

    Queue getCompute();

}
