package com.jme3.shader.builder;

import com.jme3.shader.*;

import java.util.List;

public class ShaderNodeBuilder {

    private ShaderNode node;

    protected ShaderNodeBuilder(String name, ShaderNodeDefinition def) {
        node = new ShaderNode(name, def, null);
    }

    protected ShaderNodeBuilder(ShaderNode node) {
        this.node = node;
    }

    protected ShaderNode getNode() {
        return node;
    }

    protected ShaderNodeVariable variable(String name){
        ShaderNodeDefinition def = node.getDefinition();
        for (ShaderNodeVariable variable : def.getParams()) {
            if(variable.getName().equals(name)){
                ShaderNodeVariable var = variable.clone();
                var.setNameSpace(node.getName());
                return var;
            }
        }

        for (ShaderNodeVariable variable : def.getOutputs()) {
            if(variable.getName().equals(name)){
                ShaderNodeVariable var = variable.clone();
                var.setNameSpace(node.getName());
                return var;
            }
        }
        return null;
    }

    public ShaderNodeBuilder inputs(VariableMappingBuilder... inputs){
        List<VariableMapping> mappings = node.getInputMapping();
        mappings.clear();
        for (VariableMappingBuilder mb : inputs) {
            ShaderNodeVariable left = findVariable(mb.getName(), node.getDefinition().getInputs() );
            if(left == null){
                throw new IllegalArgumentException("Couldn't find input " + mb.getName() + " in node definition " + node.getDefinition().getName());
            }
            left = left.clone();
            left.setNameSpace(node.getName());
            ShaderNodeVariable right = mb.getVariable();
            VariableMapping m = map(left, right);
            mappings.add(m);
        }
        return this;
    }


    public ShaderNodeBuilder outputs(VariableMappingBuilder... outputs){
        List<VariableMapping> mappings = node.getOutputMapping();
        mappings.clear();
        for (VariableMappingBuilder mb : outputs) {
            ShaderNodeVariable right = findVariable(mb.getName(), node.getDefinition().getOutputs() );
            if(right == null){
                throw new IllegalArgumentException("Couldn't find input " + mb.getName() + " in node definition " + node.getDefinition().getName());
            }
            right = right.clone();
            right.setNameSpace(node.getName());
            ShaderNodeVariable left = mb.getVariable();
            VariableMapping m = map(left, right);
            mappings.add(m);
        }
        return this;
    }


    private VariableMapping map(ShaderNodeVariable left, ShaderNodeVariable right) {
        if(right.getType() == null){
            // tmp variable, with default value
            VariableMapping m = new VariableMapping();
            m.setLeftVariable(left);
            m.setRightExpression(right.getDefaultValue());
            return m;
        }
        int leftCard = ShaderUtils.getCardinality(left.getType(), "");
        int rightCard = ShaderUtils.getCardinality(right.getType(), "");
        String swizzle = "xyzw";
        String rightVarSwizzle = "";
        String leftVarSwizzle ="";
        if (rightCard > leftCard) {
            rightVarSwizzle = swizzle.substring(0, leftCard);
        } else if (rightCard > rightCard) {
            leftVarSwizzle = swizzle.substring(0, rightCard);
        }

        return new VariableMapping(left, leftVarSwizzle, right, rightVarSwizzle, null);
    }

    protected ShaderNodeVariable findVariable(String name, List<ShaderNodeVariable> list){
        for (ShaderNodeVariable variable : list) {
            if(variable.getName().equals(name)){
                return variable;
            }
        }
        return null;
    }

    public void build(){

    }
}
