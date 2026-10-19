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

}
