package com.jme3.vulkan.material.experimental;

import com.jme3.math.Matrix4f;
import com.jme3.renderer.ViewPort;
import com.jme3.util.natives.Destructor;
import com.jme3.util.struct.StructList;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.frames.Frame;
import com.jme3.vulkan.pipeline.PipelineLayout;
import com.jme3.vulkan.pipeline.PipelineShader;
import com.jme3.vulkan.pipeline.graphics.GraphicsPipeline;
import com.jme3.vulkan.render.RenderList;
import com.jme3.vulkan.scene.Scene;
import com.jme3.vulkan.scene.components.*;
import com.jme3.vulkan.shadergen.impl.PBR;
import com.jme3.vulkan.shadergen.impl.PBR.Material;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders simple PBR materials using flat color, metallic, and roughness, and a normal texture.
 */
public class PBRTechnique implements ShadingTechnique {

    private final Scene scene;
    private final PipelineLayout layout;
    private final List<PipelineShader> shaderStages = Collections.emptyList();
    private final PBR shader = new PBR();
    private final PBR.Transforms transforms = new PBR.Transforms();

    private final Map<PipelineKey, GraphicsPipeline> pipelineCache = new HashMap<>();

    public PBRTechnique(Scene scene, LogicalDevice<?> device, Frame frames, int materials) {
        this.scene = scene;
        shader.compile(device, frames, materials);
        layout = new PipelineLayout(device, shader.getSetLayouts(), shader.getPushConstantRanges());
        ByteBuffer transformData = MemoryUtil.memAlloc(transforms.size());
        transforms.bind(new DataBuffer(transformData));
        Destructor.run(transformData, () -> MemoryUtil.memFree(transformData));
    }

    @Override
    public boolean acceptNode(int node) {
        return scene.hasTag("pbr", node);
    }

    @Override
    public void render(CommandBuffer cmd, ViewPort vp, RenderList.Section nodes) {

        RenderList.Section sect = nodes.partition(n -> scene.hasTag("pbr", n) && scene.hasMesh(n));

        Matrix4f worldViewProj = new Matrix4f();

        for (int i = 0; i < sect.size(); i++) {
            int node = sect.markRendered(i);

            PipelineKey key = PipelineKey.create(scene.getNode(node), shader.getVertexAttributes()::get);
            key.setPushConstants(layout.getPushConstantRanges());
            key.setAttachments(vp.getOutputFrameBuffer());

            GraphicsPipeline pipeline = pipelineCache.computeIfAbsent(key, k -> new GraphicsPipeline(layout, shaderStages, k));
            cmd.cmdBindPipeline(pipeline);

            transforms.worldViewProjection.set(scene.getWorldMatrix(node, worldViewProj)
                    .multLocal(vp.getCamera().getViewProjectionMatrix()));
            shader.getTransforms().set(cmd, transforms.cache().getBytes());

            shader.getMaterial().set(node, scene.getComponent(MaterialPipe.class).get(node));
            shader.getColorMap().setFrom(node, scene.getComponent(ColorMap.class));
            shader.getNormalMap().setFrom(node, scene.getComponent(NormalMap.class));
            shader.bindDescriptors(cmd, node);

            shader.draw(cmd, scene.getMesh(node));

        }
    }

    @Override
    public void tagAdded(Scene scene, int node, String tag) {}

    @Override
    public void tagRemoved(Scene scene, int node, String tag) {
        shader.releaseDescriptors(node);
    }

    private static class MaterialPipe extends StructPipe<Material> {

        private MaterialPipe(Scene scene, StructList<Material> array) {
            super(scene, array);
            map(BaseColor.class, p -> p.baseColor);
            map(Metallic.class, p -> p.metallic);
            map(Roughness.class, p -> p.roughness);
        }

    }

}
