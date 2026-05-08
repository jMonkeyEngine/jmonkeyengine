package com.jme3.vulkan.material.experimental;

import com.jme3.backend.Engine;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.util.struct.FixedSizeStruct;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructLayout;
import com.jme3.util.struct.StructMapping;
import com.jme3.vulkan.JmePlatform;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.saving.UpdateHint;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.uniforms.BufferBinding;
import com.jme3.vulkan.descriptors.uniforms.TextureBinding;
import com.jme3.vulkan.material.exp2.RenderSession;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.render.bucket.RenderElement;
import com.jme3.vulkan.render.bucket.GeometryBucket;
import com.jme3.vulkan.shaderc.ShaderType;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

public class PBRTechnique implements ShadingTechnique {

    private static class ModelTransform extends Struct {

        public final Field<Matrix4f> worldViewProjection = new Field<>(new Matrix4f());
        public final Field<Matrix4f> world = new Field<>(new Matrix4f());

        public ModelTransform() {
            addFields(worldViewProjection, world);
            bind(StructLayout.std140);
        }

    }

    private static class MaterialParams extends FixedSizeStruct {

        public final Field<ColorRGBA> color = new Field<>(new ColorRGBA());
        public final Field<Float> metallic = new Field<>(.5f);
        public final Field<Float> roughness = new Field<>(.5f);

        public MaterialParams() {
            addFields(color, metallic, roughness);
            bind(StructLayout.std140);
        }

    }

    private final ShaderBindingSet transformBinding, materialBinding;
    private final MappableBuffer transformData, materialData;
    private final ModelTransform transformStruct = new ModelTransform();
    private final MaterialParams materialStruct = new MaterialParams();
    private final ShaderProgram program = ShaderProgram.build()
            .addSource(ShaderType.Vertex, "Common/MatDefs/Misc/Unshaded.vert")
            .addSource(ShaderType.Fragment, "Common/MatDefs/Misc/Unshaded.frag")
            .build();

    public PBRTechnique(Engine engine) {
        // create objects to interface parameters to shader sets/bindings by set (i.e. DescriptorSets)
        transformBinding = engine.createShaderBindings(b -> {
            b.addBinding(0, new BufferBinding(Descriptor.UniformBuffer, ShaderStage.AllGraphics));
        });
        materialBinding = engine.createShaderBindings(b -> {
            b.addBinding(0, new BufferBinding(Descriptor.UniformBuffer, ShaderStage.Fragment));
            b.addBinding(1, new TextureBinding(ShaderStage.Fragment));
            b.addBinding(2, new TextureBinding(ShaderStage.Fragment));
        });
        // create and write transform data buffer to binding object at binding 0
        transformData = JmePlatform.allocateStandardBuffer(transformStruct.getSize(), BufferUsage.Uniform, UpdateHint.Stream);
        transformBinding.stage(0, transformData);
        transformBinding.write(); // write immediately: no further changes planned
        // create and write material data buffer to binding object at binding 0
        materialData = JmePlatform.allocateStandardBuffer(1, BufferUsage.Uniform, UpdateHint.Dynamic);
        materialBinding.stage(0, materialData);
    }

    @Override
    public void render(RenderSession session, ViewPort vp, GeometryBucket bucket, Function<Geometry, RenderElement> elementFactory) {

        // get select geometry from geometry bucket
        Collection<Geometry> selected = bucket.selectGeometries(g -> g.getMaterial().containsInterface(PBR.class));
        if (selected.isEmpty()) {
            return;
        }

        // plan on using transformBinding and materialBinding in the next pipeline (they are not bound)
        session.stageBindingSet(1, transformBinding.bind());
        session.stageBindingSet(2, materialBinding.bind(0));

        // resize material data buffer to contain a MaterialParams struct per geometry.
        // this takes advantage of "dynamic offsets" on vulkan descriptor sets
        materialData.resizeUp(selected.size() * materialStruct.getSize());

        try (StructMapping<ModelTransform> transformMap = transformData.mapStruct(transformStruct);
             StructMapping<MaterialParams> matMap = materialData.mapAllStructs(materialStruct)) {

            // create render elements, sort elements, and render in order
            selected.stream().map(elementFactory).sorted(bucket.getComparator()).forEachOrdered(e -> {

                // bind render element resources (i.e. pipeline)
                e.bind();

                // set transform matrices, viewProjection and other world parameters handled by Engine object
                e.getGeometry().getWorldMatrix().mult(vp.getCamera().getViewProjectionMatrix(), transformStruct.worldViewProjection.alias());
                transformStruct.worldViewProjection.set();
                transformStruct.world.set(e.getGeometry().getWorldMatrix());

                // set pbr parameters
                PBR pbr = e.getMaterial().getInterface(PBR.class);
                materialStruct.color.compareAndSet(pbr.getColor());
                materialStruct.metallic.compareAndSet(pbr.getMetallic());
                materialStruct.roughness.compareAndSet(pbr.getRoughness());

                // write textures to the binding object
                materialBinding.stage(1, pbr.getColorMap());
                materialBinding.stage(2, pbr.getNormalMap());
                materialBinding.write();

                // bind material data and textures to set=2 in shader
                session.stageBindingSet(2, materialBinding.bind(materialStruct.getPosition()));
                session.applyBindingSets();

                // render element mesh
                e.render();

                // move to next section of material data buffer
                matMap.increment();
            });
        }

    }

    @Override
    public ShaderProgram getProgram() {
        return program;
    }

}
