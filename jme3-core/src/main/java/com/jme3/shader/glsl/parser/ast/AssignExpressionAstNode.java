package com.jme3.shader.glsl.parser.ast;

/**
 * The node to present an assign expression in the code.
 *
 * @author JavaSaBr
 */
public class AssignExpressionAstNode extends AstNode {

    /**
     * The first part.
     */
    private AstNode firstPart;

    /**
     * The second part.
     */
    private AstNode secondPart;

    /**
     * Gets the first part.
     *
     * @return the first part.
     */
    public AstNode getFirstPart() {
        return firstPart;
    }

    /**
     * Sets the first part.
     *
     * @param firstPart the first part.
     */
    public void setFirstPart(final AstNode firstPart) {
        this.firstPart = firstPart;
    }

    /**
     * Gets the second part.
     *
     * @return the second part.
     */
    public AstNode getSecondPart() {
        return secondPart;
    }

    /**
     * Sets the second part.
     *
     * @param secondPart the second part.
     */
    public void setSecondPart(final AstNode secondPart) {
        this.secondPart = secondPart;
    }
}
