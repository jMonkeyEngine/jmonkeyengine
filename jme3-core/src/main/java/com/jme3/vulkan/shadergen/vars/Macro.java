package com.jme3.vulkan.shadergen.vars;

public class Macro implements Var {

    private final String name;

    public Macro(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return "NULL_TYPE";
    }

    @Override
    public Macro declare() {
        return this;
    }

    @Override
    public Macro declare(String variableName) {
        return this;
    }

    @Override
    public Macro create(String exp) {
        return new Macro(exp);
    }

    public Bool defined() {
        return new Bool(String.format("defined(%s)", name));
    }

    public Bool asBoolean() {
        return new Bool(name);
    }

    public Int asInt() {
        return new Int(name);
    }

    public UInt asUInt() {
        return new UInt(name);
    }

    public Flt asFloat() {
        return new Flt(name);
    }

}
