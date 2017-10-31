package com.jme3.shader.glsl.parser.ast.declaration;

import com.jme3.shader.glsl.parser.ast.BodyAstNode;
import com.jme3.shader.glsl.parser.ast.NameAstNode;
import com.jme3.shader.glsl.parser.ast.TypeAstNode;

/**
 * The node to present a method declaration in the code.
 *
 * @author JavaSaBr
 */
public class MethodDeclarationAstNode extends DeclarationAstNode {

    /**
     * The name.
     */
    private NameAstNode name;

    /**
     * The body.
     */
    private BodyAstNode body;

    /**
     * The return type.
     */
    private TypeAstNode returnType;

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

    /**
     * Gets the return type.
     *
     * @return the return type.
     */
    public TypeAstNode getReturnType() {
        return returnType;
    }

    /**
     * Sets the return type.
     *
     * @param returnType the return type.
     */
    public void setReturnType(final TypeAstNode returnType) {
        this.returnType = returnType;
    }

    /**
     * Gets the name.
     *
     * @return the name.
     */
    public NameAstNode getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name.
     */
    public void setName(final NameAstNode name) {
        this.name = name;
    }
}
