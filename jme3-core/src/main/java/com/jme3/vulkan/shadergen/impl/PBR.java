package com.jme3.vulkan.shadergen.impl;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.shadergen.*;
import com.jme3.vulkan.shadergen.vars.*;

/**
 * Shader implementation for {@link com.jme3.vulkan.material.experimental.PBR}: flat base
 * color, metallic, and roughness, with an optional normal map. Attribute locations match
 * {@code PBR.ATTRIBUTE_LOCATIONS} (Position=0, TexCoord=1, Normal=2, Tangent=3), and the
 * color/normal map bindings match {@code PBR.TexturePipe} (ColorMap=0, NormalMap=1 within
 * set 1).
 */
@ShadingLangs(glsl = 460)
public class PBR extends Shader {

    public static class Transforms extends StructVar {
        public final Field<Matrix4f, Mat4> worldViewProjection = new Field<>(new Matrix4f(), Mat4::new);
        { add(worldViewProjection);
          setupLayout(StructLayout.std140); }
    }

    public static class Material extends StructVar {
        public final Field<ColorRGBA, Vec4> baseColor = new Field<>(new ColorRGBA(), Vec4::new);
        public final Field<Float, Flt> metallic = new Field<>(0f, Flt::new);
        public final Field<Float, Flt> roughness = new Field<>(0f, Flt::new);
        { add(baseColor, metallic, roughness);
          setupLayout(StructLayout.std140); }
    }

    private final Global<Transforms> transforms = Global.uniform(1, 0, new Transforms());
    private final Global<Material> material = Global.uniform(1, 1, new Material());

    private final Global<Sampler2D> colorMap = Global.sampler(1, 2, new Sampler2D());
    private final Global<Sampler2D> normalMap = Global.sampler(1, 3, new Sampler2D());

    @EntryPoint(ShaderStage.Vertex)
    public void vertex(@In(0) Vec3 inPosition, @In(1) Vec2 inTexCoord, @In(2) Vec3 inNormal, @In(3) Vec4 inTangent,
                       @Out(0) Vec2 outTexCoord, @Out(1) Vec3 outNormal, @Out(2) Vec4 outTangent, @Out(3) Vec3 outWorldPosition) {
    }

    @EntryPoint(ShaderStage.Fragment)
    public void fragment(@In(0) Vec2 texCoord, @In(1) Vec3 normal, @In(2) Vec4 tangent, @In(3) Vec3 worldPosition,
                         @Out(0) Vec4 fragColor) {
    }

}
