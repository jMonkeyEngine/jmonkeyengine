package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.shadergen.vars.Var;

public interface Global<K, T extends Var> {

    T use();

    T compile();

    K getKey();

}
