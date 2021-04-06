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
import java.util.Objects;

/**
 * Represents a mapping between 2 shader node variables or a left shader node variable and a value expression.
 *
 * @author Nehon
 */
public class VariableMapping implements Savable, Cloneable {

    private ShaderNodeVariable leftVariable;
    private ShaderNodeVariable rightVariable;
    private String rightExpression;
    private String condition;
    private String leftSwizzling = "";
    private String rightSwizzling = "";

    /**
     * Creates a VariableMapping.
     */
    public VariableMapping() {
    }

    /**
     * Creates a VariableMapping.
     *
     * @param leftVariable   the left hand side variable of the expression
     * @param leftSwizzling  the swizzling of the left variable
     * @param rightVariable  the right hand side variable of the expression
     * @param rightSwizzling the swizzling of the right variable
     * @param condition      the condition for this mapping
     */
    public VariableMapping(ShaderNodeVariable leftVariable, String leftSwizzling, ShaderNodeVariable rightVariable,
                           String rightSwizzling, String condition) {
        this.leftVariable = leftVariable;
        this.rightVariable = rightVariable;
        this.condition = condition;
        this.leftSwizzling = Objects.requireNonNull(leftSwizzling);
        this.rightSwizzling = Objects.requireNonNull(rightSwizzling);
    }

    /**
     * Gets the left variable.
     *
     * @return the left variable.
     */
    public ShaderNodeVariable getLeftVariable() {
        return leftVariable;
    }

    /**
     * Sets the left variable.
     *
     * @param leftVariable the left variable.
     */
    public void setLeftVariable(ShaderNodeVariable leftVariable) {
        this.leftVariable = leftVariable;
    }

    /**
     * Gets the right variable.
     *
     * @return the right variable or null.
     */
    public ShaderNodeVariable getRightVariable() {
        return rightVariable;
    }

    /**
     * Sets the right variable.
     *
     * @param rightVariable the right variable.
     */
    public void setRightVariable(ShaderNodeVariable rightVariable) {
        this.rightVariable = rightVariable;
    }

    /**
     * Gets the right expression.
     *
     * @return the right expression or null.
     */
    public String getRightExpression() {
        return rightExpression;
    }

    /**
     * Sets the right expression.
     *
     * @param rightExpression the right expression.
     */
    public void setRightExpression(final String rightExpression) {
        this.rightExpression = rightExpression;
    }

    /**
     * Gets the condition.
     *
     * @return the condition
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Sets the condition.
     *
     * @param condition the condition or null.
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * Gets the left swizzle.
     *
     * @return the left swizzle or empty string.
     */
    public String getLeftSwizzling() {
        return leftSwizzling;
    }

    /**
     * Sets the left swizzle.
     *
     * @param leftSwizzling the left swizzle.
     */
    public void setLeftSwizzling(String leftSwizzling) {
        this.leftSwizzling = Objects.requireNonNull(leftSwizzling);
    }

    /**
     * Gets the right swizzle.
     *
     * @return the right swizzle or empty string.
     */
    public String getRightSwizzling() {
        return rightSwizzling;
    }

    /**
     * Sets the right swizzle.
     *
     * @param rightSwizzling the right swizzle.
     */
    public void setRightSwizzling(String rightSwizzling) {
        this.rightSwizzling = Objects.requireNonNull(rightSwizzling);
    }

    /**
     * jme serialization (not used)
     *
     * @param ex the exporter
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(leftVariable, "leftVariable", null);
        oc.write(rightVariable, "rightVariable", null);
        oc.write(rightExpression, "rightExpression", null);
        oc.write(condition, "condition", "");
        oc.write(leftSwizzling, "leftSwizzling", "");
        oc.write(rightSwizzling, "rightSwizzling", "");
    }

    /**
     * jme serialization (not used)
     *
     * @param im the importer
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        leftVariable = (ShaderNodeVariable) ic.readSavable("leftVariable", null);
        rightVariable = (ShaderNodeVariable) ic.readSavable("rightVariable", null);
        rightExpression = ic.readString("rightExpression", null);
        condition = ic.readString("condition", "");
        leftSwizzling = ic.readString("leftSwizzling", "");
        rightSwizzling = ic.readString("rightSwizzling", "");
    }

    @Override
    public String toString() {

        final StringBuilder builder = new StringBuilder(leftVariable.toString());

        if (!leftSwizzling.isEmpty()) {
            builder.append('.').append(leftSwizzling);
        }

        builder.append(" = ");

        if (rightVariable != null) {

            builder.append(rightVariable.getType())
                    .append(' ')
                    .append(rightVariable.getNameSpace())
                    .append('.')
                    .append(rightVariable.getName());

            if (!rightSwizzling.isEmpty()) {
                builder.append('.').append(rightSwizzling);
            }

        } else if (rightExpression != null) {
            builder.append(rightExpression);
        }

        if (condition != null && !condition.isEmpty()) {
            builder.append(" : ").append(condition);
        }

        return builder.toString();
    }

    @Override
    protected VariableMapping clone() throws CloneNotSupportedException {
        VariableMapping clone = (VariableMapping) super.clone();
        clone.leftVariable = leftVariable.clone();
        if (rightVariable != null) {
            clone.rightVariable = rightVariable.clone();
        }
        return clone;
    }
}
