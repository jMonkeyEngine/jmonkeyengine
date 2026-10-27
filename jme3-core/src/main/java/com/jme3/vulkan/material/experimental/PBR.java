package com.jme3.vulkan.material.experimental;

import com.jme3.backend.Engine;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructList;
import com.jme3.vulkan.alloc.ReservedStructArray;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.descriptors.uniforms.TextureBinding;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.mesh.attributes.CommonAttributes;
import com.jme3.vulkan.pipeline.graphics.GraphicsPipeline;
import com.jme3.vulkan.scene.Scene;
import com.jme3.vulkan.shadergen.impl.PBR.Material;
import com.jme3.vulkan.shadergen.impl.PBR.Transforms;
import com.jme3.vulkan.scene.components.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Renders simple PBR materials using flat color, metallic, and roughness, and a normal texture.
 */
@TriggeredOn({"PBR"})
public class PBR implements ShadingTechnique {

    private final Scene scene;

    // for passing in textures directly instead of bindless
    private DescriptorPool pool;
    private final DescriptorSetLayout textureLayout;

    /**
     * Maps the vertex attributes this technique's shader reads to the location each
     * occupies. Stands in for a real shader's declared input locations until PBR has an
     * actual compiled shader program to ask instead.
     */
    private static final Map<String, Integer> ATTRIBUTE_LOCATIONS = new HashMap<>();
    static {
        ATTRIBUTE_LOCATIONS.put(CommonAttributes.Position, 0);
        ATTRIBUTE_LOCATIONS.put(CommonAttributes.TexCoord, 1);
        ATTRIBUTE_LOCATIONS.put(CommonAttributes.Normal, 2);
        ATTRIBUTE_LOCATIONS.put(CommonAttributes.Tangent, 3);
    }

    private final Map<PipelineKey, GraphicsPipeline> pipelineCache = new HashMap<>();

    private ParamPipe params;
    private TexturePipe textures;

    public PBR(Scene scene, Engine engine, int materials) {
        this.scene = scene;
        pool = engine.createDescriptorPool(materials, new PoolSize(DescriptorType.CombinedImageSampler, materials * 2));
        textureLayout = engine.createDescriptorSetLayout(new DescriptorSetLayout.Info()
                .addBinding(0, DescriptorType.CombinedImageSampler, 1, ShaderStage.Fragment)
                .addBinding(1, DescriptorType.CombinedImageSampler, 1, ShaderStage.Fragment));
    }

    @Override
    public boolean acceptNode(int node) {
        return scene.hasTag("pbr", node);
    }

    @Override
    public void render(CommandBuffer cmd, Scene.Group nodes) {
        for (int node : nodes) {
            PipelineKey key = PipelineKey.create(scene.getNode(node), ATTRIBUTE_LOCATIONS::get);
            GraphicsPipeline pipeline = getOrCreatePipeline(key);
            // ... bind pipeline, descriptor sets, and vertex buffers, then issue the draw call
        }
    }

    /**
     * Gets the {@link GraphicsPipeline} for {@code key}, building and caching one first if
     * this is the first node that has needed it.
     *
     * @param key pipeline identity key
     * @return the pipeline {@code key} describes
     */
    private GraphicsPipeline getOrCreatePipeline(PipelineKey key) {
        return pipelineCache.computeIfAbsent(key, this::buildPipeline);
    }

    private GraphicsPipeline buildPipeline(PipelineKey key) {
        return GraphicsPipeline.build(pool.getDevice(), b -> {
            b.applyKey(key);
            // TODO: setLayout/addShaders still need this technique's actual shader program,
            // which doesn't exist yet
        });
    }

    @Override
    public void tagAdded(Scene scene, int node, String tag) {
        scene.getPipe(ParamPipe.class, s -> new ParamPipe(s, new ReservedStructArray<>()));
    }

    @Override
    public void tagRemoved(Scene scene, int node, String tag) {

    }

    public void renderScene(CommandBuffer cmd, Scene.Group geometries) {
        // For each geometry, we need to generate a pipeline. This will be done by the technique managing
        // a "pipeline pool" where each pipeline in the pool shares some base properties. Variations are
        // requested from the pool. Geometries are then sorted based on what pipeline they are to be
        // rendered with.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Constants constants = new Constants();
            constants.bind(new DataBuffer(stack.malloc(constants.size())), 0);
            Scene scene = geometries.getScene();
            textures = scene.getPipe(TexturePipe.class, s -> new TexturePipe(s, this::createTexturesSet));
            params = scene.getPipe(ParamPipe.class, s -> new ParamPipe(s, new StructList<>(
                    new Params(), 100, size -> )));
            for (int g : geometries) {
                // Stands in for a real "tag added" event, which doesn't exist yet - this should only
                // need to happen once, when a node is first tagged for PBR, not every frame for every
                // candidate geometry. activate() is cheap to call repeatedly in the meantime since it
                // no-ops once a node is already complete.
                archetype.activate(g);
                if (archetype.get(g) == null) {
                    continue; // node isn't tagged for PBR (or isn't geometric); nothing to render
                }
                MaterialImpl mat = geometries.getScene().getUserData(g, MaterialImpl.class);
                constants.worldViewProjection.set(geometries.getWorldMatrix(g));
                constants.paramsIndex.set(mat.paramElement);
                VK10.vkCmdPushConstants(cmd, pipelinelayout, ShaderStage.AllGraphics, 0, constants.cache());
                // ... bind constants to pipeline
                // ... bind vertex buffers to pipeline
                VK10.vkCmdBindDescriptorSets();
            }
        }
    }

    private DescriptorSet createTexturesSet() {
        DescriptorSet set = pool.allocateSet(textureLayout).orElseThrow(RuntimeException::new);
        set.setBinding(0, TextureBinding::new);
        set.setBinding(1, TextureBinding::new);
        return set;
    }

    private static class ParamPipe extends StructPipe<Material> {

        private ParamPipe(Scene scene, StructList<Material> array) {
            super(scene, array);
            map(BaseColor.class, p -> p.baseColor);
            map(Metallic.class, p -> p.metallic);
            map(Roughness.class, p -> p.roughness);
        }

    }

    private static class TexturePipe extends DescriptorPipe {

        private final Supplier<DescriptorSet> factory;

        public TexturePipe(Scene scene, Supplier<DescriptorSet> factory) {
            super(scene);
            this.factory = factory;
            map(ColorMap.class, 0, 0);
            map(NormalMap.class, 1, 0);
        }

        @Override
        protected DescriptorSet createDescriptorSet(int node) {
            return factory.get();
        }

    }

    protected static class Params extends Struct<StructField> {

        public final Field<ColorRGBA> color = new Field<>(new ColorRGBA());
        public final Field<Float> metallic = new Field<>(0f);
        public final Field<Float> roughness = new Field<>(0f);

        public Params() {
            add(color, metallic, roughness);
        }

    }

    protected static class Constants extends Struct {

        public final Field<Matrix4f> worldViewProjection = new Field<>(new Matrix4f());
        public final Field<Integer> paramsIndex = new Field<>(0);

    }

}
