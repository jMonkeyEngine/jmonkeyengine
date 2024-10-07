/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.shader;

import com.jme3.export.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A ShaderNode is the unit brick part of a shader program. A shader can be
 * described by several shader nodes that are plugged together through inputs
 * and outputs.
 *
 * A ShaderNode is based on a definition that has a shader code, inputs and
 * output variables. This node can be activated based on a condition, and has
 * input and output mapping.
 *
 * This class is not intended to be used by JME users directly. It's the
 * structure for loading shader nodes from a J3md material definition file
 *
 * @author Nehon
 */
public class ShaderNode implements Savable, Cloneable {

    private String name;
    private ShaderNodeDefinition definition;
    private String condition;

    private List<VariableMapping> inputMapping = new ArrayList<>();
    private List<VariableMapping> outputMapping = new ArrayList<>();

    /**
     * Creates a shader node.
     *
     * @param name       the name.
     * @param definition the shader node definition.
     * @param condition  the condition to activate this node.
     */
    public ShaderNode(String name, ShaderNodeDefinition definition, String condition) {
        this.name = name;
        this.definition = definition;
        this.condition = condition;
    }

    /**
     * Creates a shader node.
     */
    public ShaderNode() {
    }

    /**
     * Gets the name of the node.
     *
     * @return the name of the node
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the node.
     *
     * @param name the name of the node.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the shader node definition.
     *
     * @return the shader node definition.
     */
    public ShaderNodeDefinition getDefinition() {
        return definition;
    }

    /**
     * Sets the shader node definition.
     *
     * @param definition the shader node definition.
     */
    public void setDefinition(ShaderNodeDefinition definition) {
        this.definition = definition;
    }

    /**
     * Gets the condition.
     *
     * @return the condition.
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Sets the condition.
     *
     * @param condition the condition.
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * Returns a list of variable mapping representing the input mappings of this
     * node.
     *
     * @return the input mappings.
     */
    public List<VariableMapping> getInputMapping() {
        return inputMapping;
    }

    /**
     * Sets the input mappings.
     *
     * @param inputMapping the input mappings.
     */
    public void setInputMapping(List<VariableMapping> inputMapping) {
        this.inputMapping = inputMapping;
    }

    /**
     * Returns a list of variable mapping representing the output mappings of this
     * node.
     *
     * @return the output mappings.
     */
    public List<VariableMapping> getOutputMapping() {
        return outputMapping;
    }

    /**
     * Sets the output mappings.
     *
     * @param outputMapping the output mappings.
     */
    public void setOutputMapping(List<VariableMapping> outputMapping) {
        this.outputMapping = outputMapping;
    }

    /**
     * jme serialization
     *
     * @param ex the exporter
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(name, "name", "");
        oc.write(definition, "definition", null);
        oc.write(condition, "condition", null);
        oc.writeSavableArrayList((ArrayList) inputMapping, "inputMapping", new ArrayList<VariableMapping>());
        oc.writeSavableArrayList((ArrayList) outputMapping, "outputMapping", new ArrayList<VariableMapping>());
    }

    /**
     * jme serialization
     *
     * @param im the importer
     * @throws IOException from the importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        name = ic.readString("name", "");
        definition = (ShaderNodeDefinition) ic.readSavable("definition", null);
        condition = ic.readString("condition", null);
        inputMapping = ic.readSavableArrayList("inputMapping", new ArrayList<>());
        outputMapping = ic.readSavableArrayList("outputMapping", new ArrayList<>());
    }

    /**
     * convenience toString
     *
     * @return a string
     */
    @Override
    public String toString() {

        final StringBuilder builder = new StringBuilder("ShaderNode:");
        builder.append("\n\tname=").append(name)
                .append("\n\tdefinition=").append(definition.getName())
                .append("\n\tcondition=").append(condition);

        if (!inputMapping.isEmpty()) {
            builder.append("\n\tinputMapping:\n");
            for (final VariableMapping mapping : inputMapping) {
                builder.append("\t\t").append(mapping).append('\n');
            }
        }

        if (!outputMapping.isEmpty()) {
            builder.append("\n\toutputMapping:\n");
            for (final VariableMapping mapping : outputMapping) {
                builder.append("\t\t").append(mapping).append('\n');
            }
        }

        if (builder.charAt(builder.length() - 1) == '\n') {
            builder.delete(builder.length() - 1, builder.length());
        }

        return builder.toString();
    }

    @Override
    public ShaderNode clone() throws CloneNotSupportedException {
        ShaderNode clone = (ShaderNode) super.clone();

        // No need to clone the definition.
        clone.definition = definition;

        clone.inputMapping = new ArrayList<>();
        for (VariableMapping variableMapping : inputMapping) {
            clone.inputMapping.add(variableMapping.clone());
        }

        clone.outputMapping = new ArrayList<>();
        for (VariableMapping variableMapping : outputMapping) {
            clone.outputMapping.add(variableMapping.clone());
        }

        return clone;
    }
}
