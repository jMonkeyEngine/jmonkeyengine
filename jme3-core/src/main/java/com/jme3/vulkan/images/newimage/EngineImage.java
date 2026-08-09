package com.jme3.vulkan.images.newimage;

import com.jme3.math.Vector3i;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.images.GpuImage;
import com.jme3.vulkan.images.ImageRoles;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

public interface EngineImage {

    long getHandle();

    GpuImage.Type getType();

    Format getFormat();

    VulkanImage.Layout getLayout();

    Vector3i getSize();

    int getSamples();

    int getMipLevels();

    int getArrayLayers();

    VulkanImage.Tiling getTiling();

    Flag<ImageRoles> getRoles();

    Flag<MemoryProp> getMemoryProperties();

    void transitionLayout(CommandBuffer cmd, VulkanImage.Layout layout);

}
