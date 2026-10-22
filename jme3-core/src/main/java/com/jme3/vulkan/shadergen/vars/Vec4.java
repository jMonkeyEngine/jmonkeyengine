package com.jme3.vulkan.shadergen.vars;

public class Vec4 extends MathVar<Vec4> {

    public static final String TYPE = "vec4";

    public Vec4() {
        this("vec4()");
    }

    public Vec4(float x, float y, float z, float w) {
        this(fmt("vec4(%f,%f,%f,%f)", x, y, z, w));
    }

    public Vec4(Flt x, Flt y, Flt z, Flt w) {
        this(fmt("vec4(%s,%s,%s,%s)", x, y, z, w));
    }

    public Vec4(Vec2 xy, float z, float w) {
        this(fmt("vec4(%s,%f,%f)", xy, z, w));
    }

    public Vec4(Vec2 xy, Flt z, Flt w) {
        this(fmt("vec4(%s,%s,%s)", xy, z, w));
    }

    public Vec4(Vec3 xyz, float w) {
        this(fmt("vec4(%s,%f)", xyz, w));
    }

    public Vec4(Vec3 xyz, Flt w) {
        this(fmt("vec4(%s,%s)", xyz, w));
    }

    public Vec4(Vec4 xyzw) {
        this(fmt("vec4(%s)", xyzw));
    }

    public Vec4(String exp) {
        super(TYPE, exp);
    }

    @Override
    public Vec4 create(String exp) {
        return new Vec4(exp);
    }

    public Flt x() {
        return new Flt(fmt("%s.x", exp));
    }

    public Flt y() {
        return new Flt(fmt("%s.y", exp));
    }

    public Flt z() {
        return new Flt(fmt("%s.z", exp));
    }

    public Flt w() {
        return new Flt(fmt("%s.w", exp));
    }

    public Flt r() {
        return new Flt(fmt("%s.r", exp));
    }

    public Flt g() {
        return new Flt(fmt("%s.g", exp));
    }

    public Flt b() {
        return new Flt(fmt("%s.b", exp));
    }

    public Flt a() {
        return new Flt(fmt("%s.a", exp));
    }

    public Vec3 xyz() {
        return new Vec3(fmt("%s.xyz", exp));
    }

    public Vec3 rgb() {
        return new Vec3(fmt("%s.xyz", exp));
    }

    public Vec2 swizzle(char x, char y) {
        return new Vec2(fmt("%s.%c%c", exp, x, y));
    }

    public Vec3 swizzle(char x, char y, char z) {
        return new Vec3(fmt("%s.%c%c%c", exp, x, y, z));
    }

    public Vec4 swizzle(char x, char y, char z, char w) {
        return new Vec4(fmt("%s.%c%c%c%c", exp, x, y, z, w));
    }

    public IVec4 ivec4() {
        return new IVec4(fmt("ivec4(%s)", exp));
    }

    public UVec4 uvec4() {
        return new UVec4(fmt("uvec4(%s)", exp));
    }

}
