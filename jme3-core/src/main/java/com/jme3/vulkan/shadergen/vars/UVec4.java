package com.jme3.vulkan.shadergen.vars;

public final class UVec4 extends MathVar<UVec4> {

    public static final String TYPE = "uvec4";

    public UVec4() {
        this("uvec4()");
    }

    public UVec4(int x, int y, int z, int w) {
        this(fmt("uvec4(%d,%d,%d,%d)", x, y, z, w));
    }

    public UVec4(UInt x, UInt y, UInt z, UInt w) {
        this(fmt("uvec4(%s,%s,%s,%s)", x, y, z, w));
    }

    public UVec4(UVec2 xy, int z, int w) {
        this(fmt("uvec4(%s,%d,%d)", xy, z, w));
    }

    public UVec4(UVec2 xy, UInt z, UInt w) {
        this(fmt("uvec4(%s,%s,%s)", xy, z, w));
    }

    public UVec4(UVec3 xyz, int w) {
        this(fmt("uvec4(%s,%d)", xyz, w));
    }

    public UVec4(UVec3 xyz, UInt w) {
        this(fmt("uvec4(%s,%s)", xyz, w));
    }

    public UVec4(UVec4 xyzw) {
        this(fmt("uvec4(%s)", xyzw));
    }

    public UVec4(String exp) {
        super(TYPE, exp);
    }

    @Override
    public UVec4 create(String exp) {
        return new UVec4(exp);
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

    public UInt w() {
        return new UInt(fmt("%s.w", exp));
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

    public UInt a() {
        return new UInt(fmt("%s.a", exp));
    }

    public UVec2 swizzle(char x, char y) {
        return new UVec2(fmt("%s.%c%c", exp, x, y));
    }

    public UVec3 swizzle(char x, char y, char z) {
        return new UVec3(fmt("%s.%c%c%c", exp, x, y, z));
    }

    public UVec4 swizzle(char x, char y, char z, char w) {
        return new UVec4(fmt("%s.%c%c%c%c", exp, x, y, z, w));
    }

    public Vec4 vec4() {
        return new Vec4(fmt("vec4(%s)", exp));
    }

    public IVec4 ivec4() {
        return new IVec4(fmt("ivec4(%s)", exp));
    }

}
