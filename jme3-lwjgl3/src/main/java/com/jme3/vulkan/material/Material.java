package com.jme3.vulkan.material;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.material.uniforms.Uniform;
import com.jme3.vulkan.material.uniforms.UniformSet;
import com.jme3.vulkan.pipelines.Pipeline;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Relates shader uniform values to sets and bindings.
 */
public class Material {

    private final DescriptorPool pool;
    private final List<UniformSet> uniforms = new ArrayList<>();
    private final HashMap<String, Uniform<?>> uniformLookup = new HashMap<>();

    public Material(DescriptorPool pool) {
        this.pool = pool;
    }

    public void bind(CommandBuffer cmd, Pipeline pipeline) {
        bind(cmd, pipeline, 0);
    }

    public void bind(CommandBuffer cmd, Pipeline pipeline, int offset) {
        LinkedList<DescriptorSetLayout> availableLayouts = new LinkedList<>(
                Arrays.asList(pipeline.getLayout().getDescriptorSetLayouts()));
        ArrayList<DescriptorSetLayout> allocationLayouts = new ArrayList<>(availableLayouts.size());
        ArrayList<UniformSet> allocationTargets = new ArrayList<>(availableLayouts.size());
        for (UniformSet set : uniforms) {
            set.update(pipeline.getDevice());
            // Select an existing descriptor set to be active. If
            // no existing set may be selected, a set layout is
            // returned with which to allocate a new descriptor set.
            DescriptorSetLayout allocation = set.selectExistingActiveSet(availableLayouts);
            if (allocation != null) {
                // allocate all new sets at once
                allocationLayouts.add(allocation);
                allocationTargets.add(set);
            }
        }
        if (!allocationLayouts.isEmpty()) {
            if (allocationLayouts.size() != allocationTargets.size()) {
                throw new IllegalStateException("Each layout must have a corresponding target uniform set.");
            }
            DescriptorSet[] allocatedSets = pool.allocateSets(
                    allocationLayouts.toArray(new DescriptorSetLayout[0]));
            for (int i = 0; i < allocatedSets.length; i++) {
                UniformSet target = allocationTargets.get(i);
                target.addActiveSet(allocatedSets[i]);
                allocatedSets[i].write(target.getUniforms());
            }
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer setBuf = stack.mallocLong(uniforms.size());
            for (UniformSet s : uniforms) {
                setBuf.put(s.getActiveSet().getId());
            }
            setBuf.flip();
            vkCmdBindDescriptorSets(cmd.getBuffer(), pipeline.getBindPoint().getVkEnum(),
                    pipeline.getLayout().getNativeObject(), offset, setBuf, null);
        }
    }

    protected UniformSet addSet(UniformSet set) {
        uniforms.add(set);
        return set;
    }

    protected UniformSet addSet(Uniform... uniforms) {
        return addSet(new UniformSet(uniforms));
    }

    public List<UniformSet> getSets() {
        return Collections.unmodifiableList(uniforms);
    }

    @SuppressWarnings("unchecked")
    public <T extends Uniform> T get(String name) {
        // Not sure if caching results are really worth it...
        Uniform<?> uniform = uniformLookup.get(name);
        if (uniform != null) {
            return (T)uniform;
        }
        for (UniformSet set : uniforms) {
            for (Uniform<?> u : set.getUniforms()) {
                if (name.equals(u.getName())) {
                    uniformLookup.put(u.getName(), u);
                    return (T)u;
                }
            }
        }
        return null;
    }

}
