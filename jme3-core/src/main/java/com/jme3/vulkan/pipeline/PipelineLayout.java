package com.jme3.vulkan.pipeline;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.CacheableNativeBuilder;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;

import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class PipelineLayout extends AbstractNative<Long> {

    private final LogicalDevice<?> device;
    private final List<DescriptorSetLayout> layouts = new ArrayList<>();

    protected PipelineLayout(LogicalDevice<?> device) {
        this.device = device;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyPipelineLayout(device.getNativeObject(), object, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PipelineLayout that = (PipelineLayout) o;
        return Objects.equals(device, that.device) && Objects.equals(layouts, that.layouts);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(layouts);
    }

    public List<DescriptorSetLayout> getSetLayouts() {
        return Collections.unmodifiableList(layouts);
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

        @Override
        protected void construct() {
            LongBuffer layoutBuf = stack.mallocLong(layouts.size());
            for (DescriptorSetLayout l : layouts) {
                layoutBuf.put(l.getNativeObject());
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
            ref = Native.get().register(PipelineLayout.this);
            device.getNativeReference().addDependent(ref);
        }

        @Override
        protected PipelineLayout getBuildTarget() {
            return PipelineLayout.this;
        }

        public void nextUniformSet(DescriptorSetLayout layout) {
            layouts.add(layout);
        }

        public void nextUniformSet(Consumer<DescriptorSetLayout.Builder> config) {
            layouts.add(DescriptorSetLayout.build(device, config));
        }

    }

    private static class LayoutCount extends ArrayList<DescriptorSetLayout> {

        private int material = 0;

        @Override
        public boolean add(DescriptorSetLayout l) {
            if (++material > size()) {
                return super.add(l);
            }
            return false;
        }

        public void reset() {
            material = 0;
        }

    }

}
