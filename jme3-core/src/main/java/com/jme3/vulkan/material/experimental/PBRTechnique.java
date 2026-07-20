package com.jme3.vulkan.material.experimental;

import com.jme3.backend.Engine;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.util.MapBuilder;
import com.jme3.util.struct.Struct;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.buffer.alloc.BufferAllocator;
import com.jme3.vulkan.commands.RenderCommands;
import com.jme3.vulkan.descriptors.DescriptorSet;
import com.jme3.vulkan.descriptors.UniformBinding;
import com.jme3.vulkan.material.exp2.RenderSession;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.render.bucket.GeometryBucket;
import org.lwjgl.vulkan.VK10;

import java.util.Collection;
import java.util.HashMap;

/**
 * Renders simple PBR materials using flat color, metallic, and roughness, and a normal texture.
 */
public class PBRTechnique implements ShadingTechnique {

    private static class ShaderArgs extends Struct {



    }

    private final ShaderBindingSet shared;
    private final EngineBuffer sharedDataBuffer;

    public PBRTechnique(Engine engine, BufferAllocator<EngineBuffer> allocator) {
        this.shared = engine.createShaderSet(MapBuilder.build(new HashMap<Integer, UniformBinding>())
                .put(0, engine.createUniformBufferBinding(ShaderStage.Fragment)).get());
        this.sharedDataBuffer = allocator.createBuffer();
    }

    @Override
    public void update(RenderCommands cmd, Geometry g) {
        PBR pbr = g.getMaterial().getInterface(PBR.class);
        VK10.vkCmdBindDescriptorSets();
    }

}
