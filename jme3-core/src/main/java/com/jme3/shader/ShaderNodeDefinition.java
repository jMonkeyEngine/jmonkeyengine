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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.shader.Shader.ShaderType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shader node definition structure meant for holding loaded data from a
 * material definition j3md file
 *
 * @author Nehon
 */
public class ShaderNodeDefinition implements Savable {

    private String name;
    private Shader.ShaderType type;
    private List<String> shadersLanguage = new ArrayList<String>();
    private List<String> shadersPath = new ArrayList<String>();
    private String documentation;
    private List<ShaderNodeVariable> inputs = new ArrayList<ShaderNodeVariable>();
    private List<ShaderNodeVariable> outputs = new ArrayList<ShaderNodeVariable>();
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
        OutputCapsule oc = (OutputCapsule) ex.getCapsule(this);
        oc.write(name, "name", "");
        String[] str = new String[shadersLanguage.size()];
        oc.write(shadersLanguage.toArray(str), "shadersLanguage", null);
        oc.write(shadersPath.toArray(str), "shadersPath", null);
        oc.write(type, "type", null);
        oc.writeSavableArrayList((ArrayList) inputs, "inputs", new ArrayList<ShaderNodeVariable>());
        oc.writeSavableArrayList((ArrayList) outputs, "inputs", new ArrayList<ShaderNodeVariable>());
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
        InputCapsule ic = (InputCapsule) im.getCapsule(this);
        name = ic.readString("name", "");

        String[] str = ic.readStringArray("shadersLanguage", null);
        if (str != null) {
            shadersLanguage = Arrays.asList(str);
        } else {
            shadersLanguage = new ArrayList<String>();
        }

        str = ic.readStringArray("shadersPath", null);
        if (str != null) {
            shadersPath = Arrays.asList(str);
        } else {
            shadersPath = new ArrayList<String>();
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
