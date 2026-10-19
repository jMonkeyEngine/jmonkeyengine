package com.jme3.vulkan.shadergen.vars;

public final class UVec2 extends MathVar<UVec2> {

    public static final String TYPE = "uvec2";

    public UVec2() {
        this("uvec2()");
    }

    public UVec2(int x, int y) {
        this(fmt("uvec2(%d,%d)", x, y));
    }

    public UVec2(UInt x, UInt y) {
        this(fmt("uvec2(%s,%s)", x, y));
    }

    public UVec2(String exp) {
        super(TYPE, exp);
    }

    @Override
    public UVec2 create(String exp) {
        return new UVec2(exp);
    }

    public UVec2 x(int x) {
        emit("%s.x=%d;", exp, x);
        return this;
    }

    public UVec2 x(UInt x) {
        emit("%s.x=%s;", exp, x);
        return this;
    }

    public UVec2 y(int y) {
        emit("%s.y=%d;", exp, y);
        return this;
    }

    public UVec2 y(UInt y) {
        emit("%s.y=%s;", exp, y);
        return this;
    }

    public UInt x() {
        return new UInt(fmt("%s.x", exp));
    }

    public UInt y() {
        return new UInt(fmt("%s.y", exp));
    }

    public UVec2 xy() {
        return new UVec2(fmt("%s.xy", exp));
    }

    public UVec2 yx() {
        return new UVec2(fmt("%s.yx", exp));
    }

    public UVec3 z(int z) {
        return new UVec3(fmt("uvec3(%s,%d)", exp, z));
    }

    public UVec3 z(UInt z) {
        return new UVec3(fmt("uvec3(%s,%s)", exp, z));
    }

    public UVec4 zw(int z, int w) {
        return new UVec4(fmt("uvec4(%s,%d,%d)", exp, z, w));
    }

    public UVec4 zw(UInt z, UInt w) {
        return new UVec4(fmt("uvec4(%s,%s,%s)", exp, z, w));
    }

    public Vec2 vec2() {
        return new Vec2(fmt("vec2(%s)", exp));
    }

    public IVec2 ivec2() {
        return new IVec2(fmt("ivec2(%s)", exp));
    }

}
