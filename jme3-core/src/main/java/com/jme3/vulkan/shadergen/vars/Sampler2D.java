package com.jme3.vulkan.shadergen.vars;

public class Sampler2D extends AbstractVar<Sampler2D> {

    public static final String TYPE = "sampler2D";

    public Sampler2D() {
        this("");
    }

    public Sampler2D(String exp) {
        super(TYPE, exp);
    }

    @Override
    public Sampler2D create(String exp) {
        return new Sampler2D(exp);
    }

    public Vec4 sample(Vec2 coord) {
        return new Vec4(fmt("texture(%s, %s)", exp, coord));
    }

    public Vec4 sample(Vec2 coord, Flt bias) {
        return new Vec4(fmt("texture(%s, %s, %s)", exp, coord, bias));
    }

    public Vec4 sampleLod(Vec2 coord, Flt lod) {
        return new Vec4(fmt("textureLod(%s, %s, %s)", exp, coord, lod));
    }

    public Vec4 sampleGrad(Vec2 coord, Flt dx, Flt dy) {
        return new Vec4(fmt("textureGrad(%s, %s, %s, %s)", exp, coord, dx, dy));
    }

    public IVec2 size(Int lod) {
        return new IVec2(fmt("textureSize(%s,%s)", exp, lod));
    }

    public Int levels() {
        return new Int(fmt("textureQueryLevels(%s)", exp));
    }

}
