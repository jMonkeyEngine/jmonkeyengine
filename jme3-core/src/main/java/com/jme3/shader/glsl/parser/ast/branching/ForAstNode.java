package com.jme3.shader.glsl.parser.ast.branching;

import com.jme3.shader.glsl.parser.ast.AstNode;
import com.jme3.shader.glsl.parser.ast.BodyAstNode;

/**
 * The node to present an 'for' a statement in the code.
 *
 * @author JavaSaBr
 */
public class ForAstNode extends AstNode {

    /**
     * The body.
     */
    private BodyAstNode body;

    /**
     * Gets the body.
     *
     * @return the body.
     */
    public BodyAstNode getBody() {
        return body;
    }

    /**
     * Sets the body.
     *
     * @param body the body.
     */
    public void setBody(final BodyAstNode body) {
        this.body = body;
    }
}
