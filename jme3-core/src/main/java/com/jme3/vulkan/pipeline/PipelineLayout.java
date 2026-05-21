package com.jme3.vulkan.pipeline;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.CacheableNativeBuilder;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;

import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class PipelineLayout extends AbstractNative<Long> {

    private final LogicalDevice<?> device;
    private final DescriptorSetLayout[] layouts;

    protected PipelineLayout(LogicalDevice<?> device) {
        this.device = device;
        this.layouts = new DescriptorSetLayout[device.getPhysicalLimits().maxBoundDescriptorSets()];
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vkDestroyPipelineLayout(device.getNativeObject(), object, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PipelineLayout that = (PipelineLayout)o;
        return device == that.device && Arrays.equals(layouts, that.layouts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(device), Arrays.hashCode(layouts));
    }

    public DescriptorSetLayout getSetLayout(int location) {
        return layouts[location];
    }

    public LogicalDevice<?> getDevice() {
        return device;
    }

    public Builder build() {
        return new Builder();
    }

    public static PipelineLayout build(LogicalDevice<?> device, Consumer<Builder> config) {
        Builder b = new PipelineLayout(device).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends CacheableNativeBuilder<PipelineLayout, PipelineLayout> {

        private int maxLayoutIndex = -1;

        @Override
        protected void construct() {
            LongBuffer layoutBuf = stack.mallocLong(maxLayoutIndex - 1);
            DescriptorSetLayout dummy = DescriptorSetLayout.nullLayout();
            for (int i = 0; i <= maxLayoutIndex; i++) {
                layoutBuf.put((layouts[i] != null ? layouts[i] : dummy).getNativeObject());
            }
            layoutBuf.flip();
            VkPipelineLayoutCreateInfo create = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .setLayoutCount(layoutBuf.limit())
                    .pSetLayouts(layoutBuf);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreatePipelineLayout(device.getNativeObject(), create, null, idBuf),
                    "Failed to create pipeline.");
            object = idBuf.get(0);
            ref = DisposableManager.reference(PipelineLayout.this);
            device.getReference().addDependent(ref);
        }

        @Override
        protected PipelineLayout getBuildTarget() {
            return PipelineLayout.this;
        }

        public void addSetLayout(int location, DescriptorSetLayout layout) {
            layouts[location] = layout;
            maxLayoutIndex = Math.max(maxLayoutIndex, location);
        }

    }

}
