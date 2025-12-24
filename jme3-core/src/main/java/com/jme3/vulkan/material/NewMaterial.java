package com.jme3.vulkan.material;

import com.jme3.asset.AssetKey;
import com.jme3.asset.CloneableSmartAsset;
import com.jme3.dev.NotFullyImplemented;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.texture.Texture;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.material.uniforms.BufferUniform;
import com.jme3.vulkan.material.uniforms.Uniform;
import com.jme3.vulkan.material.uniforms.VulkanUniform;
import com.jme3.vulkan.pipeline.Pipeline;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Relates shader uniform values to shader descriptor sets and bindings.
 */
public class NewMaterial implements VulkanMaterial, CloneableSmartAsset {

    private final Map<String, VulkanUniform<?>> uniforms = new HashMap<>();
    private final Map<String, VulkanTechnique> techniques = new HashMap<>();
    private final Map<DescriptorSetLayout, CachedDescriptorSet> setCache = new HashMap<>();

    @Override
    public CloneableSmartAsset clone() {
        return null;
    }

    @Override
    public void setKey(AssetKey key) {

    }

    @Override
    public AssetKey getKey() {
        return null;
    }

    @Override
    public void bind(CommandBuffer cmd, Pipeline pipeline, DescriptorPool pool) {
        List<DescriptorSetLayout> layouts = pipeline.getLayout().getSetLayouts();
        List<DescriptorSetLayout> reqSetAllocation = new ArrayList<>(layouts.size());
        for (DescriptorSetLayout l : layouts) {
            if (!setCache.containsKey(l)) {
                reqSetAllocation.add(l);
            }
        }
        DescriptorSet[] allocatedSets = pool.allocateSets(reqSetAllocation);
        for (ListIterator<DescriptorSetLayout> it = reqSetAllocation.listIterator(); it.hasNext();) {
            setCache.put(it.next(), new CachedDescriptorSet(allocatedSets[it.previousIndex()]));
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer sets = stack.mallocLong(layouts.size());
            for (DescriptorSetLayout layout : layouts) {
                CachedDescriptorSet set = setCache.get(layout);
                if (set == null) {
                    throw new NullPointerException("Cached descriptor set not available.");
                }
                for (Map.Entry<String, SetLayoutBinding> binding : layout.getBindings().entrySet()) {
                    VulkanUniform<?> uniform = uniforms.get(binding.getKey());
                    if (uniform == null) {
                        throw new NullPointerException("Layout requires uniform \"" + binding.getKey() + "\" which does not exist.");
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
            vkCmdBindDescriptorSets(cmd.getBuffer(), pipeline.getBindPoint().getEnum(),
                    pipeline.getLayout().getNativeObject(), 0, sets, null);
        }
    }

    @Override
    public void setUniformBuffer(String name, GpuBuffer buffer) {
        BufferUniform u = getUniform(name);
        u.set(buffer);
    }

    @Override
    public void setTexture(String name, Texture<?, ?> texture) {
        Uniform<Texture<?, ?>> u = getUniform(name);
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

    @Override
    public void setUniform(String name, Uniform<?> uniform) {
        if (!(uniform instanceof VulkanUniform)) {
            throw new ClassCastException("Uniform must implement VulkanUniform to be used in a Vulkan context.");
        }
        uniforms.put(name, (VulkanUniform<?>)uniform);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Uniform<?>> T getUniform(String name) {
        return (T)uniforms.get(name);
    }

    public Map<String, Uniform<?>> getUniforms() {
        return Collections.unmodifiableMap(uniforms);
    }

    @Override
    public VulkanTechnique getTechnique(String name) {
        return techniques.get(name);
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
            if (changes.isEmpty()) return;
            set.write(changes.values());
            changes.clear();
        }

        public DescriptorSet getSet() {
            return set;
        }

    }

}
