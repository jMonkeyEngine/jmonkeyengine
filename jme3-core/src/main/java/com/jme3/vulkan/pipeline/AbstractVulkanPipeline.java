package com.jme3.vulkan.pipeline;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.util.Flag;

import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;

public abstract class AbstractVulkanPipeline extends AbstractNative<Long> implements Pipeline {

    public static final String DEFAULT_SHADER_ENTRY_POINT = "main";
    private static final AtomicInteger nextSortId = new AtomicInteger(0);

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
    private final int sortId;

    public AbstractVulkanPipeline(LogicalDevice<?> device, PipelineBindPoint bindPoint) {
        this.device = device;
        this.bindPoint = bindPoint;
        this.sortId = nextSortId.getAndIncrement();
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyPipeline(device.getNativeObject(), object, null);
    }

    @Override
    public void bind(CommandBuffer cmd) {
        vkCmdBindPipeline(cmd.getBuffer(), bindPoint.getEnum(), object);
    }

    @Override
    public boolean isMaterialEquivalent(Pipeline other) {
        if (!(other instanceof AbstractVulkanPipeline)) return false;
        AbstractVulkanPipeline that = (AbstractVulkanPipeline)other;
        return this == that || (bindPoint.is(other.getBindPoint()) && getLayout() == that.getLayout());
    }

    @Override
    public PipelineBindPoint getBindPoint() {
        return bindPoint;
    }

    @Override
    public int getSortId() {
        return sortId;
    }

    public LogicalDevice<?> getDevice() {
        return device;
    }

}
