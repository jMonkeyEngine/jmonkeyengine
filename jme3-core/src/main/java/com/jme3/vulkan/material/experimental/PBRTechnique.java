package com.jme3.vulkan.material.experimental;

import com.jme3.backend.Engine;
import com.jme3.math.Matrix4f;
import com.jme3.texture.Texture;
import com.jme3.util.struct.Struct;
import com.jme3.vulkan.alloc.StructArray;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.descriptors.uniforms.BufferBinding;
import com.jme3.vulkan.descriptors.uniforms.TextureBinding;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.mesh.ExperimentalCubeMesh;
import com.jme3.vulkan.scene.Scene;

import java.util.BitSet;

/**
 * Renders simple PBR materials using flat color, metallic, and roughness, and a normal texture.
 */
public class PBRTechnique {

    private EngineBuffer instances;

    // support up to 100 materials
    private final StructArray<Params> parameters;
    private final BitSet usedMatIndices = new BitSet();

    // for passing in textures directly instead of bindless (not used, only provided as an example)
    private final DescriptorPool pool;
    private final DescriptorSetLayout textureLayout;

    public PBRTechnique(Engine engine, GlobalsDescriptorSet globals, int materials) {
        // 100 sets for 100 materials, with 2 CombinedImageSampler descriptors per material
        pool = engine.createDescriptorPool(materials, new PoolSize(DescriptorType.CombinedImageSampler, materials * 2));
        textureLayout = engine.createDescriptorSetLayout(new DescriptorSetLayout.Info()
                .addBinding(0, DescriptorType.CombinedImageSampler, 1, ShaderStage.Fragment)
                .addBinding(1, DescriptorType.CombinedImageSampler, 1, ShaderStage.Fragment));
        parameters = new StructArray<>(materials, new Params());
        parameters.bind(engine.createDynamicBuffer(parameters.capacity(), EngineBuffer.Role.Storage));
        globals.setNextBinding(new BufferBinding(DescriptorType.StorageBuffer, parameters));
    }

    public void renderScene(Scene.Subset geometries) {
        // each batched geometry (an "instance") requires pointers to mesh and material data.
        // the first thing to do is determine which geometries should be batched together.
        // for now we'll assume all nodes in the subset are geometric and are batchable together.
        DataBuffer inst = instances.cache();
        geometries.filter(g -> )
        for (int g : geometries) {
            ExperimentalCubeMesh mesh; // somehow extract mesh from geometry
            inst.put(mesh.getVertexArrayAddress()); // first long is the vertex buffer
            inst.put(mesh.getIndicesArrayAddress()); // second long is the index buffer
        }
    }

    protected DescriptorSet createTexturesSet() {
        DescriptorSet set = pool.allocateSets(textureLayout).orElseThrow(() -> new RuntimeException("Pool out of memory."))[0];
        set.setBinding(0, TextureBinding::new);
        set.setBinding(1, TextureBinding::new);
        return set;
    }

    public class Material {

        // struct array index that this material's data is stored at
        private final int materialIndex;

        // for if we want to pass textures directly into the shader
        private final DescriptorSet textures = createTexturesSet();

        protected Material() {
            materialIndex = usedMatIndices.nextClearBit(0);
            usedMatIndices.set(materialIndex);
        }

        public void setMetallic(float metallic) {
            parameters.index(materialIndex).metallic.set(metallic);
        }

        public void setRoughness(float roughness) {
            parameters.index(materialIndex).roughness.set(roughness);
        }

        public void setColorMap(Texture colorMap) {
            textures.getBinding(0).set(0, colorMap);
        }

        public void setNormalMap(Texture normalMap) {
            textures.getBinding(1).set(0, normalMap);
        }

    }

    private static class Params extends Struct {

        public final Field<Integer> positionVertex = new Field<>(0);
        public final Field<Float> metallic = new Field<>(0f);
        public final Field<Float> roughness = new Field<>(0f);

        public Params() {
            addFields(positionVertex, metallic, roughness);
        }

    }

    private static class Constants extends Struct {

        public final Field<Matrix4f> worldViewProjection = new Field<>(new Matrix4f());
        public final Field<Integer> materialIndex = new Field<>(0);

        public Constants() {
            addFields(worldViewProjection, materialIndex);
        }
    }

}
