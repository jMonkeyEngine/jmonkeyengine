package com.jme3.vulkan.material;

import com.jme3.backend.Engine;
import com.jme3.math.ColorRGBA;
import com.jme3.util.struct.Member;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructLayout;

public class PBRMaterial extends NewMaterial {

    public static class MetallicRoughness implements Struct {
        @Member(0) public final ColorRGBA color = new ColorRGBA();
        @Member(1) public float metallic = 0f;
        @Member(2) public float roughness = 1f;
    }

    public PBRMaterial(Engine engine) {
        setUniform("MetallicRoughness", engine.createUniformBuffer(StructLayout.std140, new MetallicRoughness()));
        setUniform("BaseColorMap", engine.createTextureUniform()).set();
    }

}
