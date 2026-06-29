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
import com.jme3.vulkan.alloc.StructArray;
import com.jme3.vulkan.buffer.BufferUsage;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.saving.UpdateHint;
import com.jme3.vulkan.descriptors.UniformBinding;
import com.jme3.vulkan.material.exp2.RenderSession;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.render.bucket.GraphicsElement;
import com.jme3.vulkan.render.bucket.GeometryBucket;
import com.jme3.vulkan.render.bucket.RenderElement;
import com.jme3.vulkan.shaderc.ShaderType;
import com.jme3.vulkan.slang.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This shading technique is obsolete due to API changes.
 */
@Deprecated
public class OldPBRTechnique implements ShadingTechnique {

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
        }

        @Override
        public void onCacheEviction(InlineTimedCache<?, ?> cache, Object key) {
            availableIndices.clear(index);
        }

    }

    protected static StructArray<MaterialParams> materials = new StructArray<>(MaterialParams::new, 100);

    private static final String SPECIALIZATION = "import pbrUtils;export static const int numLights = {0};export struct Fog : IFog = {1};";

    private final ShaderBindingSet transformBinding, materialBinding;
    private final ModelTransform transform = new ModelTransform();
    private final InlineTimedCache<PBR, PBRMetadata> metadata = InlineTimedCache.identity(TimeUnit.SECONDS.toMillis(2));
    private final BitSet availableIndices = new BitSet();

    public OldPBRTechnique(Engine engine) {
        // create objects to interface parameters to shader sets/bindings by set (i.e. DescriptorSets)
        Map<Integer, UniformBinding> temp = new HashMap<>();
        transformBinding = engine.createShaderSet(MapBuilder.build(temp)
                .put(0, engine.createUniformBufferBinding(ShaderStage.Vertex)).get());
        materialBinding = engine.createShaderSet(MapBuilder.build(temp)
                .put(0, engine.createUniformBufferBinding(ShaderStage.Fragment)).get());
        // create and write transform data buffer to binding object at binding 0
        MappableBuffer transformData = engine.createBuffer(transform.getSize(), BufferUsage.Uniform, UpdateHint.Stream);
        transformBinding.stage(0, transformData);
        transformBinding.write(); // write immediately: no further changes planned
        // create and write material data buffer to binding object at binding 0
        MappableBuffer materialData = engine.createBuffer(materials.getByteSize(), BufferUsage.Uniform, UpdateHint.Dynamic);
        materialBinding.stage(0, materialData);
    }

    @Override
    public void render(RenderSession session, ViewPort vp, GeometryBucket bucket) {

        Engine engine = session.getEngine();

        // get select geometry from geometry bucket
        Collection<Geometry> selected = bucket.selectGeometries(g -> g.getMaterial().containsInterface(PBR.class));
        if (selected.isEmpty()) {
            return;
        }

        if (selected.size() > materials.length()) {
            materials.setLength(selected.size());
            materials.getBuffer().resize(session, materials.getByteSize());
        }

        List<RenderElement> elements = new ArrayList<>(selected.size());

        Session slang = engine.getSlangSession();
        Module shaderMod = slang.loadModule("PBRLighting");

        for (Geometry g : selected) {
            PBR pbr = g.getMaterial().getInterface(PBR.class);
            Module special = slang.loadAnonymousModule(String.format(SPECIALIZATION, pbr.getNumLights(), pbr.isFogEnabled() ? "NormalFog" : "NoFog"));

            GraphicsPipeline pipeline = session.getEngine().createGraphicsPipeline();
            pipeline.setState(g.getMaterial().getGraphicsState());
            session.getForcedState().accept(pipeline);
            pipeline.setShader(ShaderType.Vertex, engine.createShader(slang.createComposite(shaderMod.getEntryPoint("vertexMain"), special)));
            pipeline.setShader(ShaderType.Fragment, engine.createShader(slang.createComposite(shaderMod.getEntryPoint("fragmentMain"), special)));
            pipeline.setMesh(g.getMesh());
            pipeline.setLineWidth(2.1f);

            elements.add(new GraphicsElement(vp.getCamera(), g, pipeline));
        }

        elements.sort(bucket.getComparator());

        try (MappingArena arena = new MappingArena(transform)) {

            // create render elements, sort elements, and render in order
            for (RenderElement e : elements) {

                // bind render element resources (i.e. pipeline)
                e.bind();

                // set transform matrices, viewProjection and other world parameters handled by Engine object
                e.getGeometry().getWorldMatrix().mult(vp.getCamera().getViewProjectionMatrix(), transform.worldViewProjection.alias());
                transform.worldViewProjection.set();
                transform.world.set(e.getGeometry().getWorldMatrix());
                transform.push();

                PBR pbr = e.getMaterial().getInterface(PBR.class);
                PBRMetadata meta = metadata.get(pbr);
                meta.textureSet.stage(0, pbr.getColorMap());
                meta.textureSet.stage(1, pbr.getNormalMap());

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
