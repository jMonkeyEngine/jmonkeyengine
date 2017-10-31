package com.jme3.shader.glsl.parser.ast.branching.condition;

import com.jme3.shader.glsl.parser.GlslLang;

/**
 * The node to present define condition Is.
 *
 * @author JavaSaBr
 */
public class DefineConditionIsAstNode extends ConditionIsAstNode {

    @Override
    protected String getStringAttributes() {
        return GlslLang.PR_TYPE_DEFINE;
    }
}
