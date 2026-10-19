package com.jme3.vulkan.shadergen.vars;

public final class Flt extends Num<Flt, Float> {

    public static final String TYPE = "float";

    public Flt(String exp) {
        super(TYPE, exp);
    }

    public Flt(float f) {
        this(Float.toString(f));
    }

    @Override
    public Flt create(String exp) {
        return new Flt(exp);
    }

    public Int Int() {
        return new Int(fmt("int(%s)", exp));
    }

    public Flt add(Int i) {
        return new Flt(fmt("(%s+%s)", exp, i));
    }

    public Flt sub(Int i) {
        return new Flt(fmt("(%s-%s)", exp, i));
    }

    public Flt mult(Int i) {
        return new Flt(fmt("(%s*%s)", exp, i));
    }

    public Flt div(Int i) {
        return new Flt(fmt("(%s/%s)", exp, i));
    }

    public Flt addLocal(Int i) {
        emit("%s+=%;", exp, i);
        return this;
    }

    public Flt subLocal(Int i) {
        emit("%s-=%s;", exp, i);
        return this;
    }

    public Flt multLocal(Int i) {
        emit("%s*=%s;", exp, i);
        return this;
    }

    public Flt divLocal(Int i) {
        emit("%s/=%s;", exp, i);
        return this;
    }

}
