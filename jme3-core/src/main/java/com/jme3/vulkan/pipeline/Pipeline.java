package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.util.natives.AbstractNative;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pipeline.states.BasePipelineState;
import com.jme3.vulkan.pipeline.states.PipelineState;
import com.jme3.vulkan.util.Flag;

import java.util.concurrent.atomic.AtomicLong;

import static org.lwjgl.vulkan.VK10.*;

public abstract class Pipeline extends AbstractNative<Long> {

    public static final String DEFAULT_SHADER_ENTRY_POINT = "main";
    private static final AtomicLong nextSortId = new AtomicLong(0L);

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
    protected final PipelineLayout layout;
    protected final PipelineBindPoint bindPoint;
    protected final Pipeline parent;
    private final long sortId;

    public Pipeline(LogicalDevice<?> device, PipelineLayout layout, PipelineBindPoint bindPoint, Pipeline parent) {
        this.device = device;
        this.layout = layout;
        this.bindPoint = bindPoint;
        this.parent = parent;
        this.sortId = nextSortId.getAndIncrement();
    }

    public abstract BasePipelineState<?, ?> getState();

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyPipeline(device.getNativeObject(), object, null);
    }

    public void bind(CommandBuffer cmd) {
        vkCmdBindPipeline(cmd.getBuffer(), bindPoint.getEnum(), object);
    }

    public boolean isMaterialEquivalent(Pipeline other) {
        return this == other || (other != null && bindPoint.is(other.bindPoint) && getLayout() == other.getLayout());
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

    public Pipeline getParent() {
        return parent;
    }

    public long getSortId() {
        return sortId;
    }

}