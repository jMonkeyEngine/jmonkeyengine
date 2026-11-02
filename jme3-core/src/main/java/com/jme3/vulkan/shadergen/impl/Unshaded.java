package com.jme3.vulkan.shadergen.impl;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.shadergen.*;
import com.jme3.vulkan.shadergen.lib.Skinning;
import com.jme3.vulkan.shadergen.vars.*;

@ShadingLangs(glsl = 460)
public class Unshaded extends Shader {

    public static class Transforms extends StructVar {
        public final Field<Matrix4f, Mat4> worldViewProjection = new Field<>(new Matrix4f(), Mat4::new);
        { add(worldViewProjection);
          setupLayout(StructLayout.std140); }
    }

    public static class Visuals extends StructVar {
        public final Field<ColorRGBA, Vec4> color = new Field<>(new ColorRGBA(), Vec4::new);
        public final Field<ColorRGBA, Vec4> glow = new Field<>(new ColorRGBA(), Vec4::new);
        public final Field<Float, Flt> alphaDiscard = new Field<>(0f, Flt::new);
        public final Field<Float, Flt> desaturation = new Field<>(0f, Flt::new);
        { add(color, glow, alphaDiscard, desaturation);
          setupLayout(StructLayout.std140); }
    }

    private final PushConstants<Transforms> transforms = Global.pushConstants(new Transforms());
    private final Uniform<Visuals> visuals = Global.uniform(1, 1, new Visuals());

    private final Sampler<Sampler2D> colorMap = Global.sampler(1, 3, new Sampler2D());
    private final Sampler<Sampler2D> glowMap = Global.sampler(1, 4, new Sampler2D());

    private final Specialization<Bool> DISCARD_ALPHA = Global.specialization(0, False());
    private final Specialization<UInt> NUM_BONES = Global.specialization(1, UInt(0));
    private final Specialization<UInt> MORPH_TARGETS = Global.specialization(2, UInt(0));
    private final Specialization<Bool> HAS_GLOWCOLOR = Global.specialization(3, False());
    private final Specialization<Bool> HAS_GLOWMAP = Global.specialization(4, False());
    private final Specialization<Bool> HAS_VERTEX_COLOR = Global.specialization(5, False());
    private final Specialization<Bool> HAS_COLOR = Global.specialization(6, False());
    private final Specialization<Bool> DESATURATE = Global.specialization(7, False());
    private final Specialization<Bool> HAS_COLORMAP = Global.specialization(8, False());

    @EntryPoint(ShaderStage.Vertex)
    public void vertex(@In(0) Vec3 inPosition, @In(1) Vec2 inTexCoord, @In(2) Vec2 inTexCoord2, @In(3) Vec4 inColor,
                       @Out(0) Vec2 outTexCoord, @Out(1) Vec2 outTexCoord2, @Out(2) Vec4 outColor) {
        outTexCoord.set(inTexCoord);
        outTexCoord2.set(inTexCoord2);
        outColor.set(inColor);
        Vec4 modelSpacePos = new Vec4(inPosition, 1.0f);
        Skinning skin = Import(new Skinning());
        If(MORPH_TARGETS.use().gt(0), () -> {
            skin.morphCompute(modelSpacePos);
        });
        If(NUM_BONES.use().gt(0), () -> {
            skin.skinningCompute(modelSpacePos);
        });
        gl.clipPosition.set(transforms.use().worldViewProjection.use().mult(modelSpacePos));
    }

    @EntryPoint(ShaderStage.Fragment)
    public void fragment(@In(0) Vec2 texCoord, @In(1) Vec2 texCoord2, @In(2) Vec4 vertColor, @Out(0) Vec4 fragColor) {
        Vec4 color = new Vec4(1f, 1f, 1f, 1f);
        If(HAS_VERTEX_COLOR.use(), () -> {
            color.multLocal(vertColor);
        });
        If(HAS_COLOR.use(), () -> {
            color.multLocal(visuals.use().color.use());
        });
        If(HAS_COLORMAP.use(), () -> {
            color.multLocal(colorMap.use().sample(texCoord));
        });
        If(DISCARD_ALPHA.use(), () -> {
            If(color.a().lt(visuals.use().alphaDiscard.use()), this::Discard);
        });
        If(DESATURATE.use(), () -> {
            Vec3 gray = new Vec3(new Vec3(0.2126f, 0.7152f, 0.0722f).dot(color.rgb()));
            color.rgb().set(color.rgb().mix(gray, visuals.use().desaturation.use()));
        });
        fragColor.set(color);
    }

    @EntryPoint(value=ShaderStage.Fragment, name="glow")
    public void glow(@In(1) Vec2 texCoord, @Out(0) Vec4 fragColor) {
        Vec4 color = new Vec4(1, 1, 1, 1);
        If(HAS_GLOWCOLOR.use(), () -> {
            color.multLocal(visuals.use().glow.use());
        });
        If(HAS_GLOWMAP.use(), () -> {
            color.multLocal(glowMap.use().sample(texCoord));
        });
        fragColor.set(color);
    }

}
