package com.jme3.shader.glsl.parser.ast.util;

import com.jme3.shader.UniformBinding;
import com.jme3.shader.glsl.parser.GlslLang;
import com.jme3.shader.glsl.parser.GlslParser;
import com.jme3.shader.glsl.parser.Token;
import com.jme3.shader.glsl.parser.ast.AstNode;
import com.jme3.shader.glsl.parser.ast.NameAstNode;
import com.jme3.shader.glsl.parser.ast.declaration.ExternalFieldDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.declaration.ExternalFieldDeclarationAstNode.ExternalFieldType;
import com.jme3.shader.glsl.parser.ast.declaration.MethodDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.preprocessor.ExtensionPreprocessorAstNode;
import com.jme3.shader.glsl.parser.ast.preprocessor.ImportPreprocessorAstNode;
import com.jme3.shader.glsl.parser.ast.value.DefineValueAstNode;
import com.jme3.shader.glsl.parser.ast.value.StringValueAstNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * The utility class to work with AST nodes.
 *
 * @author JavaSaBr
 */
public class AstUtils {

    public static final BiPredicate<GlslParser, char[]> EMPTY = new BiPredicate<GlslParser, char[]>() {
        @Override
        public boolean test(final GlslParser parser, final char[] content) {
            return false;
        }
    };

    public static final BiPredicate<GlslParser, char[]> END_IF = new BiPredicate<GlslParser, char[]>() {
        @Override
        public boolean test(final GlslParser parser, final char[] content) {

            final Token token = parser.readToken(content);

            if (token.getType() != GlslParser.TOKEN_PREPROCESSOR) {
                return false;
            }

            final Token keyWordToken = parser.findToken(content, GlslParser.TOKEN_KEYWORD);
            final String text = keyWordToken.getText();
            return text.equals(GlslLang.PR_TYPE_ENDIF);
        }
    };

    public static final BiPredicate<GlslParser, char[]> END_IF_OR_ELSE_OR_ELSE_IF = new BiPredicate<GlslParser, char[]>() {
        @Override
        public boolean test(final GlslParser parser, final char[] content) {

            final Token token = parser.readToken(content);

            if (token.getType() != GlslParser.TOKEN_PREPROCESSOR) {
                return false;
            }

            final Token keyWordToken = parser.findToken(content, GlslParser.TOKEN_KEYWORD);
            final String text = keyWordToken.getText();
            return text.equals(GlslLang.PR_TYPE_ENDIF) || text.equals(GlslLang.PR_TYPE_ELSE) ||
                    text.equals(GlslLang.PR_TYPE_ELIF);
        }
    };

    public static final BiPredicate<GlslParser, char[]> RIGHT_BRACE = new BiPredicate<GlslParser, char[]>() {
        @Override
        public boolean test(final GlslParser parser, final char[] content) {
            final Token token = parser.readToken(content);
            return token.getType() == GlslParser.TOKEN_RIGHT_BRACE;
        }
    };

    public static final BiPredicate<GlslParser, char[]> IF_WITHOUT_BRACES = new BiPredicate<GlslParser, char[]>() {
        @Override
        public boolean test(final GlslParser parser, final char[] content) {

            Token token = parser.getPreviousReadToken();
            if (token.getType() == GlslParser.TOKEN_SEMICOLON) {
                return true;
            }

            token = parser.readToken(content);
            return token.getType() == GlslParser.TOKEN_SEMICOLON;
        }
    };

    public static final BiPredicate<GlslParser, char[]> ELSE_WITHOUT_BRACES = IF_WITHOUT_BRACES;

    /**
     * Find all existing nodes.
     *
     * @param node the node.
     * @param type the type.
     * @param <T>  the type.
     * @return the list of all found nodes.
     */
    public static <T extends AstNode> List<T> findAllByType(final AstNode node, final Class<T> type) {
        return findAllByType(node, new ArrayList<T>(4), type);
    }

    /**
     * Find all existing nodes.
     *
     * @param node   the node.
     * @param result the result.
     * @param type   the type.
     * @param <T>    the type.
     * @return the list of all found nodes.
     */
    public static <T extends AstNode> List<T> findAllByType(final AstNode node, final List<T> result,
                                                            final Class<T> type) {

        node.visit(new Predicate<AstNode>() {

            @Override
            public boolean test(final AstNode node) {
                if (type.isInstance(node)) {
                    result.add(type.cast(node));
                }
                return true;
            }
        });

        return result;
    }

    /**
     * Get parse level of the node.
     *
     * @param node the node.
     * @return the parse level.
     */
    public static int getParseLevel(final AstNode node) {

        if (hasParentByType(node, MethodDeclarationAstNode.class)) {
            return GlslParser.LEVEL_METHOD;
        }

        return GlslParser.LEVEL_FILE;
    }

    /**
     * Checks of existing a parent of the type.
     *
     * @param node the node.
     * @param type the type of a parent.
     * @return true if a parent of the type is exists.
     */
    public static boolean hasParentByType(final AstNode node, final Class<? extends AstNode> type) {

        AstNode parent = node.getParent();
        while (parent != null) {
            if (type.isInstance(parent)) {
                return true;
            }
            parent = parent.getParent();
        }

        return false;
    }

    /**
     * Updates offset, length and text of the node.
     *
     * @param node    the node.
     * @param content the content.
     */
    public static void updateOffsetAndLengthAndText(final AstNode node, final char[] content) {

        final List<AstNode> children = node.getChildren();
        if (children.isEmpty()) {
            node.setOffset(0);
            updateLengthAndText(node, content);
            return;
        }

        final AstNode first = children.get(0);
        node.setOffset(first.getOffset());

        updateLengthAndText(node, content);
    }

    /**
     * Updates length and text of the node.
     *
     * @param node    the node.
     * @param content the content.
     */
    public static void updateLengthAndText(final AstNode node, final char[] content) {

        final List<AstNode> children = node.getChildren();
        if (children.isEmpty()) {
            node.setLength(0);
            node.setText("");
            return;
        }

        final AstNode last = children.get(children.size() - 1);
        node.setLength(last.getOffset() + last.getLength() - node.getOffset());

        updateText(node, content);
    }

    /**
     * Updates text of the node.
     *
     * @param node    the node.
     * @param content the content.
     */
    public static void updateText(final AstNode node, final char[] content) {
        node.setText(String.valueOf(content, node.getOffset(), node.getLength()));
    }

    /**
     * Gets indent of the node.
     *
     * @param node the node.
     * @return the indent.
     */
    public static String getIndent(final AstNode node) {

        int count = 0;

        AstNode parent = node.getParent();
        while (parent != null) {
            count++;
            parent = parent.getParent();
        }

        return getIndent(count);
    }

    /**
     * Gets indent of the level.
     *
     * @param level the level.
     * @return the indent.
     */
    public static String getIndent(final int level) {

        final char[] result = new char[level * 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = ' ';
        }

        return String.valueOf(result);
    }

    /**
     * Removed duplicates of the extensionNodes.
     *
     * @param extensionNodes the extensionNodes.
     */
    public static void removeExtensionDuplicates(final List<ExtensionPreprocessorAstNode> extensionNodes) {

        if (extensionNodes.size() < 2) {
            return;
        }

        for (Iterator<ExtensionPreprocessorAstNode> iterator = extensionNodes.iterator(); iterator.hasNext(); ) {

            final ExtensionPreprocessorAstNode extensionNode = iterator.next();
            final NameAstNode name = extensionNode.getExtension();

            boolean isDuplicate = false;
            for (final ExtensionPreprocessorAstNode other : extensionNodes) {
                if (other != extensionNode && Objects.equals(name, other.getExtension())) {
                    isDuplicate = true;
                    break;
                }
            }

            if (isDuplicate) {
                iterator.remove();
            }
        }
    }

    /**
     * Removed duplicates of the imports.
     *
     * @param importNodes the imports.
     */
    public static void removeImportDuplicates(final List<ImportPreprocessorAstNode> importNodes) {

        if (importNodes.size() < 2) {
            return;
        }

        for (Iterator<ImportPreprocessorAstNode> iterator = importNodes.iterator(); iterator.hasNext(); ) {

            final ImportPreprocessorAstNode importNode = iterator.next();
            final StringValueAstNode value = importNode.getValue();

            boolean isDuplicate = false;
            for (final ImportPreprocessorAstNode other : importNodes) {
                if (other != importNode && Objects.equals(value, other.getValue())) {
                    isDuplicate = true;
                    break;
                }
            }

            if (isDuplicate) {
                iterator.remove();
            }
        }
    }

    /**
     * Removed duplicates of the define values.
     *
     * @param defineValues the define values.
     */
    public static void removeDefineValueDuplicates(final List<DefineValueAstNode> defineValues) {

        if (defineValues.size() < 2) {
            return;
        }

        for (Iterator<DefineValueAstNode> iterator = defineValues.iterator(); iterator.hasNext(); ) {

            final DefineValueAstNode defineValue = iterator.next();
            final String value = defineValue.getValue();

            boolean isDuplicate = false;
            for (final DefineValueAstNode other : defineValues) {
                if (other != defineValue && Objects.equals(value, other.getValue())) {
                    isDuplicate = true;
                    break;
                }
            }

            if (isDuplicate) {
                iterator.remove();
            }
        }
    }

    /**
     * Copies only global uniforms from the first list to the second.
     *
     * @param fields the list of external field.
     * @param result the result list.
     */
    public static void copyGlobalUniforms(final List<ExternalFieldDeclarationAstNode> fields,
                                          final List<ExternalFieldDeclarationAstNode> result) {
        if (fields.isEmpty()) {
            return;
        }

        for (final ExternalFieldDeclarationAstNode field : fields) {

            final ExternalFieldType type = field.getFieldType();
            if (type != ExternalFieldType.UNIFORM) {
                continue;
            }

            final NameAstNode nameNode = field.getName();
            final String name = nameNode.getName();

            if (!name.startsWith("g_")) {
                continue;
            }

            if (!result.contains(field)) {
                result.add(field);
            }
        }
    }

    /**
     * Removes exists global uniforms from the fields list.
     *
     * @param fields   the fields list.
     * @param bindings the list of exists global uniforms.
     */
    public static void removeExists(final List<ExternalFieldDeclarationAstNode> fields,
                                    final List<UniformBinding> bindings) {

        if (fields.isEmpty() || bindings.isEmpty()) {
            return;
        }

        for (Iterator<ExternalFieldDeclarationAstNode> iterator = fields.iterator(); iterator.hasNext(); ) {

            final ExternalFieldDeclarationAstNode field = iterator.next();

            final NameAstNode nameNode = field.getName();
            final String name = nameNode.getName();

            final UniformBinding binding = UniformBinding.valueOf(name.substring(2, name.length()));

            if (bindings.contains(binding)) {
                iterator.remove();
            }
        }
    }
}