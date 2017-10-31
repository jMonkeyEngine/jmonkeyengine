package com.jme3.shader.glsl.parser.ast;

/**
 * The node to present assign value.
 *
 * @author JavaSaBr
 */
public class AssignAstNode extends AstNode {

    /**
     * The assigned value.
     */
    private AstNode value;

    /**
     * Gets the assigned value.
     *
     * @return the assigned value.
     */
    public AstNode getValue() {
        return value;
    }

    /**
     * Sets the assigned value.
     *
     * @param value the assigned value.
     */
    public void setValue(final AstNode value) {
        this.value = value;
    }
}
