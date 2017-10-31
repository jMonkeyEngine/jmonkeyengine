package com.jme3.shader.glsl.parser.ast;

/**
 * The node to present a name.
 *
 * @author JavaSaBr
 */
public class NameAstNode extends AstNode {

    /**
     * The name.
     */
    private String name;

    /**
     * Gets the name.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name.
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
        final NameAstNode that = (NameAstNode) o;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
