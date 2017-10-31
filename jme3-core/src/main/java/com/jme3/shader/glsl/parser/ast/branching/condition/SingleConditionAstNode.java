package com.jme3.shader.glsl.parser.ast.branching.condition;

import com.jme3.shader.glsl.parser.ast.AstNode;

/**
 * The node to present a single condition.
 *
 * @author JavaSaBr
 */
public class SingleConditionAstNode extends ConditionAstNode {

    /**
     * The expression.
     */
    private AstNode expression;

    /**
     * Gets the expression.
     *
     * @return the expression.
     */
    public AstNode getExpression() {
        return expression;
    }

    /**
     * Sets the expression.
     *
     * @param expression the expression.
     */
    public void setExpression(final AstNode expression) {
        this.expression = expression;
    }
}
