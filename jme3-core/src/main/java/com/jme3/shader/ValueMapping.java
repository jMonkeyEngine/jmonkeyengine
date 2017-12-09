package com.jme3.shader;

import com.jme3.export.*;

import java.io.IOException;

/**
 * The mapping to define values of some input variables.
 *
 * @author JavaSaBr
 */
public class ValueMapping implements Savable, Cloneable {

    /**
     * The variable.
     */
    private ShaderNodeVariable variable;

    /**
     * The value.
     */
    private String value;

    public ValueMapping() {
    }

    public ValueMapping(final ShaderNodeVariable variable) {
        this.variable = variable;
    }

    public ValueMapping(final ShaderNodeVariable variable, final String value) {
        this.variable = variable;
        this.value = value;
    }

    /**
     * Gets the variable.
     *
     * @return the variable.
     */
    public ShaderNodeVariable getVariable() {
        return variable;
    }

    /**
     * Sets the variable.
     *
     * @param variable the variable.
     */
    public void setVariable(final ShaderNodeVariable variable) {
        this.variable = variable;
    }

    /**
     * Sets the value.
     *
     * @param value the value.
     */
    public void setValue(final String value) {
        this.value = value;
    }

    /**
     * Gets the value.
     *
     * @return the value.
     */
    public String getValue() {
        return value;
    }

    @Override
    public ValueMapping clone() throws CloneNotSupportedException {
        final ValueMapping clone = (ValueMapping) super.clone();
        clone.variable = variable == null ? null : variable.clone();
        return clone;
    }

    @Override
    public void write(final JmeExporter ex) throws IOException {
        final OutputCapsule oc = ex.getCapsule(this);
        oc.write(variable, "variable", null);
        oc.write(value, "value", null);
    }

    @Override
    public void read(final JmeImporter im) throws IOException {
        final InputCapsule ic = im.getCapsule(this);
        variable = (ShaderNodeVariable) ic.readSavable("", null);
        value = ic.readString("value", null);
    }

    @Override
    public String toString() {
        return variable + " = '" + value + '\'';
    }
}
