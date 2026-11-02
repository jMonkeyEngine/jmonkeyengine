package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.shadergen.vars.Var;

/**
 * A specialization constant: a constant whose value can be given when a pipeline is created, without
 * recompiling the shader. It occupies no descriptor binding.
 *
 * @param <T> type of the constant
 */
public class Specialization <T extends Var> extends BlockedGlobal<ConstantId, T> {

    /**
     * @param id constant id the value is specialized by
     * @param value default value of the constant
     */
    public Specialization(int id, T value) {
        super(new ConstantId(id), value);
    }

    @Override
    protected void compile(ShaderLogic logic, T value, T ref) {
        logic.add("layout(constant_id=%d) const %s %s=%s;", getKey().getId(), value.getType(), ref, value);
    }

}
