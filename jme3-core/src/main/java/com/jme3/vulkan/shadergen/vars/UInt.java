package com.jme3.vulkan.shadergen.vars;

public class UInt extends Num<UInt, Integer> {

    public static final String TYPE = "uint";

    public UInt(String exp) {
        super(TYPE, exp);
    }

    public UInt(int i) {
        this(Integer.toUnsignedString(i) + "u");
    }

    public UInt(UInt i) {
        this(i.toString());
    }

    @Override
    public UInt create(String exp) {
        return new UInt(exp);
    }

    public Flt flt() {
        return new Flt(fmt("float(%s)", exp));
    }

    public Int Int() {
        return new Int(fmt("int(%s)", exp));
    }

    public UInt shiftLeft(int n) {
        return new UInt(fmt("(%s<<%s)", exp, n));
    }

    public UInt shiftLeft(UInt n) {
        return new UInt(fmt("(%s<<%s)", exp, n));
    }

    public UInt shiftRight(int n) {
        return new UInt(fmt("(%s>>%s)", exp, n));
    }

    public UInt shiftRight(UInt n) {
        return new UInt(fmt("(%s>>%s)", exp, n));
    }

    public void shiftLeftLocal(int n) {
        emit("%s<<=%s;", exp, n);
    }

    public void shiftLeftLocal(UInt n) {
        emit("%s<<=%s;", exp, n);
    }

    public void shiftRightLocal(int n) {
        emit("%s>>=%s;", exp, n);
    }

    public void shiftRightLocal(UInt n) {
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
