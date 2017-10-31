package com.jme3.shader.glsl.parser.ast.value;

/**
 * The node to present an extension status value in the code.
 *
 * @author JavaSaBr
 */
public class ExtensionStatusValueAstNode extends ValueAstNode {

    /**
     * The flag of enabling an extension.
     */
    private boolean enabled;

    /**
     * Sets the flag of enabling an extension.
     *
     * @param enabled true if an extension is enabled.
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the flag of enabling an extension.
     *
     * @return true if an extension is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    protected String getStringAttributes() {
        return "enabled = " + isEnabled();
    }
}
