package com.jme3.vulkan.devices;

import com.jme3.vulkan.commands.Queue;

public interface GraphicalDevice extends PhysicalDevice {

   Queue getGraphics();

}
