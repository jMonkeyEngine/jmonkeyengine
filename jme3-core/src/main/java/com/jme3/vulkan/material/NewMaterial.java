package com.jme3.vulkan.material;

import com.jme3.dev.NotFullyImplemented;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.texture.Texture;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.uniforms.BufferUniform;
import com.jme3.vulkan.material.uniforms.TextureUniform;
import com.jme3.vulkan.material.uniforms.Uniform;
import com.jme3.vulkan.mesh.MeshDescription;
import com.jme3.vulkan.pipeline.Pipeline;
import com.jme3.vulkan.pipeline.cache.PipelineCache;
import com.jme3.vulkan.pipeline.states.BasePipelineState;
import com.jme3.vulkan.pipeline.states.PipelineState;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Relates shader uniform values to sets and bindings.
 */
public class NewMaterial implements VkMaterial {

    private final DescriptorPool pool;
    private final Map<Integer, UniformSet> uniformSets = new HashMap<>();
    private final Map<String, Uniform<?>> uniformLookup = new HashMap<>();
    private final BitSet usedSetSlots = new BitSet();
    private final Map<String, BasePipelineState<?, ?>> techniques = new HashMap<>();
    private BasePipelineState<?, ?> additionalState;

    public NewMaterial(DescriptorPool pool) {
        this.pool = pool;
    }

    @Override
    public boolean bind(CommandBuffer cmd, Pipeline pipeline) {
        LinkedList<DescriptorSetLayout> availableLayouts = new LinkedList<>(
                pipeline.getLayout().getDescriptorSetLayouts());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer sets = stack.mallocLong(uniformSets.size());
            for (int i = usedSetSlots.nextSetBit(0), offset = i; i >= 0;) {
                DescriptorSet acquired = uniformSets.get(i).acquire(pool, availableLayouts);
                if (acquired == null) {
                    return false; // material is not supported by the pipeline
                }
                sets.put(acquired.getNativeObject());
                int next = usedSetSlots.nextSetBit(i + 1);
                // if there are no more sets remaining, or the next position skips over at
                // least one index, bind the current descriptor sets
                if (next < 0 || next > i + 1) {
                    sets.flip();
                    vkCmdBindDescriptorSets(cmd.getBuffer(), pipeline.getBindPoint().getEnum(),
                            pipeline.getLayout().getNativeObject(), offset, sets, null);
                    sets.clear();
                    offset = next;
                }
                i = next;
            }
        }
        return true;
    }

    @Override
    public Pipeline selectPipeline(PipelineCache cache, MeshDescription mesh,
                                   String forcedTechnique, PipelineState overrideState) {
        BasePipelineState<?, ?> state = techniques.get(forcedTechnique);
        return state.selectPipeline(cache, mesh);
    }

    @Override
    public void setUniform(String name, GpuBuffer buffer) {
        BufferUniform u = getUniform(name);
        u.set(buffer);
    }

    @Override
    public void setTexture(String name, Texture texture) {
        Uniform<Texture> u = getUniform(name);
        u.set(texture);
    }

    @Override
    @NotFullyImplemented
    public void setParam(String uniform, String param, Object value) {
        Uniform<? extends GpuBuffer> u = getUniform(uniform);
        GpuBuffer buffer = u.get();
        //buffer.map(Structure::new).set(param, value);
        //buffer.unmap();
    }

    @Override
    @NotFullyImplemented
    public void clearParam(String uniform, String param) {
        Uniform<? extends GpuBuffer> u = getUniform(uniform);
        // clear parameter
    }

    @Override
    @NotFullyImplemented
    public <T> T getParam(String uniform, String name) {
        Uniform<? extends GpuBuffer> u = getUniform(uniform);
        // get parameter
        return null;
    }

    @Override
    public Texture getTexture(String name) {
        Uniform<? extends Texture> t = getUniform(name);
        return t != null ? t.get() : null;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        throw new UnsupportedOperationException("Exporting not yet supported.");
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        throw new UnsupportedOperationException("Importing not yet supported.");
    }

    @SuppressWarnings("unchecked")
    public <T extends Uniform> T getUniform(String name) {
        // Not sure if caching the results is really worth it...
        Uniform<?> uniform = uniformLookup.get(name);
        if (uniform != null) {
            return (T)uniform;
        }
        for (UniformSet set : uniformSets.values()) {
            for (Uniform<?> u : set) {
                if (name.equals(u.getName())) {
                    uniformLookup.put(u.getName(), u);
                    return (T)u;
                }
            }
        }
        return null;
    }

    protected UniformSet addSet(UniformSet set) {
        if (uniformSets.put(set.getSetIndex(), set) != null) {
            throw new IllegalArgumentException("Set index already occupied: " + set.getSetIndex());
        }
        usedSetSlots.set(set.getSetIndex());
        return set;
    }

    protected UniformSet addSet(int index, Uniform... uniforms) {
        return addSet(new UniformSet(index, uniforms));
    }

    public void setTechnique(String name, BasePipelineState<?, ?> state) {
        techniques.put(name, state);
    }

    public DescriptorSetLayout[] createLayouts(LogicalDevice<?> device) {
        return uniformSets.values().stream().map(u -> u.createLayout(device)).toArray(DescriptorSetLayout[]::new);
    }

    public Map<Integer, UniformSet> getSets() {
        return Collections.unmodifiableMap(uniformSets);
    }

}
