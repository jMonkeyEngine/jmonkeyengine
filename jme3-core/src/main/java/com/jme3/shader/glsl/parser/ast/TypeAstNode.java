package com.jme3.shader.glsl.parser.ast;

/**
 * The node to present a type.
 *
 * @author JavaSaBr
 */
public class TypeAstNode extends AstNode {

    /**
     * THe type name.
     */
    private String name;

    /**
     * Gets the type name.
     *
     * @return the type name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the type name.
     *
     * @param name the type name.
     */
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    protected String getStringAttributes() {
        return getName();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TypeAstNode that = (TypeAstNode) o;
        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
