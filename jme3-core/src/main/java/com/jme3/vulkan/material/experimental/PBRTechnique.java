package com.jme3.vulkan.material.experimental;

import com.jme3.backend.Engine;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.util.MapBuilder;
import com.jme3.util.cache.InlineTimedCache;
import com.jme3.util.cache.Freeable;
import com.jme3.util.struct.*;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.saving.UpdateHint;
import com.jme3.vulkan.descriptors.UniformBinding;
import com.jme3.vulkan.material.exp2.RenderSession;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.pipeline.state.GraphicsState;
import com.jme3.vulkan.render.bucket.RenderElement;
import com.jme3.vulkan.render.bucket.GeometryBucket;
import com.jme3.vulkan.shaderc.ShaderType;
import com.jme3.vulkan.slang.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class PBRTechnique implements ShadingTechnique {

    private static class ModelTransform extends Struct {

        public final Field<Matrix4f> worldViewProjection = new Field<>(new Matrix4f());
        public final Field<Matrix4f> world = new Field<>(new Matrix4f());

        public ModelTransform() {
            addFields(worldViewProjection, world);
            bind(StructLayout.std140);
        }

    }

    public static class MaterialParams extends Struct {

        public final Field<ColorRGBA> color = new Field<>(new ColorRGBA(0f, 0f, 0f, 0f));
        public final Field<Float> metallic = new Field<>(0f);
        public final Field<Float> roughness = new Field<>(0f);

        public MaterialParams() {
            addFields(color, metallic, roughness);
            bind(StructLayout.std140);
        }

    }

    private class PBRMetadata implements Freeable {

        private final PBR pbr;
        private final int index;
        private final ShaderBindingSet textureSet;
        private final MaterialParams paramStruct = new MaterialParams();

        public PBRMetadata(Engine engine, PBR pbr) {
            this.pbr = pbr;
            availableIndices.set(index = availableIndices.nextClearBit(0));
            textureSet = engine.createShaderSet(MapBuilder.build(new HashMap<Integer, UniformBinding>())
                    .put(0, engine.createTextureBinding(ShaderStage.Fragment))
                    .put(1, engine.createTextureBinding(ShaderStage.Fragment)).get());
            pbr.getColor().addDownstream(paramStruct.color);
            pbr.getMetallic().addDownstream(paramStruct.metallic);
            pbr.getRoughness().addDownstream(paramStruct.roughness);
        }

        @Override
        public void onCacheEviction(InlineTimedCache<?, ?> cache, Object key) {
            availableIndices.clear(index);
        }

    }

    private static final String SPECIALIZATION = "import pbrUtils;export static const int numLights = {0};export struct Fog : IFog = {1};";

    private final ShaderBindingSet transformBinding, materialBinding;
    private final MappableBuffer transformData, materialData;
    private final ModelTransform transformStruct = new ModelTransform();
    private final MaterialParams materialStruct = new MaterialParams();
    private final InlineTimedCache<PBR, PBRMetadata> metadata = InlineTimedCache.identityCache(TimeUnit.SECONDS.toMillis(2));
    private final BitSet availableIndices = new BitSet();

    public PBRTechnique(Engine engine) {
        // create objects to interface parameters to shader sets/bindings by set (i.e. DescriptorSets)
        Map<Integer, UniformBinding> temp = new HashMap<>();
        transformBinding = engine.createShaderSet(MapBuilder.build(temp)
                .put(0, engine.createUniformBufferBinding(ShaderStage.Vertex)).get());
        materialBinding = engine.createShaderSet(MapBuilder.build(temp)
                .put(0, engine.createUniformBufferBinding(ShaderStage.Fragment)).get());
        // create and write transform data buffer to binding object at binding 0
        transformData = engine.createBuffer(transformStruct.getSize(), BufferUsage.Uniform, UpdateHint.Stream);
        transformBinding.stage(0, transformData);
        transformBinding.write(); // write immediately: no further changes planned
        // create and write material data buffer to binding object at binding 0
        materialData = engine.createBuffer(1, BufferUsage.Uniform, UpdateHint.Dynamic);
        materialBinding.stage(0, materialData);
    }

    @Override
    public void render(RenderSession session, ViewPort vp, GeometryBucket bucket) {

        // get select geometry from geometry bucket
        Collection<Geometry> selected = bucket.selectGeometries(g -> g.getMaterial().containsInterface(PBR.class));
        if (selected.isEmpty()) {
            return;
        }

        // plan on using transformBinding and materialBinding in the next pipeline
        session.stageShaderSet(1, transformBinding.bind());
        session.stageShaderSet(2, materialBinding.bind());

        List<RenderElement> elements = new ArrayList<>(selected.size());

        Session slang = session.getEngine().getSlangSession();
        Module shaderMod = slang.loadModule("PBRLighting");

        for (Geometry g : selected) {
            PBR pbr = g.getMaterial().getInterface(PBR.class);
            metadata.computeIfAbsent(pbr, k -> new PBRMetadata(session.getEngine()));
            GraphicsState state = g.getMaterial().getGraphicsState().clone();
            Module special = slang.loadAnonymousModule(String.format(SPECIALIZATION, pbr.getNumLights(), pbr.isFogEnabled() ? "NormalFog" : "NoFog"));
            state.setShader(ShaderType.Vertex, session.getEngine().createShader(slang.createComposite(shaderMod.getEntryPoint("vertexMain"), special)));
            state.setShader(ShaderType.Fragment, session.getEngine().createShader(slang.createComposite(shaderMod.getEntryPoint("fragmentMain"), special)));
            state.applyMesh(g.getMesh());
            session.stageShaderSet(3, pbr.getTextureSet().bind());
            elements.add(session.createRenderElement(vp, g, state));
        }

        elements.sort(bucket.getComparator());

        // resize material data buffer to contain a MaterialParams struct per geometry.
        // this takes advantage of "dynamic offsets" on vulkan descriptor sets
        materialData.resizeUp(availableIndices.nextClearBit(0) * materialStruct.getSize());

        try (StructMapping<ModelTransform> transformMap = transformData.mapStruct(transformStruct);
             StructMapping<MaterialParams> materialMap = materialData.mapAllStructs(materialStruct)) {

            // create render elements, sort elements, and render in order
            for (RenderElement e : elements) {

                // bind render element resources (i.e. pipeline)
                e.bind();

                // set transform matrices, viewProjection and other world parameters handled by Engine object
                e.getGeometry().getWorldMatrix().mult(vp.getCamera().getViewProjectionMatrix(), transformStruct.worldViewProjection.alias());
                transformStruct.worldViewProjection.set();
                transformStruct.world.set(e.getGeometry().getWorldMatrix());

                PBR pbr = e.getMaterial().getInterface(PBR.class);
                PBRMetadata meta = metadata.get(pbr);
                meta.textureSet.stage(0, pbr.getColorMap());
                meta.textureSet.stage(1, pbr.getNormalMap());
                materialMap.sample(meta.index);

                // set parameters
                materialStruct.color.set(pbr.getColor());
                materialStruct.metallic.set(pbr.getMetallic());
                materialStruct.roughness.set(pbr.getRoughness());

                // write textures to the binding object
                materialBinding.stage(1, pbr.getColorMap());
                materialBinding.stage(2, pbr.getNormalMap());
                materialBinding.write();

                // bind material data and textures to set=2 in shader
                session.stageShaderSet(2, materialBinding.bind(materialStruct.getPosition()));
                session.stageShaderSet(3, meta.textureSet.bind());
                session.bindShaderSets();

                // render element mesh
                e.render();
            }
        }

    }

}
