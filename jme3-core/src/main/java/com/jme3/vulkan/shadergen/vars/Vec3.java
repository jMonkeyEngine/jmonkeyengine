package com.jme3.vulkan.shadergen.vars;

public class Vec3 extends MathVar<Vec3> {

    public static final String TYPE = "vec3";

    public Vec3() {
        this("vec3()");
    }

    public Vec3(float x, float y, float z) {
        this(fmt("vec3(%f,%f,%f)", x, y, z));
    }

    public Vec3(Flt x, Flt y, Flt z) {
        this(fmt("vec3(%s,%s,%s)", x, y, z));
    }

    public Vec3(Vec2 xy, float z) {
        this(fmt("vec3(%s,%f)", xy, z));
    }

    public Vec3(Vec2 xy, Flt z) {
        this(fmt("vec3(%s,%s)", xy, z));
    }

    public Vec3(Vec3 xyz) {
        this(fmt("vec3(%s)", xyz));
    }

    public Vec3(String exp) {
        super(TYPE, exp);
    }

    @Override
    public Vec3 create(String exp) {
        return new Vec3(exp);
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

    public Flt r() {
        return new Flt(fmt("%s.r", exp));
    }

    public Flt g() {
        return new Flt(fmt("%s.g", exp));
    }

    public Flt b() {
        return new Flt(fmt("%s.b", exp));
    }

    public Vec2 swizzle(char x, char y) {
        return new Vec2(fmt("%s.%c%c", exp, x, y));
    }

    public Vec3 swizzle(char x, char y, char z) {
        return new Vec3(fmt("%s.%c%c%c", exp, x, y, z));
    }

    public IVec3 ivec3() {
        return new IVec3(fmt("ivec3(%s)", exp));
    }

    public UVec3 uvec3() {
        return new UVec3(fmt("uvec3(%s)", exp));
    }

}
