package com.jme3.vulkan.shadergen.vars;

public final class Vec2 extends MathVar<Vec2> {

    public static final String TYPE = "vec2";

    public Vec2() {
         this("vec2()");
    }

    public Vec2(float x, float y) {
        this(fmt("vec2(%f,%f)", x, y));
    }

    public Vec2(Flt x, Flt y) {
        this(fmt("vec2(%s,%s)", x, y));
    }

    public Vec2(String exp) {
        super(TYPE, exp);
    }

    @Override
    public Vec2 create(String exp) {
        return new Vec2(exp);
    }

    public Vec2 x(float x) {
        emit("%s.x=%f;", exp, x);
        return this;
    }

    public Vec2 x(Flt x) {
        emit("%s.x=%s;", exp, x);
        return this;
    }

    public Vec2 y(float y) {
        emit("%s.y=%f;", exp, y);
        return this;
    }

    public Vec2 y(Flt y) {
        emit("%s.y=%s;", exp, y);
        return this;
    }

    public Flt x() {
        return new Flt(fmt("%s.x", exp));
    }

    public Flt y() {
        return new Flt(fmt("%s.y", exp));
    }

    public Vec2 xy() {
        return new Vec2(fmt("%s.xy", exp));
    }

    public Vec2 yx() {
        return new Vec2(fmt("%s.yx", exp));
    }

    public Vec3 z(float z) {
        return new Vec3(fmt("vec3(%s, %f)", exp, z));
    }

    public Vec3 z(Flt z) {
        return new Vec3(fmt("vec3(%s, %s)", exp, z));
    }

    public Vec4 zw(float z, float w) {
        return new Vec4(fmt("vec3(%s, %f, %f)", exp, z, w));
    }

    public Vec4 zw(Flt z, Flt w) {
        return new Vec4(fmt("vec3(%s, %s, %s)", exp, z, w));
    }

    public IVec2 ivec2() {
        return new IVec2(fmt("ivec2(%s)", exp));
    }

    public UVec2 uvec2() {
        return new UVec2(fmt("uvec2(%s)", exp));
    }

}
