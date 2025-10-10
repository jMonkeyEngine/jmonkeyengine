package com.jme3.vulkan.pipelines;

import com.jme3.util.AbstractBuilder;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.NewMaterial;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;

import java.nio.LongBuffer;
import java.util.*;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class PipelineLayout extends AbstractNative<Long> {

    private final LogicalDevice<?> device;
    private Collection<DescriptorSetLayout> layouts;

    public PipelineLayout(LogicalDevice<?> device, DescriptorSetLayout... layouts) {
        this.device = device;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyPipelineLayout(device.getNativeObject(), object, null);
    }

    public Collection<DescriptorSetLayout> getDescriptorSetLayouts() {
        return Collections.unmodifiableCollection(layouts);
    }

    public Builder build() {
        return new Builder();
    }

    public class Builder extends AbstractBuilder {

        private final Collection<DescriptorSetLayout> descriptorLayouts = new ArrayList<>();
        private final Collection<NewMaterial> supportedMaterials = new ArrayList<>();

        @Override
        protected void build() {
            Collection<DescriptorSetLayout> descriptors = createDescriptors();
            LongBuffer layoutBuf = stack.mallocLong(descriptors.size());
            for (DescriptorSetLayout l : descriptors) {
                layoutBuf.put(l.build(stack).getNativeObject());
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

        private Collection<DescriptorSetLayout> createDescriptors() {
            // The number of duplicates of a particular layout to return is the
            // maximum of those duplicates that come from any material.
            Map<DescriptorSetLayout, LayoutCount> layoutBuckets = new HashMap<>();
            int usedLayouts = 0;
            for (NewMaterial m : supportedMaterials) {
                DescriptorSetLayout[] layouts = m.createLayouts(device);
                for (DescriptorSetLayout l : layouts) {
                    LayoutCount c = layoutBuckets.get(l);
                    if (c == null) {
                        layoutBuckets.put(l, c = new LayoutCount());
                    }
                    if (c.add(l)) {
                        usedLayouts++;
                    }
                }
                layoutBuckets.values().forEach(LayoutCount::reset);
            }
            layouts.clear();
            layouts = new ArrayList<>(usedLayouts + descriptorLayouts.size());
            layouts.addAll(descriptorLayouts);
            layoutBuckets.values().forEach(layouts::addAll);
            return layouts;
        }

        public void addDescriptorLayout(DescriptorSetLayout layout) {
            descriptorLayouts.add(layout);
        }

        public void addMaterial(NewMaterial material) {
            supportedMaterials.add(material);
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
