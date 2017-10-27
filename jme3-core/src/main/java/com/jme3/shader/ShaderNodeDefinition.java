/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
import com.jme3.shader.Shader.ShaderType;

import java.io.IOException;
import java.util.*;

/**
 * Shader node definition structure meant for holding loaded data from a
 * material definition j3md file
 *
 * @author Nehon
 */
public class ShaderNodeDefinition implements Savable {

    private static final String[] EMPTY_STRINGS = new String[0];

    private List<ShaderNodeVariable> inputs = new ArrayList<>();
    private List<ShaderNodeVariable> outputs = new ArrayList<>();
    private Map<String, List<String>> additionalValues = new HashMap<>();

    private List<String> shadersLanguage = new ArrayList<>();
    private List<String> shadersPath = new ArrayList<>();

    private String name;
    private Shader.ShaderType type;
    private String documentation;
    private String path = null;

    private boolean noOutput = false;

    /**
     * creates a ShaderNodeDefinition
     *
     * @param name the name of the definition
     * @param type the type of the shader
     * @param shaderPath the path of the shader
     * @param shaderLanguage the shader language (minimum required for this
     * definition)
     */
    public ShaderNodeDefinition(String name, ShaderType type, String shaderPath, String shaderLanguage) {
        this.name = name;
        this.type = type;
        shadersLanguage.add(shaderLanguage);
        shadersPath.add(shaderPath);
    }

    /**
     * creates a ShaderNodeDefinition
     */
    public ShaderNodeDefinition() {
    }

    /**
     * Gets the list of additional values.
     *
     * @param name the name of values type.
     * @return the list of values or null.
     */
    public List<String> getAdditionalValues(final String name) {
        return additionalValues.get(name);
    }

    /**
     * Gets the list of names of additional values.
     *
     * @return the list of names of values.
     */
    public Set<String> getAdditionalValuesNames() {
        return additionalValues.keySet();
    }

    /**
     * Sets the list of values by the values name.
     *
     * @param name   the name of values type.
     * @param values the list of values.
     */
    public void setAdditionalValues(final String name, final List<String> values) {
        additionalValues.put(name, values);
    }

    /**
     * Removes the list of values by the values name.
     *
     * @param name the name of values type.
     */
    public void removeAdditionalValues(final String name) {
        additionalValues.remove(name);
    }

    /**
     * returns the name of the definition
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * sets the name of the definition
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return the type of shader the definition applies to
     */
    public ShaderType getType() {
        return type;
    }

    /**
     * sets the type of shader this definition applies to
     *
     * @param type the type
     */
    public void setType(ShaderType type) {
        this.type = type;
    }

    /**
     *
     * @return the documentation for this definition
     */
    public String getDocumentation() {
        return documentation;
    }

    /**
     * sets the documentation
     *
     * @param documentation the documentation
     */
    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    /**
     *
     * @return the input variables of this definition
     */
    public List<ShaderNodeVariable> getInputs() {
        return inputs;
    }

    /**
     * sets the input variables of this definition
     *
     * @param inputs the inputs
     */
    public void setInputs(List<ShaderNodeVariable> inputs) {
        this.inputs = inputs;
    }

    /**
     *
     * @return the output variables of this definition
     */
    public List<ShaderNodeVariable> getOutputs() {
        return outputs;
    }

    /**
     * sets the output variables of this definition
     *
     * @param outputs the output
     */
    public void setOutputs(List<ShaderNodeVariable> outputs) {
        this.outputs = outputs;
    }

    /**
     * retrun the path of this definition
     * @return 
     */
    public String getPath() {
        return path;
    }

    /**
     * sets the path of this definition
     * @param path 
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    

    /**
     * jme serialization (not used)
     *
     * @param ex the exporter
     * @throws IOException
     */
    @Override
    public void write(JmeExporter ex) throws IOException {

        final OutputCapsule oc = ex.getCapsule(this);
        oc.write(name, "name", "");

        if (!shadersLanguage.isEmpty()) {
            final String[] array = new String[shadersLanguage.size()];
            oc.write(shadersLanguage.toArray(array), "shadersLanguage", null);
            oc.write(shadersPath.toArray(array), "shadersPath", null);
        }

        oc.write(type, "type", null);

        final Set<String> additionalValuesNames = getAdditionalValuesNames();
        if (!additionalValuesNames.isEmpty()) {
            final String[] names = additionalValuesNames.toArray(new String[additionalValuesNames.size()]);
            oc.write(names, "additionalValuesNames", null);
            for (final String name : names) {

                final List<String> valuesList = getAdditionalValues(name);
                if (valuesList.isEmpty()) {
                    continue;
                }

                final String[] values = valuesList.toArray(new String[valuesList.size()]);
                oc.write(values, "additionalValuesNames_" + name, null);
            }
        }

        oc.writeSavableArrayList((ArrayList) inputs, "inputs", new ArrayList<ShaderNodeVariable>());
        oc.writeSavableArrayList((ArrayList) outputs, "outputs", new ArrayList<ShaderNodeVariable>());
    }

    public List<String> getShadersLanguage() {
        return shadersLanguage;
    }

    public List<String> getShadersPath() {
        return shadersPath;
    }

    public boolean isNoOutput() {
        return noOutput;
    }

    public void setNoOutput(boolean noOutput) {
        this.noOutput = noOutput;
    }

    /**
     * jme serialization (not used)
     *
     * @param im the importer
     * @throws IOException
     */
    @Override
    public void read(JmeImporter im) throws IOException {

        final InputCapsule ic = im.getCapsule(this);
        name = ic.readString("name", "");

        String[] str = ic.readStringArray("shadersLanguage", null);
        if (str != null) {
            shadersLanguage = Arrays.asList(str);
        } else {
            shadersLanguage = new ArrayList<>();
        }

        str = ic.readStringArray("shadersPath", null);
        if (str != null) {
            shadersPath = Arrays.asList(str);
        } else {
            shadersPath = new ArrayList<>();
        }

        final String[] additionalValuesNames = ic.readStringArray("additionalValuesNames", null);

        if (additionalValuesNames != null) {
            for (final String valuesName : additionalValuesNames) {

                final String[] values = ic.readStringArray("additionalValuesNames_" + valuesName, null);
                if (values == null || values.length < 1) {
                    continue;
                }

                final List<String> valuesList = new ArrayList<>(values.length);
                valuesList.addAll(Arrays.asList(values));

                setAdditionalValues(valuesName, valuesList);
            }
        }

        type = ic.readEnum("type", Shader.ShaderType.class, null);
        inputs = (List<ShaderNodeVariable>) ic.readSavableArrayList("inputs", new ArrayList<ShaderNodeVariable>());
        outputs = (List<ShaderNodeVariable>) ic.readSavableArrayList("outputs", new ArrayList<ShaderNodeVariable>());
    }

    /**
     * convenience tostring
     *
     * @return a string
     */
    @Override
    public String toString() {
        return "\nShaderNodeDefinition{\n" + "name=" + name + "\ntype=" + type + "\nshaderPath=" + shadersPath + "\nshaderLanguage=" + shadersLanguage + "\ndocumentation=" + documentation + "\ninputs=" + inputs + ",\noutputs=" + outputs + '}';
    }
}
