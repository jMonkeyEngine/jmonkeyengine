package com.jme3.vulkan.shadergen.vars;

public final class IVec4 extends MathVar<IVec4> {

    public static final String TYPE = "ivec4";

    public IVec4() {
        this("ivec4()");
    }

    public IVec4(int x, int y, int z, int w) {
        this(fmt("ivec4(%d,%d,%d,%d)", x, y, z, w));
    }

    public IVec4(Int x, Int y, Int z, Int w) {
        this(fmt("ivec4(%s,%s,%s,%s)", x, y, z, w));
    }

    public IVec4(IVec2 xy, int z, int w) {
        this(fmt("ivec4(%s,%d,%d)", xy, z, w));
    }

    public IVec4(IVec2 xy, Int z, Int w) {
        this(fmt("ivec4(%s,%s,%s)", xy, z, w));
    }

    public IVec4(IVec3 xyz, int w) {
        this(fmt("ivec4(%s,%d)", xyz, w));
    }

    public IVec4(IVec3 xyz, Int w) {
        this(fmt("ivec4(%s,%s)", xyz, w));
    }

    public IVec4(IVec4 xyzw) {
        this(fmt("ivec4(%s)", xyzw));
    }

    public IVec4(String exp) {
        super(TYPE, exp);
    }

    @Override
    public IVec4 create(String exp) {
        return new IVec4(exp);
    }

    public Int x() {
        return new Int(fmt("%s.x", exp));
    }

    public Int y() {
        return new Int(fmt("%s.y", exp));
    }

    public Int z() {
        return new Int(fmt("%s.z", exp));
    }

    public Int w() {
        return new Int(fmt("%s.w", exp));
    }

    public Int r() {
        return new Int(fmt("%s.r", exp));
    }

    public Int g() {
        return new Int(fmt("%s.g", exp));
    }

    public Int b() {
        return new Int(fmt("%s.b", exp));
    }

    public Int a() {
        return new Int(fmt("%s.a", exp));
    }

    public IVec2 swizzle(char x, char y) {
        return new IVec2(fmt("%s.%c%c", exp, x, y));
    }

    public IVec3 swizzle(char x, char y, char z) {
        return new IVec3(fmt("%s.%c%c%c", exp, x, y, z));
    }

    public IVec4 swizzle(char x, char y, char z, char w) {
        return new IVec4(fmt("%s.%c%c%c%c", exp, x, y, z, w));
    }

    public Vec4 vec4() {
        return new Vec4(fmt("vec4(%s)", exp));
    }

    public UVec4 uvec4() {
        return new UVec4(fmt("uvec4(%s)", exp));
    }

}
