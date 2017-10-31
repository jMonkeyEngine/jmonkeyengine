package com.jme3.shader.glsl.parser.ast;

/**
 * The node to present a symbol.
 *
 * @author JavaSaBr
 */
public class SymbolAstNode extends AstNode {

    @Override
    protected String getStringAttributes() {
        return getText();
    }
}
