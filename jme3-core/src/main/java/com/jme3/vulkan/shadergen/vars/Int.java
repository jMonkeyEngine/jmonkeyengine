package com.jme3.vulkan.shadergen.vars;

public class Int extends Num<Int, Integer> {

    public static final String TYPE = "int";

    public Int(String exp) {
        super(TYPE, exp);
    }

    public Int(int i) {
        this(Integer.toString(i));
    }

    public Int(Int i) {
        this(i.toString());
    }

    @Override
    public Int create(String exp) {
        return new Int(exp);
    }

    public Flt flt() {
        return new Flt(fmt("float(%s)", exp));
    }

    public UInt uint() {
        return new UInt(fmt("uint(%s)", exp));
    }

    public Int shiftLeft(int n) {
        return new Int(fmt("(%s<<%s)", exp, n));
    }

    public Int shiftLeft(Int n) {
        return new Int(fmt("(%s<<%s)", exp, n));
    }

    public Int shiftRight(int n) {
        return new Int(fmt("(%s>>%s)", exp, n));
    }

    public Int shiftRight(Int n) {
        return new Int(fmt("(%s>>%s)", exp, n));
    }

    public void shiftLeftLocal(int n) {
        emit("%s<<=%s;", exp, n);
    }

    public void shiftLeftLocal(Int n) {
        emit("%s<<=%s;", exp, n);
    }

    public void shiftRightLocal(int n) {
        emit("%s>>=%s;", exp, n);
    }

    public void shiftRightLocal(Int n) {
        emit("%s>>=%s;", exp, n);
    }

    public Flt add(Flt f) {
        return new Flt(fmt("(%s+%s)", exp, f));
    }

    public Flt sub(Flt f) {
        return new Flt(fmt("(%s-%s)", exp, f));
    }

    public Flt mult(Flt f) {
        return new Flt(fmt("(%s*%s)", exp, f));
    }

    public Flt div(Flt f) {
        return new Flt(fmt("(%s/%s)", exp, f));
    }

}
