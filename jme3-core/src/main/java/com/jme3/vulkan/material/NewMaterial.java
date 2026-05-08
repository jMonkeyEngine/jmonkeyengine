package com.jme3.vulkan.material;

import com.jme3.material.RenderState;
import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.buffers.DirectBufferMapping;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.material.experimental.ShaderInterface;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.material.technique.PushConstantRange;
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

    private final Map<String, Object> parameters = new HashMap<>();
    private final Set<Class<? extends ShaderInterface>> interfaces = new HashSet<>();
    private final Map<DescriptorSetLayout, CachedDescriptorSet> setCache = new HashMap<>();
    private final RenderState renderState = new RenderState();

    @Override
    public void bind(CommandBuffer cmd, Pipeline pipeline, DescriptorPool pool) {
        PipelineLayout layout = pipeline.getLayout();
        List<DescriptorSetLayout> descLayouts = layout.getSetLayouts();
        List<DescriptorSetLayout> needsAlloc = new ArrayList<>(descLayouts.size());
        // find layouts without an allocated descriptor set
        for (DescriptorSetLayout l : descLayouts) {
            if (!setCache.containsKey(l)) {
                needsAlloc.add(l);
            }
        }
        // allocate missing descriptor sets
        if (!needsAlloc.isEmpty()) {
            DescriptorSet[] allocatedSets = pool.allocateSets(needsAlloc);
            for (ListIterator<DescriptorSetLayout> it = needsAlloc.listIterator(); it.hasNext();) {
                // save new descriptor sets to cache
                setCache.put(it.next(), new CachedDescriptorSet(allocatedSets[it.previousIndex()]));
            }
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer setsToBind = stack.mallocLong(descLayouts.size());
            // write changes to descriptor sets
            for (DescriptorSetLayout descLayout : descLayouts) {
                CachedDescriptorSet set = setCache.get(descLayout);
                // write uniforms to descriptor set
                for (Map.Entry<String, UniformBinding> binding : descLayout.getBindings().entrySet()) {
                    DescriptorSetWriter writer = binding.getValue().createWriter(parameters.get(binding.getKey()));
                    if (writer == null) {
                        continue;
                    }
                    set.stageWriter(binding.getKey(), writer);
                }
                set.writeChanges();
                setsToBind.put(set.getSet().getNativeObject());
            }
            // bind descriptor sets
            vkCmdBindDescriptorSets(cmd.getBuffer(), pipeline.getBindPoint().getEnum(), layout.getNativeObject(), 0, setsToBind.flip(), null);
            // upload push constants
            if (!layout.getPushConstants().isEmpty()) {
                BufferMapping push = new DirectBufferMapping(stack.malloc(layout.getPushConstantBytes()));
                for (PushConstantRange constant : layout.getPushConstants()) {
                    VulkanUniform uniform = uniforms.get(constant.getName());
                    if (uniform == null) {
                        throw new NullPointerException("Uniform \"" + constant.getName() + "\" does not exist as requested by layout push constants.");
                    }
                    uniform.fillPushConstantsBuffer(cmd, constant, push);
                }
                vkCmdPushConstants(cmd.getBuffer(), layout.getNativeObject(), ShaderStage.All.bits(), 0, push.getBytes());
            }
        }
    }

    @Override
    public void setParameter(String name, Object value) {
        parameters.put(name, value);
    }

    @Override
    public <P> P getParameter(String name) {
        return (P)parameters.get(name);
    }

    @Override
    public void enableInterface(Class<? extends ShaderInterface> interfaceType, boolean enable) {
        if (enable) interfaces.add(interfaceType);
        else interfaces.remove(interfaceType);
    }

    @Override
    public boolean isInterfaceEnabled(Class<? extends ShaderInterface> interfaceType) {
        return interfaces.contains(interfaceType);
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
