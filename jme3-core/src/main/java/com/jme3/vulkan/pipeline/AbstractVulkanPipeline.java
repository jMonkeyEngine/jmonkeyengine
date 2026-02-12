package com.jme3.vulkan.pipeline;

import com.jme3.util.natives.AbstractNative;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;

public abstract class AbstractVulkanPipeline extends AbstractNative<Long> implements Pipeline {

    public enum Create implements Flag<AbstractVulkanPipeline.Create> {

        Derivative(VK_PIPELINE_CREATE_DERIVATIVE_BIT),
        AllowDerivatives(VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT);

        private final int bits;

        Create(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

    }

    protected final LogicalDevice<?> device;
    protected final PipelineBindPoint bindPoint;

    public AbstractVulkanPipeline(LogicalDevice<?> device, PipelineBindPoint bindPoint) {
        this.device = device;
        this.bindPoint = bindPoint;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vkDestroyPipeline(device.getNativeObject(), object, null);
    }

    @Override
    public void bind(CommandBuffer cmd) {
        vkCmdBindPipeline(cmd.getBuffer(), bindPoint.getEnum(), object);
    }

    @Override
    public PipelineBindPoint getBindPoint() {
        return bindPoint;
    }

    @Override
    public long getSortId() {
        return object;
    }

    public LogicalDevice<?> getDevice() {
        return device;
    }

}
