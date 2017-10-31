package com.jme3.shader.glsl.parser.ast.declaration;

/**
 * The AST node to present a file.
 *
 * @author JavaSaBr
 */
public class FileDeclarationAstNode extends DeclarationAstNode {

    /**
     * The path to the file.
     */
    private String path;

    /**
     * Gets the path to the file.
     *
     * @return the path to the file.
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path to the file.
     *
     * @param path the path to the file.
     */
    public void setPath(final String path) {
        this.path = path;
    }

    @Override
    protected String getStringAttributes() {
        return getPath();
    }
}
