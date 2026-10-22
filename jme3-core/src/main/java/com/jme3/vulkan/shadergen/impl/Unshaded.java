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
        public final Field<Float, Flt> alphaDiscard = new Field<>(0f, Flt::new);
        public final Field<Float, Flt> desaturation = new Field<>(0f, Flt::new);
        { add(alphaDiscard, desaturation);
          setupLayout(StructLayout.std140); }
    }

    private final Macro HAS_COLOR = new Macro("HAS_COLOR");
    private final Macro HAS_VERTEX_COLOR = new Macro("HAS_VERTEX_COLOR");
    private final Macro DESATURATE = new Macro("DESATURATE");

    private final Specialization<Bool> DISCARD_ALPHA = new Specialization<>(0, Bool(false));
    private final Specialization<UInt> NUM_BONES = new Specialization<>(1, UInt(0));
    private final Specialization<UInt> MORPH_TARGETS = new Specialization<>(2, UInt(0));

    private final PushConstants<Transforms> transforms = new PushConstants<>(new Transforms());
    private final Uniform<Visuals> visuals = new Uniform<>(1, 0, new Visuals());

    @EntryPoint(ShaderStage.Vertex)
    public void vertex(@In(0) Vec3 inPosition, @In(1) Vec2 inTexCoord, @In(2) Vec2 inTexCoord2, @In(3) Vec4 inColor,
                       @Out(0) Vec2 outTexCoord, @Out(1) Vec2 outTexCoord2, @Out(2) Vec4 outColor) {
        outTexCoord.set(inTexCoord);
        outTexCoord2.set(inTexCoord2);
        outColor.set(inColor);
        Vec4 modelSpacePos = new Vec4(inPosition, 1.0f);
        Skinning skin = Import(new Skinning());
        If (MORPH_TARGETS.use().gt(0), () -> {
            skin.morphCompute(modelSpacePos);
        });
        If (NUM_BONES.use().gt(0), () -> {
            skin.skinningCompute(modelSpacePos);
        });
        gl.clipPosition.set(transforms.use().worldViewProjection.use().mult(modelSpacePos));
    }

    @EntryPoint(ShaderStage.Fragment)
    public void fragment(@In(0) Vec2 texCoord, @In(1) Vec2 texCoord2, @In(2) Vec4 vertColor, @Out(0) Vec4 fragColor) {
        Vec4 color = new Vec4(1f, 1f, 1f, 1f);
        IfDirEnd (HAS_VERTEX_COLOR.defined(), () -> {
            color.multLocal(vertColor);
        });
        IfDirEnd (HAS_COLOR.defined(), () -> {
            color.multLocal(visuals.use().color.use());
        });
        If (DISCARD_ALPHA.use(), () -> {
            If(color.a().lt(visuals.use().alphaDiscard.use()), this::Discard);
        });
        IfDirEnd (DESATURATE.defined(), () -> {
            Vec3 gray = new Vec3(new Vec3(0.2126f, 0.7152f, 0.0722f).dot(color.rgb()));
            color.rgb().set(color.rgb().mix(gray, visuals.desaturation.use()));
        });
        fragColor.set(color);
    }

}
