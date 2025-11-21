package com.jme3.backend;

import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.mesh.InputRate;
import com.jme3.vulkan.mesh.VertexBinding;
import com.jme3.vulkan.mesh.VulkanVertexBinding;
import com.jme3.vulkan.util.IntEnum;

public class VulkanEngine implements Engine {

    private final LogicalDevice<?> device;
    private final int frames;

    public VulkanEngine(LogicalDevice<?> device, int frames) {
        this.device = device;
        this.frames = frames;
    }

    @Override
    public VertexBinding.Builder createMeshVertexBinding(IntEnum<InputRate> rate) {
        return VulkanVertexBinding.create(device, rate);
    }

}
