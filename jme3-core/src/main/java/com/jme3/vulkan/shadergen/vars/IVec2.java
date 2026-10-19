package com.jme3.vulkan.shadergen.vars;

public final class IVec2 extends MathVar<IVec2> {

    public static final String TYPE = "ivec2";

    public IVec2() {
        this("ivec2()");
    }

    public IVec2(int x, int y) {
        this(fmt("ivec2(%d,%d)", x, y));
    }

    public IVec2(Int x, Int y) {
        this(fmt("ivec2(%s,%s)", x, y));
    }

    public IVec2(String exp) {
        super(TYPE, exp);
    }

    @Override
    public IVec2 create(String exp) {
        return new IVec2(exp);
    }

    public IVec2 x(int x) {
        emit("%s.x=%d;", exp, x);
        return this;
    }

    public IVec2 x(Int x) {
        emit("%s.x=%s;", exp, x);
        return this;
    }

    public IVec2 y(int y) {
        emit("%s.y=%d;", exp, y);
        return this;
    }

    public IVec2 y(Int y) {
        emit("%s.y=%s;", exp, y);
        return this;
    }

    public Int x() {
        return new Int(fmt("%s.x", exp));
    }

    public Int y() {
        return new Int(fmt("%s.y", exp));
    }

    public IVec2 xy() {
        return new IVec2(fmt("%s.xy", exp));
    }

    public IVec2 yx() {
        return new IVec2(fmt("%s.yx", exp));
    }

    public IVec3 z(int z) {
        return new IVec3(fmt("ivec3(%s,%d)", exp, z));
    }

    public IVec3 z(Int z) {
        return new IVec3(fmt("ivec3(%s,%s)", exp, z));
    }

    public IVec4 zw(int z, int w) {
        return new IVec4(fmt("ivec4(%s,%d,%d)", exp, z, w));
    }

    public IVec4 zw(Int z, Int w) {
        return new IVec4(fmt("ivec4(%s,%s,%s)", exp, z, w));
    }

    public Vec2 vec2() {
        return new Vec2(fmt("vec2(%s)", exp));
    }

    public UVec2 uvec2() {
        return new UVec2(fmt("uvec2(%s)", exp));
    }

}
