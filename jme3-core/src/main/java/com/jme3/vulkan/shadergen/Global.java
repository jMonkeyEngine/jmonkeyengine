package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.shadergen.vars.Var;

public interface Global <T extends Var> {

    T use();

    T compile();

    Global getOverridenGlobal();

    default boolean isOverriding() {
        return getOverridenGlobal() != null;
    }

}
