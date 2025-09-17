package com.jme3.vulkan.material;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.texture.Texture;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.frames.VersionedResource;
import com.jme3.vulkan.material.uniforms.BufferUniform;
import com.jme3.vulkan.material.uniforms.TextureUniform;
import com.jme3.vulkan.material.uniforms.Uniform;
import com.jme3.vulkan.pipelines.Pipeline;
import com.jme3.vulkan.struct.Structure;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Relates shader uniform values to sets and bindings.
 */
public class NewMaterial implements Material {

    private final DescriptorPool pool;
    private final List<UniformSet> uniformSets = new ArrayList<>();
    private final HashMap<String, Uniform<?>> uniformLookup = new HashMap<>();

    public NewMaterial(DescriptorPool pool) {
        this.pool = pool;
    }

    public void bind(CommandBuffer cmd, Pipeline pipeline) {
        bind(cmd, pipeline, 0);
    }

    public void bind(CommandBuffer cmd, Pipeline pipeline, int offset) {
        LinkedList<DescriptorSetLayout> availableLayouts = new LinkedList<>();
        Collections.addAll(availableLayouts, pipeline.getLayout().getDescriptorSetLayouts());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer setBuf = stack.mallocLong(uniformSets.size());
            for (UniformSet set : uniformSets) {
                setBuf.put(set.acquireSet(pool, availableLayouts).getNativeObject());
            }
            setBuf.flip();
            vkCmdBindDescriptorSets(cmd.getBuffer(), pipeline.getBindPoint().getVkEnum(),
                    pipeline.getLayout().getNativeObject(), offset, setBuf, null);
        }
    }

    @Override
    public void render(Geometry geometry, LightList lights, RenderManager renderManager) {
        throw new UnsupportedOperationException("Unable to render in an OpenGL context.");
    }

    @Override
    public void render(Geometry geometry, LightList lights, CommandBuffer cmd, Pipeline pipeline) {

    }

    @Override
    public void setUniform(String name, VersionedResource<? extends GpuBuffer> buffer) {
        BufferUniform u = getUniform(name);
        u.setResource(buffer);
    }

    @Override
    public void setTexture(String name, VersionedResource<? extends Texture> texture) {
        TextureUniform u = getUniform(name);
        u.setResource(texture);
    }

    @Override
    public void setParam(String uniform, String param, Object value) {
        Uniform<? extends GpuBuffer> u = getUniform(uniform);
        GpuBuffer buffer = u.getResource().get();
        buffer.map(Structure::new).set(param, value);
        buffer.unmap();
    }

    @SuppressWarnings("unchecked")
    public <T extends Uniform> T getUniform(String name) {
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

    @Override
    public void write(JmeExporter ex) throws IOException {
        throw new UnsupportedOperationException("Exporting not yet supported.");
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        throw new UnsupportedOperationException("Importing not yet supported.");
    }

}
