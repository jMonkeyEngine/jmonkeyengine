package com.jme3.shader.builder;

import com.jme3.material.TechniqueDef;
import com.jme3.shader.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InlineShaderNodeBuilder extends ShaderNodeBuilder {


    private TechniqueDef technique;
    private Pattern varPattern = Pattern.compile("%(\\w+)");
    private String[] outputTypes;

    public InlineShaderNodeBuilder(String name, ShaderNodeDefinition def, String code, TechniqueDef technique) {
        super(name, def);
        this.technique = technique;
        Matcher m = varPattern.matcher(code);
        while (m.find()) {
            // type will be inferred with mapping
            ShaderNodeVariable v = new ShaderNodeVariable(null, m.group(1));
            def.getParams().add(v);
            def.getInputs().add(v);
        }
        def.setInlinedCode(code.replaceAll("%", ""));
        def.getOutputs().add(new ShaderNodeVariable(def.getReturnType(), "result"));
    }

    @Override
    public InlineShaderNodeBuilder inputs(VariableMappingBuilder... inputs) {
        ShaderNodeDefinition def = getNode().getDefinition();
        for (VariableMappingBuilder map : inputs) {
            ShaderNodeVariable v = findVariable(map.getName(), def.getInputs());

            def.getInputs().add(v);
            ShaderNodeVariable right = map.getVariable();
            if (right.getDefaultValue() != null) {
                throw new IllegalArgumentException("Inlined expression for input " + v.getName()
                        + " is not supported with inline node " + getNode().getName()
                        + ". Please inline the expression in the node code.");
            }
            // infer type
            int idx = right.getType().indexOf("|");
            if (idx > 0) {
                // texture type, taking the first available type
                String type = right.getType().substring(0, right.getType().indexOf("|"));
                right.setType(type);
            }

            v.setType(right.getType());

        }
        super.inputs(inputs);
        return this;
    }

    @Override
    public InlineShaderNodeBuilder outputs(VariableMappingBuilder... outputs) {
        if (outputs.length > 1 || !outputs[0].getName().equals("result")) {
            throw new IllegalArgumentException("Only the 'result' output can be mapped for an inlined node");
        }
        super.outputs(outputs);
        return this;
    }

    @Override
    public void build() {
        ShaderNodeDefinition def = getNode().getDefinition();
        //generate the code
        StringBuilder sb = new StringBuilder();
        sb.append(def.getReturnType()).append(" ").append(def.getName()).append("(");
        boolean isFirst = true;
        int outTypeIndex = 0;
        for (ShaderNodeVariable v : def.getParams()) {
            if (!isFirst) {
                sb.append(", ");
            }
            sb.append("const in ");
            if (def.getInputs().contains(v)) {

            } else {
                sb.append("out ");
                if (!def.getOutputs().contains(v)) {
                    // the variable is not in the output list
                    def.getOutputs().add(v);
                }
                if (v.getType() == null && outTypeIndex < outputTypes.length) {
                    v.setType(outputTypes[outTypeIndex]);
                } else {
                    throw new IllegalArgumentException("Output variable " + v.getName() + " has no type in node " + getNode().getName() + ". Make sure you properly declare it");
                }
            }
            if (v.getType() == null) {
                throw new IllegalArgumentException("Unable to infer type for input variable " + v.getName() + " in node " + getNode().getName());
            }
            sb.append(v.getType()).append(" ");
            sb.append(v.getName());
            isFirst = false;
        }
        sb.append("){\n\treturn ").append(def.getInlinedCode()).append(";\n}");
        def.setInlinedCode(sb.toString());
        super.build();
    }
}
