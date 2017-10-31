package com.jme3.shader.glsl.parser.ast.preprocessor;

import com.jme3.shader.glsl.parser.GlslLang;
import com.jme3.shader.glsl.parser.ast.NameAstNode;
import com.jme3.shader.glsl.parser.ast.value.ExtensionStatusValueAstNode;

/**
 * The node to present an extension preprocessor in the code.
 *
 * @author JavaSaBr
 */
public class ExtensionPreprocessorAstNode extends PreprocessorAstNode {

    /**
     * The extension.
     */
    private NameAstNode extension;

    /**
     * The extension status.
     */
    private ExtensionStatusValueAstNode status;

    /**
     * Gets the extension.
     *
     * @return the extension.
     */
    public NameAstNode getExtension() {
        return extension;
    }

    /**
     * Sets the extension.
     *
     * @param extension the extension.
     */
    public void setExtension(final NameAstNode extension) {
        this.extension = extension;
    }

    /**
     * Gets the extension status.
     *
     * @return the extension status.
     */
    public ExtensionStatusValueAstNode getStatus() {
        return status;
    }

    /**
     * Sets the extension status.
     *
     * @param status the extension status.
     */
    public void setStatus(final ExtensionStatusValueAstNode status) {
        this.status = status;
    }

    @Override
    protected String getStringAttributes() {
        return GlslLang.PR_TYPE_EXTENSION;
    }
}
