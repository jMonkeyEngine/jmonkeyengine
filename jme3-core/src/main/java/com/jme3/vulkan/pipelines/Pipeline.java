package com.jme3.vulkan.pipelines;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.util.natives.AbstractNative;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;

public abstract class Pipeline extends AbstractNative<Long> {

    public static final String DEFAULT_SHADER_ENTRY_POINT = "main";

    public enum Create implements Flag<Create> {

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
    protected final PipelineLayout layout;
    protected Flag<Create> flags = Flag.empty();
    protected Pipeline parent;

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
        vkCmdBindPipeline(cmd.getBuffer(), bindPoint.getEnum(), object);
    }

    public boolean isMaterialEquivalent(Pipeline other) {
        return this == other || (other != null && bindPoint.is(other.bindPoint) && layout == other.layout);
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

    public Flag<Create> getFlags() {
        return flags;
    }

    public Pipeline getParent() {
        return parent;
    }

}