package com.jme3.vulkan.pipelines;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.VulkanObject;
import com.jme3.vulkan.devices.LogicalDevice;

import static org.lwjgl.vulkan.VK10.*;

public abstract class Pipeline extends VulkanObject<Long> {

    public static final String DEFAULT_SHADER_ENTRY_POINT = "main";

    protected final LogicalDevice<?> device;
    protected final PipelineBindPoint bindPoint;
    protected final PipelineLayout layout;

    public Pipeline(LogicalDevice<?> device, PipelineBindPoint bindPoint, PipelineLayout layout) {
        this.device = device;
        this.bindPoint = bindPoint;
        this.layout = layout;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyPipeline(device.getNativeObject(), object, null);
    }

    public void bind(CommandBuffer cmd) {
        vkCmdBindPipeline(cmd.getBuffer(), bindPoint.getVkEnum(), object);
    }

    public LogicalDevice<?> getDevice() {
        return device;
    }

    public PipelineBindPoint getBindPoint() {
        return bindPoint;
    }

    public PipelineLayout getLayout() {
        return layout;
    }

}
