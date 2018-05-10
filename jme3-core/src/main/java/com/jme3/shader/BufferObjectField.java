package com.jme3.shader;

import static java.util.Objects.requireNonNull;

/**
 * The class to describe a filed in BO.
 *
 * @author JavaSaBr
 */
public class BufferObjectField {


    /**
     * The field name.
     */
    private final String name;

    /**
     * The field type.
     */
    private final VarType type;

    /**
     * The field value.
     */
    private Object value;

    public BufferObjectField(final String name, final VarType type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Get the field name.
     *
     * @return the field name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the field type.
     *
     * @return the field type.
     */
    public VarType getType() {
        return type;
    }

    /**
     * Gets the field value.
     *
     * @return the field value.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the field value.
     *
     * @param value the field value.
     */
    public void setValue(final Object value) {
        this.value = requireNonNull(value, "The field's value can't be null.");
    }

    @Override
    public String toString() {
        return "BufferObjectField{" +
            "name='" + name + '\'' +
            ", type=" + type +
            ", value=" + value +
            '}';
    }
}
