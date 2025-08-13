package com.jme3.vulkan.material;

import com.jme3.util.IntMap;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.pipelines.Pipeline;
import org.lwjgl.system.MemoryStack;

import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Relates shader uniform values to sets and bindings.
 */
public class Material {

    private final Pipeline pipeline;
    private final DescriptorSet[] sets;
    private final Map<String, Uniform<?>> uniforms = new HashMap<>();

    public Material(Pipeline pipeline, DescriptorPool pool, Collection<Uniform<?>> uniforms) {
        this.pipeline = pipeline;
        for (Uniform u : uniforms) {
            if (this.uniforms.put(u.getName(), u) != null) {
                throw new IllegalArgumentException("Duplicate uniform name: " + u.getName());
            }
        }
        this.sets = allocateSets(pipeline, pool, uniforms);
    }

    private DescriptorSet[] allocateSets(Pipeline pipeline, DescriptorPool pool, Collection<Uniform<?>> uniforms) {
        // group uniforms by set index
        IntMap<List<Uniform<?>>> uniformsBySet = new IntMap<>();
        for (Uniform u : uniforms) {
            List<Uniform<?>> set = uniformsBySet.get(u.getSetIndex());
            if (set == null) {
                set = new ArrayList<>();
                uniformsBySet.put(u.getSetIndex(), set);
            }
            set.add(u);
        }
        // there must be enough layouts for all sets to have a different layout
        if (uniformsBySet.size() > pipeline.getLayout().getDescriptorSetLayouts().length) {
            throw new IllegalArgumentException("Pipeline layout does not contain enough descriptor set layouts.");
        }
        List<DescriptorSetLayout> availableLayouts = new LinkedList<>(
                Arrays.asList(pipeline.getLayout().getDescriptorSetLayouts()));
        List<DescriptorSetLayout> matchedLayouts = new ArrayList<>(uniformsBySet.size());
        // for each set definition, find a set layout that matches the definition
        setLoop: for (IntMap.Entry<List<Uniform<?>>> set : uniformsBySet) {
            // Search for a layout that is compatible with the set definition
            layoutLoop: for (Iterator<DescriptorSetLayout> it = availableLayouts.iterator(); it.hasNext();) {
                DescriptorSetLayout layout = it.next();
                requiredLoop: for (Uniform u : set.getValue()) {
                    // find a layout binding that matches the requirement
                    for (SetLayoutBinding available : layout.getBindings()) {
                        if (u.isBindingCompatible(available)) {
                            // Assign this binding to the uniform.
                            // todo: figure out a safer way to do this
                            u.setBinding(available);
                            continue requiredLoop;
                        }
                    }
                    // Layout does not contain a binding at the requested index.
                    // Layout is incompatible, try the next layout.
                    continue layoutLoop;
                }
                // Layout is compatible with the set definition
                matchedLayouts.add(layout);
                it.remove();
                continue setLoop;
            }
            // No layout is available that is compatible with the set definition
            throw new IllegalArgumentException("Set layout required by material is not supported by the pipeline.");
        }
        if (matchedLayouts.size() != uniformsBySet.size()) {
            // Not every set definition found a compatible layout. This error
            // should technically be reported by a previous error, but it
            // doesn't hurt to be totally sure.
            throw new IllegalArgumentException("Material requirements are not compatible with pipeline.");
        }
        return pool.allocateSets(matchedLayouts.toArray(new DescriptorSetLayout[0]));
    }

    public void bind(CommandBuffer cmd) {
        for (DescriptorSet set : sets) {
            set.update(); // write updates to the set
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer setBuf = stack.mallocLong(sets.length);
            for (DescriptorSet s : sets) {
                setBuf.put(s.getId());
            }
            setBuf.flip();
            vkCmdBindDescriptorSets(cmd.getBuffer(), pipeline.getBindPoint().getVkEnum(),
                    pipeline.getLayout().getNativeObject(), 0, setBuf, null);
        }
    }

    public Uniform get(String name) {
        return uniforms.get(name);
    }

}
