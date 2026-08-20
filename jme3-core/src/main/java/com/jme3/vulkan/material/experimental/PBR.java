package com.jme3.vulkan.material.experimental;

import com.jme3.backend.Engine;
import com.jme3.math.Matrix4f;
import com.jme3.texture.Texture;
import com.jme3.util.struct.Struct;
import com.jme3.vulkan.alloc.StructArray;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.DynamicBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.buffer.alloc.BufferType;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.descriptors.uniforms.TextureBinding;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.mesh.ExperimentalCubeMesh;
import com.jme3.vulkan.scene.Scene;

import java.util.Optional;

/**
 * Renders simple PBR materials using flat color, metallic, and roughness, and a normal texture.
 */
public class PBR {

    private final MaterialData data;

    // for passing in textures directly instead of bindless
    private DescriptorPool pool;
    private final DescriptorSetLayout textureLayout;

    public PBR(Engine engine, int materials) {
        this(engine, materials, new PoolS)
    }

    protected PBR(Engine engine, int materials, PoolSize... sizes) {
        this.data = engine.getMaterialData();
        pool = engine.createDescriptorPool(materials, new PoolSize(DescriptorType.CombinedImageSampler, materials * 2));
        textureLayout = engine.createDescriptorSetLayout(new DescriptorSetLayout.Info()
                .addBinding(0, DescriptorType.CombinedImageSampler, 1, ShaderStage.Fragment)
                .addBinding(1, DescriptorType.CombinedImageSampler, 1, ShaderStage.Fragment));
        engine.getMaterialData().initDataType(Params.class, () -> new DynamicBuffer<>(engine,
                new StructArray<>(materials, new Params()), BufferType.Dynamic, EngineBuffer.Role.Storage));
    }

    public void renderScene(Scene.Subset geometries) {
        // each batched geometry (an "instance") requires pointers to mesh and material data.
        // the first thing to do is determine which geometries should be batched together.
        // for now we'll assume all nodes in the subset are geometric and are batchable together.
        DataBuffer inst = instances.cache();
        for (int g : geometries) {
            ExperimentalCubeMesh mesh; // somehow extract mesh from geometry
            inst.put(mesh.getVertexArrayAddress()); // first long is the vertex buffer
            inst.put(mesh.getIndicesArrayAddress()); // second long is the index buffer
        }
    }

    protected DescriptorSet createTexturesSet() {
        DescriptorSet set = pool.allocateSets(textureLayout).orElseThrow(RuntimeException::new)[0];
        set.setBinding(0, TextureBinding::new);
        set.setBinding(1, TextureBinding::new);
        return set;
    }

    public class Material {

        // index that this material's data is stored at for Params.class
        protected final int paramElement;

        // for if we want to pass textures directly into the shader
        protected final DescriptorSet textures = createTexturesSet();

        protected Material(CommandBuffer cmd) {
            paramElement = data.acquireElement(cmd, Params.class);
        }

        public void setMetallic(float metallic) {
            data.getStruct(Params.class, paramElement).metallic.set(metallic);
        }

        public void setRoughness(float roughness) {
            data.getStruct(Params.class, paramElement).roughness.set(roughness);
        }

        public void setColorMap(Texture colorMap) {
            textures.getBinding(0).set(0, colorMap);
        }

        public void setNormalMap(Texture normalMap) {
            textures.getBinding(1).set(0, normalMap);
        }

    }

    protected static class Params extends Struct {

        public final Field<Float> metallic = new Field<>(0f);
        public final Field<Float> roughness = new Field<>(0f);

        public Params() {
            addFields(metallic, roughness);
        }

    }

    protected static class Constants extends Struct {

        public final Field<Matrix4f> worldViewProjection = new Field<>(new Matrix4f());
        public final Field<Integer> materialParamsIndex = new Field<>(0);

    }

}
