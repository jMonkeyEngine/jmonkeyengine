package com.jme3.vulkan.material;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.uniforms.Uniform;
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
    private final List<UniformSet> uniformSets = new ArrayList<>();
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
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer setBuf = stack.mallocLong(uniformSets.size());
            for (UniformSet set : uniformSets) {
                setBuf.put(set.acquireSet(pipeline.getDevice(), pool, availableLayouts).getNativeObject());
            }
            setBuf.flip();
            vkCmdBindDescriptorSets(cmd.getBuffer(), pipeline.getBindPoint().getVkEnum(),
                    pipeline.getLayout().getNativeObject(), offset, setBuf, null);
        }
    }

    public DescriptorSetLayout[] createLayouts(LogicalDevice<?> device) {
        return uniformSets.stream().map(u -> u.createLayout(device)).toArray(DescriptorSetLayout[]::new);
    }

    protected UniformSet addSet(UniformSet set) {
        uniformSets.add(set);
        return set;
    }

    protected UniformSet addSet(int setIndex, UniformSet set) {
        uniformSets.add(setIndex, set);
        return set;
    }

    protected UniformSet addSet(Uniform... uniforms) {
        return addSet(new UniformSet(uniforms));
    }

    public List<UniformSet> getSets() {
        return Collections.unmodifiableList(uniformSets);
    }

    @SuppressWarnings("unchecked")
    public <T extends Uniform> T get(String name) {
        // Not sure if caching the results is really worth it...
        Uniform<?> uniform = uniformLookup.get(name);
        if (uniform != null) {
            return (T)uniform;
        }
        for (UniformSet set : uniformSets) {
            for (Uniform<?> u : set) {
                if (name.equals(u.getName())) {
                    uniformLookup.put(u.getName(), u);
                    return (T)u;
                }
            }
        }
        return null;
    }

}
