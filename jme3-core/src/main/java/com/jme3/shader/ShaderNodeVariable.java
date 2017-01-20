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
import java.io.IOException;

/**
 * A shader node variable
 *
 * @author Nehon
 */
public class ShaderNodeVariable implements Savable, Cloneable {

    private String name;
    private String type;
    private String nameSpace;
    private String condition;
    private boolean shaderOutput = false;
    private String multiplicity;

    /**
     * creates a ShaderNodeVariable
     *
     * @param type the glsl type of the variable
     * @param name the name of the variable
     */
    public ShaderNodeVariable(String type, String name) {
        this.name = name;
        this.type = type;
    }
    
    
    /**
     * creates a ShaderNodeVariable
     *
     * @param type the glsl type of the variable
     * @param nameSpace the nameSpace (can be the name of the shaderNode or
     * Global,Attr,MatParam,WorldParam)
     * @param name the name of the variable
     * @param multiplicity the number of element if this variable is an array. Can be an Int of a declared material parameter
     */
    public ShaderNodeVariable(String type, String nameSpace, String name, String multiplicity) {
        this.name = name;
        this.nameSpace = nameSpace;
        this.type = type;
        this.multiplicity = multiplicity;
    }

    /**
     * creates a ShaderNodeVariable
     *
     * @param type the glsl type of the variable
     * @param nameSpace the nameSpace (can be the name of the shaderNode or
     * Global,Attr,MatParam,WorldParam)
     * @param name the name of the variable
     */
    public ShaderNodeVariable(String type, String nameSpace, String name) {
        this.name = name;
        this.nameSpace = nameSpace;
        this.type = type;
    }

    /**
     * returns the name
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * sets the name
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return the glsl type
     */
    public String getType() {
        return type;
    }

    /**
     * sets the glsl type
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return the name space (can be the name of the shaderNode or
     * Global,Attr,MatParam,WorldParam)
     */
    public String getNameSpace() {
        return nameSpace;
    }

    /**
     * sets the nameSpace (can be the name of the shaderNode or
     * Global,Attr,MatParam,WorldParam)
     *
     * @param nameSpace
     */
    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (name != null?name.hashCode():0);
        hash = 29 * hash + (type != null?type.hashCode():0);
        hash = 29 * hash + (nameSpace != null?nameSpace.hashCode():0);
        hash = 29 * hash + (condition != null?condition.hashCode():0);
        hash = 29 * hash + (multiplicity != null?multiplicity.hashCode():0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ShaderNodeVariable other = (ShaderNodeVariable) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.type == null) ? (other.type != null) : !this.type.equals(other.type)) {
            return false;
        }
        if ((this.nameSpace == null) ? (other.nameSpace != null) : !this.nameSpace.equals(other.nameSpace)) {
            return false;
        }
        if ((this.condition == null) ? (other.condition != null) : !this.condition.equals(other.condition)) {
            return false;
        }
        if ((this.multiplicity == null) ? (other.multiplicity != null) : !this.multiplicity.equals(other.multiplicity)) {
            return false;
        }
        return true;
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
        oc.write(type, "type", "");
        oc.write(nameSpace, "nameSpace", "");
        oc.write(condition, "condition", null);
        oc.write(shaderOutput, "shaderOutput", false);
        oc.write(multiplicity, "multiplicity", null);

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
        type = ic.readString("type", "");
        nameSpace = ic.readString("nameSpace", "");
        condition = ic.readString("condition", null);        
        shaderOutput = ic.readBoolean("shaderOutput", false);
        multiplicity = ic.readString("multiplicity", null);
    }

    /**
     *
     * @return the condition for this variable to be declared
     */
    public String getCondition() {
        return condition;
    }

    /**
     * sets the condition
     *
     * @param condition the condition
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    @Override
    public String toString() {
        return "\n" + type + ' ' + (nameSpace != null ? (nameSpace + '.') : "") + name;
    }

    /**
     *
     * @return true if this variable is a shader output
     */
    public boolean isShaderOutput() {
        return shaderOutput;
    }

    /**
     * sets to true if this variable is a shader output
     *
     * @param shaderOutput true if this variable is a shader output
     */
    public void setShaderOutput(boolean shaderOutput) {
        this.shaderOutput = shaderOutput;
    }

    /**
     * 
     * @return the number of elements if this variable is an array
     */
    public String getMultiplicity() {
        return multiplicity;
    }

    /**
     * sets the number of elements of this variable making it an array
     * this value can be a number of can be a define
     * @param multiplicity 
     */
    public void setMultiplicity(String multiplicity) {
        this.multiplicity = multiplicity;
    }

    @Override
    public ShaderNodeVariable clone() throws CloneNotSupportedException {
        return (ShaderNodeVariable) super.clone();
    }
}
