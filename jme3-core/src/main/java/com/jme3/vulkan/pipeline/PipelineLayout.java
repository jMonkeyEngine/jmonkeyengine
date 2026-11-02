package com.jme3.vulkan.pipeline;

import com.jme3.util.destruction.Destructable;
import com.jme3.util.destruction.ResourceLifecycle;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.vkCreatePipelineLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;

/**
 * The descriptor set layouts and push constant ranges a pipeline is created against. Immutable
 * once created.
 */
public class PipelineLayout implements Destructable {

    private final LogicalDevice<?> device;
    private final List<DescriptorSetLayout> setLayouts;
    private final List<PushConstantRange> pushConstants;
    private final long handle;
    private final ResourceLifecycle lifecycle;

    /**
     * @param device device to create the layout on
     * @param setLayouts descriptor set layouts ordered by set number, without gaps, e.g. a
     *                   shader's {@link com.jme3.vulkan.shadergen.Shader#getSetLayouts() set layouts}
     * @param pushConstants push constant ranges, e.g. a shader's
     *                      {@link com.jme3.vulkan.shadergen.Shader#getPushConstantRanges() ranges}
     * @throws IllegalArgumentException if a set layout is null, there are more set layouts than the
     *                                  device can bind, a push constant range goes past the device's
     *                                  {@code maxPushConstantsSize}, or two ranges share a stage
     */
    public PipelineLayout(LogicalDevice<?> device, List<DescriptorSetLayout> setLayouts, List<PushConstantRange> pushConstants) {
        this.device = device;
        this.setLayouts = Collections.unmodifiableList(new ArrayList<>(setLayouts));
        this.pushConstants = Collections.unmodifiableList(new ArrayList<>(pushConstants));
        validate();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer layoutBuf = stack.mallocLong(this.setLayouts.size());
            for (DescriptorSetLayout l : this.setLayouts) {
                layoutBuf.put(l.getHandle());
            }
            VkPipelineLayoutCreateInfo create = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType$Default()
                    .pSetLayouts(layoutBuf.flip());
            if (!this.pushConstants.isEmpty()) {
                VkPushConstantRange.Buffer ranges = VkPushConstantRange.malloc(this.pushConstants.size(), stack);
                for (PushConstantRange r : this.pushConstants) {
                    r.fill(ranges.get());
                }
                create.pPushConstantRanges(ranges.flip());
            }
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreatePipelineLayout(device.getHandle(), create, null, idBuf), "Failed to create pipeline layout.");
            handle = idBuf.get(0);
        }
        // the destroyer must not reference this, or the leak backstop could never see it
        VkDevice deviceHandle = device.getHandle();
        long h = handle;
        lifecycle = new ResourceLifecycle(this, () -> vkDestroyPipelineLayout(deviceHandle, h, null));
        device.getLifecycle().addDependent(lifecycle);
    }

    private void validate() {
        int maxSets = device.getPhysicalLimits().maxBoundDescriptorSets();
        if (setLayouts.size() > maxSets) {
            throw new IllegalArgumentException(setLayouts.size() + " set layouts exceed the device's maximum of " + maxSets + ".");
        }
        for (int i = 0; i < setLayouts.size(); i++) {
            if (setLayouts.get(i) == null) {
                throw new IllegalArgumentException("Set layout " + i + " is null; use an empty layout for unused sets.");
            }
        }
        int maxPush = device.getPhysicalLimits().maxPushConstantsSize();
        for (int i = 0; i < pushConstants.size(); i++) {
            PushConstantRange range = pushConstants.get(i);
            if (range.getEnd() > maxPush) {
                throw new IllegalArgumentException(range + " exceeds the device's maximum push constant size of " + maxPush + " bytes.");
            }
            for (int j = 0; j < i; j++) {
                if ((pushConstants.get(j).getStages().bits() & range.getStages().bits()) != 0) {
                    throw new IllegalArgumentException(range + " shares a stage with " + pushConstants.get(j) + ".");
                }
            }
        }
    }

    @Override
    public ResourceLifecycle getLifecycle() {
        return lifecycle;
    }

    public long getHandle() {
        return handle;
    }

    public LogicalDevice<?> getDevice() {
        return device;
    }

    /**
     * @return descriptor set layouts, indexed by set number
     */
    public List<DescriptorSetLayout> getSetLayouts() {
        return setLayouts;
    }

    public DescriptorSetLayout getSetLayout(int set) {
        return setLayouts.get(set);
    }

    /**
     * @return push constant ranges, in the order given
     */
    public List<PushConstantRange> getPushConstantRanges() {
        return pushConstants;
    }

}
