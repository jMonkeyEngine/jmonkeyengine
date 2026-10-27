package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.shadergen.vars.Var;

public class Specialization <T extends Var> extends Global<T> {

    public Specialization(int id, T value) {
        super(null, 0, Location.constant(id), value);
    }

    @Override
    protected void compile(ShaderLogic logic, T value, T ref) {
        logic.add("layout(constant_id=%d) const %s %s=%s;", getLocation().getBinding(), value.getType(), ref, value);
    }

}
