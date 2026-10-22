package com.jme3.vulkan.shadergen.vars;

public class Bool extends AbstractVar<Bool> {

    public static final String TYPE = "bool";

    public Bool(boolean b) {
        this(Boolean.toString(b));
    }

    public Bool(String exp) {
        super(TYPE, exp);
    }

    @Override
    public Bool create(String exp) {
        return new Bool(exp);
    }

    public Bool and(Bool b) {
        return new Bool(fmt("(%s&&%s)", exp, b));
    }

    public Bool or(Bool b) {
        return new Bool(fmt("(%s||%s)", exp, b));
    }

}
