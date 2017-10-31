package com.jme3.shader.glsl.parser.ast.value;

import com.jme3.shader.glsl.parser.ast.AstNode;

/**
 * The node to present a value of something.
 *
 * @author JavaSaBr
 */
public class ValueAstNode extends AstNode {

    /**
     * The string value.
     */
    private String value;

    /**
     * Gets the value.
     *
     * @return the value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value the value.
     */
    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    protected String getStringAttributes() {
        return getValue();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ValueAstNode that = (ValueAstNode) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
