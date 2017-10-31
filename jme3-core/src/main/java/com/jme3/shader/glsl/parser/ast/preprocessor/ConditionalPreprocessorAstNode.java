package com.jme3.shader.glsl.parser.ast.preprocessor;

import com.jme3.shader.glsl.parser.ast.BodyAstNode;
import com.jme3.shader.glsl.parser.ast.branching.condition.ConditionAstNode;

/**
 * The node to present conditional preprocessor.
 *
 * @author JavaSaBr
 */
public class ConditionalPreprocessorAstNode extends PreprocessorAstNode {

    /**
     * The condition.
     */
    private ConditionAstNode condition;

    /**
     * The body.
     */
    private BodyAstNode body;

    /**
     * The else body.
     */
    private BodyAstNode elseBody;

    /**
     * The else node.
     */
    private PreprocessorAstNode elseNode;

    /**
     * The end node.
     */
    private PreprocessorAstNode endNode;

    /**
     * Gets the condition.
     *
     * @return the condition.
     */
    public ConditionAstNode getCondition() {
        return condition;
    }

    /**
     * Sets the condition.
     *
     * @param condition the condition.
     */
    public void setCondition(final ConditionAstNode condition) {
        this.condition = condition;
    }

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
     * Gets the else body.
     *
     * @return the else body.
     */
    public BodyAstNode getElseBody() {
        return elseBody;
    }

    /**
     * Sets the else body.
     *
     * @param elseBody the else body.
     */
    public void setElseBody(final BodyAstNode elseBody) {
        this.elseBody = elseBody;
    }

    /**
     * Gets the else node.
     *
     * @return the else node.
     */
    public PreprocessorAstNode getElseNode() {
        return elseNode;
    }

    /**
     * Sets the else node.
     *
     * @param elseNode the else node.
     */
    public void setElseNode(final PreprocessorAstNode elseNode) {
        this.elseNode = elseNode;
    }

    /**
     * Gets the end node.
     *
     * @return the end node.
     */
    public PreprocessorAstNode getEndNode() {
        return endNode;
    }

    /**
     * Sets the end node.
     *
     * @param endNode the end node.
     */
    public void setEndNode(final PreprocessorAstNode endNode) {
        this.endNode = endNode;
    }
}
