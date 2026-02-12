package com.jme3.vulkan.material;

import com.jme3.material.RenderState;
import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.buffers.VirtualBufferMapping;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.material.technique.NewTechnique;
import com.jme3.vulkan.material.technique.PushConstantRange;
import com.jme3.vulkan.material.technique.VulkanTechnique;
import com.jme3.vulkan.material.uniforms.Uniform;
import com.jme3.vulkan.material.uniforms.VulkanUniform;
import com.jme3.vulkan.pipeline.Pipeline;
import com.jme3.vulkan.pipeline.PipelineLayout;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Relates shader uniform values to shader descriptor sets and bindings.
 */
public class NewMaterial implements VulkanMaterial {

    private final Map<String, VulkanUniform> uniforms = new HashMap<>();
    private final Map<String, VulkanTechnique> techniques = new HashMap<>();
    private final Map<DescriptorSetLayout, CachedDescriptorSet> setCache = new HashMap<>();
    private final RenderState renderState = new RenderState();

    @Override
    public void bind(CommandBuffer cmd, Pipeline pipeline, DescriptorPool pool) {
        PipelineLayout layout = pipeline.getLayout();
        List<DescriptorSetLayout> descLayouts = layout.getSetLayouts();
        List<DescriptorSetLayout> needsAlloc = new ArrayList<>(descLayouts.size());
        for (DescriptorSetLayout l : descLayouts) {
            if (!setCache.containsKey(l)) {
                needsAlloc.add(l);
            }
        }
        if (!needsAlloc.isEmpty()) {
            DescriptorSet[] allocatedSets = pool.allocateSets(needsAlloc);
            for (ListIterator<DescriptorSetLayout> it = needsAlloc.listIterator(); it.hasNext();) {
                setCache.put(it.next(), new CachedDescriptorSet(allocatedSets[it.previousIndex()]));
            }
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer sets = stack.mallocLong(descLayouts.size());
            for (DescriptorSetLayout descLayout : descLayouts) {
                CachedDescriptorSet set = setCache.get(descLayout);
                for (Map.Entry<String, SetLayoutBinding> binding : descLayout.getBindings().entrySet()) {
                    VulkanUniform uniform = uniforms.get(binding.getKey());
                    if (!(uniform instanceof VulkanUniform)) { // uniform=null enters block
                        continue;
                    }
                    DescriptorSetWriter writer = uniform.createWriter(binding.getValue());
                    if (writer == null) {
                        continue;
                    }
                    set.stageWriter(binding.getKey(), writer);
                }
                set.writeChanges();
                sets.put(set.getSet().getNativeObject());
            }
            sets.flip();
            vkCmdBindDescriptorSets(cmd.getBuffer(), pipeline.getBindPoint().getEnum(), layout.getNativeObject(), 0, sets, null);
            if (!layout.getPushConstants().isEmpty()) {
                BufferMapping push = new VirtualBufferMapping(stack.malloc(layout.getPushConstantBytes()));
                for (PushConstantRange constant : layout.getPushConstants()) {
                    VulkanUniform uniform = uniforms.get(constant.getName());
                    if (uniform == null) {
                        throw new NullPointerException("Uniform \"" + constant.getName() + "\" does not exist as requested by layout push constants.");
                    }
                    uniform.fillPushConstantsBuffer(constant, push);
                }
                vkCmdPushConstants(cmd.getBuffer(), layout.getNativeObject(), ShaderStage.All.bits(), 0, push.getBytes());
            }
        }
    }

    @Override
    public <T extends Uniform> T setUniform(String name, T uniform) {
        if (!(uniform instanceof VulkanUniform)) { // uniform=null enters block
            throw new ClassCastException("Expected " + VulkanUniform.class + ", found " + uniform.getClass());
        }
        uniforms.put(name, (VulkanUniform)uniform);
        return uniform;
    }

    @Override
    public void setTechnique(String name, NewTechnique technique) {
        techniques.put(name, (VulkanTechnique)technique);
    }

    @Override
    public <T extends Uniform> T getUniform(String name) {
        return (T)uniforms.get(name);
    }

    @Override
    public VulkanTechnique getTechnique(String name) {
        return techniques.get(name);
    }

    @Override
    public RenderState getAdditionalRenderState() {
        return renderState;
    }

    protected static class CachedDescriptorSet {

        private final DescriptorSet set;
        private final Map<String, DescriptorSetWriter> writers = new HashMap<>();
        private final Map<String, DescriptorSetWriter> changes = new HashMap<>();

        public CachedDescriptorSet(DescriptorSet set) {
            this.set = set;
        }

        public void stageWriter(String name, DescriptorSetWriter writer) {
            if (!Objects.equals(writers.put(name, writer), writer)) {
                changes.put(name, writer);
            }
        }

        public void writeChanges() {
            set.write(changes.values());
            changes.clear();
        }

        public DescriptorSet getSet() {
            return set;
        }

    }

}
