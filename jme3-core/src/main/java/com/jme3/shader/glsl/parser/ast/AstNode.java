package com.jme3.shader.glsl.parser.ast;

import com.jme3.shader.glsl.parser.ast.util.AstUtils;
import com.jme3.shader.glsl.parser.ast.util.Predicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The base AST node.
 *
 * @author JavaSaBr
 */
public class AstNode {

    private static final List<AstNode> EMPTY_LIST = Collections.emptyList();

    /**
     * THe parent AST node.
     */
    private AstNode parent;

    /**
     * The children AST nodes.
     */
    private List<AstNode> children;

    /**
     * The text.
     */
    private String text;

    /**
     * The line.
     */
    private int line;

    /**
     * The offset.
     */
    private int offset;

    /**
     * The length.
     */
    private int length;

    public AstNode() {
        this.children = EMPTY_LIST;
    }

    /**
     * Gets the parent node.
     *
     * @return the parent node.
     */
    public AstNode getParent() {
        return parent;
    }

    /**
     * Sets the parent node.
     *
     * @param parent the parent node.
     */
    public void setParent(final AstNode parent) {
        this.parent = parent;
    }

    /**
     * Gets the children nodes.
     *
     * @return the children nodes.
     */
    public List<AstNode> getChildren() {
        return children;
    }

    /**
     * Adds the new child to this node.
     *
     * @param child the new child.
     */
    public void addChild(final AstNode child) {

        if (children == EMPTY_LIST) {
            children = new ArrayList<>(5);
        }

        children.add(child);
    }

    /**
     * Removes the old child from this nod.e
     *
     * @param child the old child.
     */
    public void removeChild(final AstNode child) {
        children.remove(child);
    }

    /**
     * Sets the length.
     *
     * @param length the length.
     */
    public void setLength(final int length) {
        this.length = length;
    }

    /**
     * Gets the length.
     *
     * @return the length.
     */
    public int getLength() {
        return length;
    }

    /**
     * Sets the line.
     *
     * @param line the line.
     */
    public void setLine(final int line) {
        this.line = line;
    }

    /**
     * Gets the line.
     *
     * @return the line.
     */
    public int getLine() {
        return line;
    }

    /**
     * Sets the offset.
     *
     * @param offset the offset.
     */
    public void setOffset(final int offset) {
        this.offset = offset;
    }

    /**
     * Gets the offset.
     *
     * @return the offset.
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Sets the text.
     *
     * @param text the text.
     */
    public void setText(final String text) {
        this.text = text;
    }

    /**
     * Gets the text.
     *
     * @return the text.
     */
    public String getText() {
        return text;
    }

    /**
     * Visit all AST nodes.
     *
     * @param visitor the visitor.
     */
    public void visit(final Predicate<AstNode> visitor) {

        final List<AstNode> children = getChildren();
        if (children.isEmpty()) {
            return;
        }

        for (int i = 0; i < children.size(); i++) {
            final AstNode child = children.get(i);
            if (visitor.test(child)) {
                child.visit(visitor);
            }
        }
    }

    /**
     * Try to find the last node of the type.
     *
     * @param type the type.
     * @param <T>  the node type.
     * @return the last node or null.
     */
    public <T extends AstNode> T getLastNode(final Class<T> type) {

        final List<AstNode> children = getChildren();
        for (int i = children.size() - 1; i >= 0; i--) {

            final AstNode child = children.get(i);
            final T lastNode = child.getLastNode(type);

            if (lastNode != null) {
                return lastNode;
            } else if (type.isInstance(child)) {
                return type.cast(child);
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return toString(this, 0);
    }

    /**
     * Build a string presentation of the node for the level.
     *
     * @param node  the node.
     * @param level the level.
     * @return the string presentation.
     */
    protected String toString(final AstNode node, final int level) {

        final String indent = AstUtils.getIndent(level);
        final Class<? extends AstNode> type = node.getClass();
        final String typeName = type.getSimpleName();

        String result = indent + "-" + typeName + ": " + node.getStringAttributes();

        final List<AstNode> children = node.getChildren();
        if (children.isEmpty()) {
            return result;
        }

        for (final AstNode child : children) {
            final String childString = toString(child, level + 1);
            result += ("\n" + childString);
        }

        return result;
    }

    /**
     * Gets the string attributes of this node.
     *
     * @return the string attributes.
     */
    protected String getStringAttributes() {
        return "";
    }
}
