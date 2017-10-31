package com.jme3.shader.glsl.parser.ast.preprocessor;

import com.jme3.shader.glsl.parser.GlslLang;
import com.jme3.shader.glsl.parser.ast.value.StringValueAstNode;

/**
 * The node to present an import preprocessor in the code.
 *
 * @author JavaSaBr
 */
public class ImportPreprocessorAstNode extends PreprocessorAstNode {

    /**
     * The value.
     */
    private StringValueAstNode value;

    /**
     * Gets the value if this import.
     *
     * @return the value if this import.
     */
    public StringValueAstNode getValue() {
        return value;
    }

    /**
     * Sets the value if this import.
     *
     * @param value the value if this import.
     */
    public void setValue(final StringValueAstNode value) {
        this.value = value;
    }

    @Override
    protected String getStringAttributes() {
        return GlslLang.PR_TYPE_IMPORT;
    }
}
