package com.jme3.shader.glsl.parser.ast.declaration;

import com.jme3.shader.glsl.parser.ast.AssignAstNode;
import com.jme3.shader.glsl.parser.ast.NameAstNode;
import com.jme3.shader.glsl.parser.ast.TypeAstNode;

/**
 * The node to present a variable declaration in the code.
 *
 * @author JavaSaBr
 */
public class VarDeclarationAstNode extends DeclarationAstNode {

    /**
     * The type.
     */
    private TypeAstNode type;

    /**
     * The name.
     */
    private NameAstNode name;

    /**
     * The assign.
     */
    private AssignAstNode assign;

    /**
     * Gets the type.
     *
     * @return the type.
     */
    public TypeAstNode getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the type.
     */
    public void setType(final TypeAstNode type) {
        this.type = type;
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

    /**
     * Gets the assign.
     *
     * @return the assign.
     */
    public AssignAstNode getAssign() {
        return assign;
    }

    /**
     * Sets the assign.
     *
     * @param assign the assign.
     */
    public void setAssign(final AssignAstNode assign) {
        this.assign = assign;
    }
}
