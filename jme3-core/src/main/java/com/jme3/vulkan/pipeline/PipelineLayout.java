package com.jme3.vulkan.pipeline;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.CacheableNativeBuilder;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.technique.PushConstantRange;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;

import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class PipelineLayout extends AbstractNative<Long> {

    private final LogicalDevice<?> device;
    private final List<DescriptorSetLayout> layouts = new ArrayList<>();
    private final List<PushConstantRange> pushConstants = new ArrayList<>();
    private int pushConstantBytes;

    protected PipelineLayout(LogicalDevice<?> device) {
        this.device = device;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vkDestroyPipelineLayout(device.getNativeObject(), object, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PipelineLayout that = (PipelineLayout)o;
        return Objects.equals(device, that.device) && Objects.equals(layouts, that.layouts);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(layouts);
    }

    public List<DescriptorSetLayout> getSetLayouts() {
        return Collections.unmodifiableList(layouts);
    }

    public List<PushConstantRange> getPushConstants() {
        return Collections.unmodifiableList(pushConstants);
    }

    public int getPushConstantBytes() {
        return pushConstantBytes;
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
            if (!pushConstants.isEmpty()) {
                pushConstantBytes = 0;
                VkPushConstantRange.Buffer ranges = VkPushConstantRange.malloc(pushConstants.size(), stack);
                for (PushConstantRange p : pushConstants) {
                    ranges.get().stageFlags(p.getScope().bits())
                            .offset(p.getOffset())
                            .size(p.getSize());
                    pushConstantBytes += p.getSize();
                }
                int limit = device.getPhysicalDevice().getProperties().limits().maxPushConstantsSize();
                if (pushConstantBytes > limit) {
                    throw new IllegalStateException("Only up to " + limit + " bytes can be used by push constants.");
                }
                create.pPushConstantRanges(ranges.flip());
            }
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

        public void addUniformSet(DescriptorSetLayout layout) {
            layouts.add(layout);
        }

        public void addUniformSet(Consumer<DescriptorSetLayout.Builder> config) {
            layouts.add(DescriptorSetLayout.build(device, config));
        }

        public void addPushConstants(PushConstantRange range) {
            pushConstants.add(range);
        }

        public void addPushConstants(Collection<PushConstantRange> constants) {
            pushConstants.addAll(constants);
        }

    }

}
