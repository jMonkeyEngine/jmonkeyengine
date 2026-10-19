package com.jme3.vulkan.shadergen.vars;

import com.jme3.vulkan.shadergen.ShaderLogic;

public abstract class AbstractVar <SELF extends AbstractVar> implements Var {

    protected final ShaderLogic logic = ShaderLogic.get();
    protected final String type;
    protected String exp;

    public AbstractVar(String type, String exp) {
        this.type = type;
        this.exp = exp;
    }

    @Override
    public String toString() {
        return exp;
    }

    @Override @SuppressWarnings("unchecked")
    public SELF declare() {
        String exp = this.exp;
        emit("%s %s=%s;", type, this.exp = logic.generateVariableName(), exp);
        return (SELF)this;
    }

    @Override @SuppressWarnings("unchecked")
    public SELF declare(String variableName) {
        String exp = this.exp;
        emit("%s %s=%s;", type, this.exp = logic.generateVariableName(), exp);
        return (SELF)this;
    }

    @Override
    public String getType() {
        return type;
    }

    public void set(SELF v) {
        emit(fmt("%s=%s;", exp, v.exp));
    }

    protected void emit(String code, Object... args) {
        logic.add(fmt(code, args));
    }

    protected void emit(String code) {
        logic.add(code);
    }

    protected static String fmt(String format, Object... args) {
        return String.format(format, args);
    }

}
