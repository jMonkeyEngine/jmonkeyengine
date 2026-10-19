package com.jme3.vulkan.shadergen.vars;

public class UVec3 extends MathVar<UVec3> {

    public static final String TYPE = "uvec3";

    public UVec3() {
        this("uvec3()");
    }

    public UVec3(int x, int y, int z) {
        this(fmt("uvec3(%d,%d,%d)", x, y, z));
    }

    public UVec3(UInt x, UInt y, UInt z) {
        this(fmt("uvec3(%s,%s,%s)", x, y, z));
    }

    public UVec3(UVec2 xy, int z) {
        this(fmt("uvec3(%s,%d)", xy, z));
    }

    public UVec3(UVec2 xy, UInt z) {
        this(fmt("uvec3(%s,%s)", xy, z));
    }

    public UVec3(UVec3 xyz) {
        this(fmt("uvec3(%s)", xyz));
    }

    public UVec3(String exp) {
        super(TYPE, exp);
    }

    @Override
    public UVec3 create(String exp) {
        return new UVec3(exp);
    }

    public UInt x() {
        return new UInt(fmt("%s.x", exp));
    }

    public UInt y() {
        return new UInt(fmt("%s.y", exp));
    }

    public UInt z() {
        return new UInt(fmt("%s.z", exp));
    }

    public UInt r() {
        return new UInt(fmt("%s.r", exp));
    }

    public UInt g() {
        return new UInt(fmt("%s.g", exp));
    }

    public UInt b() {
        return new UInt(fmt("%s.b", exp));
    }

    public UVec2 swizzle(char x, char y) {
        return new UVec2(fmt("%s.%c%c", exp, x, y));
    }

    public UVec3 swizzle(char x, char y, char z) {
        return new UVec3(fmt("%s.%c%c%c", exp, x, y, z));
    }

    public Vec3 vec3() {
        return new Vec3(fmt("vec3(%s)", exp));
    }

    public IVec3 ivec3() {
        return new IVec3(fmt("ivec3(%s)", exp));
    }

}
