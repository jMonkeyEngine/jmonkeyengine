package com.jme3.vulkan.shadergen.vars;

public class IVec3 extends MathVar<IVec3> {

    public static final String TYPE = "ivec3";

    public IVec3() {
        this("ivec3()");
    }

    public IVec3(int x, int y, int z) {
        this(fmt("ivec3(%d,%d,%d)", x, y, z));
    }

    public IVec3(Int x, Int y, Int z) {
        this(fmt("ivec3(%s,%s,%s)", x, y, z));
    }

    public IVec3(IVec2 xy, int z) {
        this(fmt("ivec3(%s,%d)", xy, z));
    }

    public IVec3(IVec2 xy, Int z) {
        this(fmt("ivec3(%s,%s)", xy, z));
    }

    public IVec3(IVec3 xyz) {
        this(fmt("ivec3(%s)", xyz));
    }

    public IVec3(String exp) {
        super(TYPE, exp);
    }

    @Override
    public IVec3 create(String exp) {
        return new IVec3(exp);
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

    public Int r() {
        return new Int(fmt("%s.r", exp));
    }

    public Int g() {
        return new Int(fmt("%s.g", exp));
    }

    public Int b() {
        return new Int(fmt("%s.b", exp));
    }

    public IVec2 swizzle(char x, char y) {
        return new IVec2(fmt("%s.%c%c", exp, x, y));
    }

    public IVec3 swizzle(char x, char y, char z) {
        return new IVec3(fmt("%s.%c%c%c", exp, x, y, z));
    }

    public Vec3 vec3() {
        return new Vec3(fmt("vec3(%s)", exp));
    }

    public UVec3 uvec3() {
        return new UVec3(fmt("uvec3(%s)", exp));
    }

}
