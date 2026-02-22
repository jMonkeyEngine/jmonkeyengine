package com.jme3.vulkan.devices;

import com.jme3.vulkan.commands.CommandQueue;

public interface GraphicalDevice extends PhysicalDevice {

   CommandQueue getGraphics();

}
