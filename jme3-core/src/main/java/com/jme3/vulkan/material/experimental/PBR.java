package com.jme3.vulkan.material.experimental;

import com.jme3.backend.Engine;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;
import com.jme3.util.MapBuilder;
import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableManager;
import com.jme3.util.natives.DisposableReference;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.alloc.*;
import com.jme3.vulkan.buffer.BufferUsage;
import com.jme3.vulkan.descriptors.UniformBinding;
import com.jme3.vulkan.material.shader.ShaderStage;

import java.util.HashMap;

public class PBR implements ShadingInterface, Disposable {

    public static class Params extends Struct {

        public final Field<ColorRGBA> color = new Field<>(new ColorRGBA());
        public final Field<Float> metallic = new Field<>(0f);
        public final Field<Float> roughness = new Field<>(0f);

        public Params() {
            addFields(color, metallic, roughness);
            bind(StructLayout.std140);
        }

    }

    // material buffers shared among all instances+subclasses
    protected static final StructArray<Params> paramsArray = new StructArray<>(new Params(), 100);

    private final int matIndex;
    private final ShaderBindingSet textures; // binding set per instance
    private final DisposableReference ref;

    public PBR(Engine engine) {
        paramsArray.bind(engine.createBuffer(paramsArray.getByteSize(), BufferUsage.Uniform, UpdateHint.Dynamic));
        matIndex = materialData.acquireIndex();
        // Blocks out subclasses from adding bindings, needs to be changed. Make ShaderBindingSets lazy/dynamic?
        textures = engine.createShaderSet(MapBuilder.build(new HashMap<Integer, UniformBinding>())
                .put(0, engine.createTextureBinding(ShaderStage.Fragment))
                .put(1, engine.createTextureBinding(ShaderStage.Fragment)).get());
        ref = DisposableManager.reference(this); // release index on death
    }

    @Override
    public Runnable createDestroyer() {
        return () -> materialData.releaseIndex(matIndex);
    }

    @Override
    public DisposableReference getReference() {
        return ref;
    }

    // Getters not provided because data is stored directly in the gpu's buffer more or less.
    // Reading *can* be a very slow operation so we encourage state tracking outside the material.
    // Users will totally use getters without realizing the performance implications.

    public void setColor(ColorRGBA color) {
        materialData.getStruct(Params.class, matIndex).color.set(color);
    }

    public void setMetallic(float metallic) {
        materialData.getStruct(Params.class, matIndex).metallic.set(metallic);
    }

    public void setRoughness(float roughness) {
        materialData.getStruct(Params.class, matIndex).roughness.set(roughness);
    }

    public void setColorMap(Texture colorMap) {
        textures.stage(0, colorMap);
    }

    public void setNormalMap(Texture normalMap) {
        textures.stage(1, normalMap);
    }

    public ShaderBindingSet getTexturesSet() {
        return textures;
    }

    public int getMaterialParamIndex() {
        return matIndex;
    }

}
