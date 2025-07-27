package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

public interface Image extends Native<Long> {

    ImageView createView(VkImageViewCreateInfo create);

    LogicalDevice getDevice();

}
