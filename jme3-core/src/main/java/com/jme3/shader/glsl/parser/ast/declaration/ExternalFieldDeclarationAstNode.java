package com.jme3.shader.glsl.parser.ast.declaration;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

/**
 * The node to present an external field declaration in the code.
 *
 * @author JavaSaBr
 */
public class ExternalFieldDeclarationAstNode extends FieldDeclarationAstNode {

    public enum ExternalFieldType {
        UNIFORM("uniform"),
        ATTRIBUTE("attribute", "in"),
        VARYING("varying", "out");

        private static final ExternalFieldType[] VALUES = values();

        public static ExternalFieldType forKeyWord(final String keyword) {
            for (final ExternalFieldType fieldType : VALUES) {
                if (fieldType.keywords.contains(keyword)) {
                    return fieldType;
                }
            }

            return null;
        }

        private Set<String> keywords;

        ExternalFieldType(final String... keywords) {
            this.keywords = new HashSet<>(asList(keywords));
        }
    }

    /**
     * The field type.
     */
    private ExternalFieldType fieldType;

    /**
     * Gets the field type.
     *
     * @return the field type.
     */
    public ExternalFieldType getFieldType() {
        return fieldType;
    }

    /**
     * Sets the field type.
     *
     * @param fieldType the field type.
     */
    public void setFieldType(final ExternalFieldType fieldType) {
        this.fieldType = fieldType;
    }

    @Override
    protected String getStringAttributes() {
        return getFieldType().name();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ExternalFieldDeclarationAstNode that = (ExternalFieldDeclarationAstNode) o;
        if (getFieldType() != that.getFieldType()) return false;
        if (!getType().equals(that.getType())) return false;
        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        int result = getFieldType().hashCode();
        result = 31 * result + getType().hashCode();
        result = 31 * result + getName().hashCode();
        return result;
    }
}
