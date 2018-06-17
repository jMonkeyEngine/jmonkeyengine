package com.jme3.shader.builder;

import com.jme3.shader.ShaderNodeVariable;

public class VariableMappingBuilder {

    private String name;
    private ShaderNodeVariable variable;

    protected VariableMappingBuilder(String name, ShaderNodeVariable variable) {
        this.name = name;
        this.variable = variable;
    }

    public String getName() {
        return name;
    }

    public ShaderNodeVariable getVariable() {
        return variable;
    }

}
