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
 * represents a mapping between 2 ShaderNodeVariables
 *
 * @author Nehon
 */
public class VariableMapping implements Savable, Cloneable {

    private ShaderNodeVariable leftVariable;
    private ShaderNodeVariable rightVariable;
    private String condition;
    private String leftSwizzling = "";
    private String rightSwizzling = "";

    /**
     * creates a VariableMapping
     */
    public VariableMapping() {
    }

    /**
     * creates a VariableMapping
     *
     * @param leftVariable the left hand side variable of the expression
     * @param leftSwizzling the swizzling of the left variable
     * @param rightVariable the right hand side variable of the expression
     * @param rightSwizzling the swizzling of the right variable
     * @param condition the condition for this mapping
     */
    public VariableMapping(ShaderNodeVariable leftVariable, String leftSwizzling, ShaderNodeVariable rightVariable, String rightSwizzling, String condition) {
        this.leftVariable = leftVariable;
        this.rightVariable = rightVariable;
        this.condition = condition;
        this.leftSwizzling = leftSwizzling;
        this.rightSwizzling = rightSwizzling;
    }

    /**
     *
     * @return the left variable
     */
    public ShaderNodeVariable getLeftVariable() {
        return leftVariable;
    }

    /**
     * sets the left variable
     *
     * @param leftVariable the left variable
     */
    public void setLeftVariable(ShaderNodeVariable leftVariable) {
        this.leftVariable = leftVariable;
    }

    /**
     *
     * @return the right variable
     */
    public ShaderNodeVariable getRightVariable() {
        return rightVariable;
    }

    /**
     * sets the right variable
     *
     * @param rightVariable the right variable
     */
    public void setRightVariable(ShaderNodeVariable rightVariable) {
        this.rightVariable = rightVariable;
    }

    /**
     *
     * @return the condition
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

    /**
     *
     * @return the left swizzle
     */
    public String getLeftSwizzling() {
        return leftSwizzling;
    }

    /**
     * sets the left swizzle
     *
     * @param leftSwizzling the left swizzle
     */
    public void setLeftSwizzling(String leftSwizzling) {
        this.leftSwizzling = leftSwizzling;
    }

    /**
     *
     * @return the right swizzle
     */
    public String getRightSwizzling() {
        return rightSwizzling;
    }

    /**
     * sets the right swizzle
     *
     * @param rightSwizzling the right swizzle
     */
    public void setRightSwizzling(String rightSwizzling) {
        this.rightSwizzling = rightSwizzling;
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
        oc.write(leftVariable, "leftVariable", null);
        oc.write(rightVariable, "rightVariable", null);
        oc.write(condition, "condition", "");
        oc.write(leftSwizzling, "leftSwizzling", "");
        oc.write(rightSwizzling, "rightSwizzling", "");
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
        leftVariable = (ShaderNodeVariable) ic.readSavable("leftVariable", null);
        rightVariable = (ShaderNodeVariable) ic.readSavable("rightVariable", null);
        condition = ic.readString("condition", "");
        leftSwizzling = ic.readString("leftSwizzling", "");
        rightSwizzling = ic.readString("rightSwizzling", "");
    }

    @Override
    public String toString() {
        return "\n{" + leftVariable.toString() + (leftSwizzling.length() > 0 ? ("." + leftSwizzling) : "") + " = " + rightVariable.getType() + " " + rightVariable.getNameSpace() + "." + rightVariable.getName() + (rightSwizzling.length() > 0 ? ("." + rightSwizzling) : "") + " : " + condition + "}";
    }

    @Override
    protected VariableMapping clone() throws CloneNotSupportedException {
        VariableMapping clone = (VariableMapping) super.clone();

        clone.leftVariable = leftVariable.clone();
        clone.rightVariable = rightVariable.clone();

        return clone;
    }
}
