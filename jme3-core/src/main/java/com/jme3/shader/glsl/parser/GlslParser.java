package com.jme3.shader.glsl.parser;

import static com.jme3.shader.glsl.parser.GlslLang.KEYWORDS;
import static com.jme3.shader.glsl.parser.GlslLang.PREPROCESSOR_WITH_CONDITION;
import static java.util.Objects.requireNonNull;
import com.jme3.shader.glsl.parser.ast.*;
import com.jme3.shader.glsl.parser.ast.branching.ForAstNode;
import com.jme3.shader.glsl.parser.ast.branching.IfAstNode;
import com.jme3.shader.glsl.parser.ast.branching.condition.*;
import com.jme3.shader.glsl.parser.ast.declaration.ExternalFieldDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.declaration.ExternalFieldDeclarationAstNode.ExternalFieldType;
import com.jme3.shader.glsl.parser.ast.declaration.FileDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.declaration.LocalVarDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.declaration.MethodDeclarationAstNode;
import com.jme3.shader.glsl.parser.ast.preprocessor.*;
import com.jme3.shader.glsl.parser.ast.util.AstUtils;
import com.jme3.shader.glsl.parser.ast.util.BiPredicate;
import com.jme3.shader.glsl.parser.ast.value.DefineValueAstNode;
import com.jme3.shader.glsl.parser.ast.value.ExtensionStatusValueAstNode;
import com.jme3.shader.glsl.parser.ast.value.StringValueAstNode;
import com.jme3.shader.glsl.parser.ast.value.ValueAstNode;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * The parser of GLSL code.
 *
 * @author JavaSaBr
 */
public class GlslParser {

    public static final int TOKEN_PREPROCESSOR = 2;
    public static final int TOKEN_STRING = 3;
    public static final int TOKEN_KEYWORD = 4;
    public static final int TOKEN_WORD = 5;
    public static final int TOKEN_SEMICOLON = 6;
    public static final int TOKEN_LEFT_PARENTHESIS = 7;
    public static final int TOKEN_RIGHT_PARENTHESIS = 8;
    public static final int TOKEN_EXCLAMATION_MARK = 9;
    public static final int TOKEN_OR = 10;
    public static final int TOKEN_AND = 11;
    public static final int TOKEN_LEFT_BRACE = 12;
    public static final int TOKEN_RIGHT_BRACE = 13;
    public static final int TOKEN_ASSIGN = 14;
    public static final int TOKEN_COLON = 15;
    public static final int TOKEN_SINGLE_COMMENT = 16;
    public static final int TOKEN_COMPARE = 17;
    public static final int TOKEN_DOT = 18;
    public static final int TOKEN_LEFT_BRACKET = 19;
    public static final int TOKEN_RIGHT_BRACKET = 20;

    public static final int LEVEL_FILE = 1;
    public static final int LEVEL_STRUCT = 2;
    public static final int LEVEL_METHOD = 3;

    private static final Set<Character> SPLIT_CHARS = new HashSet<>();
    private static final Set<Character> SINGLE_COMMENT_SPLIT_CHARS = new HashSet<>();

    static {
        SPLIT_CHARS.add(' ');
        SPLIT_CHARS.add('\n');
        SPLIT_CHARS.add('\t');
        SPLIT_CHARS.add('\r');
        SINGLE_COMMENT_SPLIT_CHARS.add('\n');
        SINGLE_COMMENT_SPLIT_CHARS.add('\r');
    }

    /**
     * Creates a new instance of this parser.
     *
     * @return the new instance of this parser.
     */
    public static GlslParser newInstance() {
        return new GlslParser();
    }

    /**
     * The stack of ast nodes.
     */
    private final Deque<AstNode> nodeStack;

    /**
     * The list of read tokens.
     */
    private final Deque<Token> readTokens;

    /**
     * The set of split chars.
     */
    private Set<Character> splitChars;

    /**
     * The current token.
     */
    private Token currentToken;

    /**
     * The current line.
     */
    private int line;

    /**
     * The current offset.
     */
    private int offset;

    /**
     * The saved line.
     */
    private int savedLine;

    /**
     * The saved offset.
     */
    private int savedOffset;

    private GlslParser() {
        this.nodeStack = new ArrayDeque<>();
        this.splitChars = SPLIT_CHARS;
        this.readTokens = new ArrayDeque<>();
    }

    /**
     * Parse the GLSL file.
     *
     * @param path     the path to GLSL file.
     * @param glslCode the GLSL code.
     * @return the file ast node.
     */
    public FileDeclarationAstNode parseFileDeclaration(final String path, final String glslCode) {

        final FileDeclarationAstNode node = new FileDeclarationAstNode();
        node.setPath(path);
        node.setLine(line);
        node.setOffset(offset);
        node.setLength(glslCode.length());
        node.setText(glslCode);

        if (glslCode.length() < 6 && glslCode.trim().isEmpty()) {
            return node;
        }

        nodeStack.addLast(node);
        try {
            parseContent(glslCode.toCharArray(), LEVEL_FILE, AstUtils.EMPTY);
        } catch (final RuntimeException e) {
            System.out.println(glslCode);
            throw e;
        } finally {
            nodeStack.removeLast();
        }

        return node;
    }

    /**
     * Parse content.
     *
     * @param content       the content.
     * @param level         the level.
     * @param exitCondition the exit condition.
     */
    private void parseContent(final char[] content, final int level, final BiPredicate<GlslParser, char[]> exitCondition) {

        int prevOffset;
        int prevLine;

        Token token;
        do {

            prevOffset = offset;
            prevLine = line;

            try {

                if (exitCondition.test(this, content)) {
                    return;
                }

            } finally {
                offset = prevOffset;
                line = prevLine;
            }

            token = readToken(content);

            if (token.getType() == TOKEN_PREPROCESSOR) {
                parsePreprocessor(token, findToken(content, TOKEN_KEYWORD), content);
            } else if (token.getType() == TOKEN_KEYWORD) {

                if (GlslLang.KW_IF.equals(token.getText())) {
                    parseIf(token, content);
                } else if (GlslLang.KW_FOR.equals(token.getText())) {
                    parseFor(token, content);
                }  else if (GlslLang.KW_DISCARD.equals(token.getText())) {
                    parseDiscard(token);
                } else if (level == LEVEL_FILE) {
                    parseExternalFieldDeclaration(token, content);
                }

            } else if (token.getType() == TOKEN_WORD) {

                if (level == LEVEL_FILE) {
                    parseMethodDeclaration(token, content);
                } else if (level == LEVEL_METHOD) {

                    Token secondToken;

                    saveState();
                    try {
                        secondToken = findToken(content, TOKEN_ASSIGN, TOKEN_DOT, TOKEN_LEFT_PARENTHESIS, TOKEN_WORD);
                    } finally {
                        restoreState();
                    }

                    if (secondToken.getType() == TOKEN_WORD) {
                        parseLocalVarDeclaration(token, content);
                    } else if (secondToken.getType() == TOKEN_LEFT_PARENTHESIS) {
                        parseMethodCall(token, content);
                    } else if (secondToken.getType() == TOKEN_ASSIGN || secondToken.getType() == TOKEN_DOT) {
                        parseAssignExpression(token, content);
                    }
                }
            }

        } while (token != Token.EOF_TOKEN);
    }

    /**
     * Parse a preprocessor AST node.
     *
     * @param token        the preprocessor token.
     * @param keywordToken the keyword token.
     * @param content      the content.
     * @return the preprocessor AST node.
     */
    private PreprocessorAstNode parsePreprocessor(final Token token, final Token keywordToken, final char[] content) {

        final String keyword = keywordToken.getText();

        if (GlslLang.PR_TYPE_IMPORT.equals(keyword)) {
            return parseImportPreprocessor(content, token);
        } else if (GlslLang.PR_TYPE_EXTENSION.equals(keyword)) {
            return parseExtensionPreprocessor(content, token);
        } else if (GlslLang.PR_TYPE_DEFINE.equals(keyword)) {
            return parseDefinePreprocessor(content, token);
        } else if (GlslLang.PR_TYPE_ERROR.equals(keyword)) {
            return parseErrorPreprocessor(content, token);
        }

        if (!PREPROCESSOR_WITH_CONDITION.contains(keyword)) {
            return parsePreprocessorNode(token, keywordToken, content);
        }

        return parseConditionalPreprocessor(token, content, keyword);
    }

    /**
     * Parse a preprocessor AST node.
     *
     * @param token        the preprocessor token.
     * @param keyWordToken the keyword token of the preprocessor.
     * @param content      the content.
     * @return the preprocessor AST node.
     */
    private PreprocessorAstNode parsePreprocessorNode(final Token token, final Token keyWordToken,
                                                      final char[] content) {

        final AstNode parent = nodeStack.getLast();
        final PreprocessorAstNode node = new PreprocessorAstNode();
        node.setParent(parent);
        node.setType(keyWordToken.getText());
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());
        node.setLength(keyWordToken.getOffset() + keyWordToken.getLength() - node.getOffset());
        node.setText(String.valueOf(content, node.getOffset(), node.getLength()));

        parent.addChild(node);

        return node;
    }

    /**
     * Parse a conditional preprocessor AST node.
     *
     * @param token   the conditional preprocessor token.
     * @param content the content.
     * @param type    the type of the preprocessor.
     * @return the conditional preprocessor AST node.
     */
    private ConditionalPreprocessorAstNode parseConditionalPreprocessor(final Token token, final char[] content,
                                                                        final String type) {

        final AstNode parent = nodeStack.getLast();
        final ConditionalPreprocessorAstNode node = new ConditionalPreprocessorAstNode();
        node.setParent(parent);
        node.setType(type);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());

        nodeStack.addLast(node);
        try {

            node.setCondition(parseCondition(type, content));
            node.setBody(parseBody(content, AstUtils.END_IF_OR_ELSE_OR_ELSE_IF));

            final Token nextDefine = findToken(content, TOKEN_PREPROCESSOR);
            final Token nextKeyWord = findToken(content, TOKEN_KEYWORD);

            if (GlslLang.PR_TYPE_ENDIF.equals(nextKeyWord.getText())) {
                node.setEndNode(parsePreprocessor(nextDefine, nextKeyWord, content));
            } else if (GlslLang.PR_TYPE_ELSE.equals(nextKeyWord.getText())) {
                node.setElseNode(parsePreprocessor(nextDefine, nextKeyWord, content));
                node.setElseBody(parseBody(content, AstUtils.END_IF));
                node.setEndNode(parsePreprocessor(findToken(content, TOKEN_PREPROCESSOR), findToken(content, TOKEN_KEYWORD), content));
            } else if (GlslLang.PR_TYPE_ELIF.equals(nextKeyWord.getText())) {

                final BodyAstNode body = new BodyAstNode();
                body.setParent(node);

                nodeStack.addLast(body);
                try {
                    parsePreprocessor(nextDefine, nextKeyWord, content);
                } finally {
                    nodeStack.removeLast();
                }

                AstUtils.updateOffsetAndLengthAndText(body, content);

                node.addChild(body);
                node.setElseBody(body);

                final ConditionalPreprocessorAstNode lastNode = node.getLastNode(ConditionalPreprocessorAstNode.class);
                final PreprocessorAstNode endNode = lastNode.getEndNode();
                final String endNodeText = endNode.getText();

                final Token endToken = new Token(TOKEN_PREPROCESSOR, endNode.getOffset(), endNode.getLine(), "#");
                final Token endKeyWordToken = new Token(TOKEN_KEYWORD, endNode.getOffset(), endNode.getLine(), endNodeText);

                node.setEndNode(parsePreprocessor(endToken, endKeyWordToken, content));
            }

        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse an 'if' AST node.
     *
     * @param token   the 'if' token.
     * @param content the content.
     * @return the 'if' AST node.
     */
    private IfAstNode parseIf(final Token token, final char[] content) {

        final AstNode parent = nodeStack.getLast();
        final IfAstNode node = new IfAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());

        nodeStack.addLast(node);
        try {

            node.setCondition(parseCondition(null, content));

            Token nextToken;

            saveState();
            try {
                nextToken = findToken(content, TOKEN_LEFT_BRACE, TOKEN_WORD, TOKEN_KEYWORD);
            } finally {
                restoreState();
            }

            if (nextToken.getType() == TOKEN_LEFT_BRACE) {

                node.setBody(parseBody(content, AstUtils.RIGHT_BRACE));
                nextToken = findToken(content, TOKEN_RIGHT_BRACE, TOKEN_KEYWORD);

                // if this IF is closed
                if (nextToken.getType() == TOKEN_RIGHT_BRACE) {
                    parseSymbol(nextToken);

                    // try to find next potential tokens
                    saveState();
                    try {
                        nextToken = findToken(content, TOKEN_RIGHT_BRACE, TOKEN_WORD, TOKEN_KEYWORD);
                    } finally {
                        restoreState();
                    }

                    // if we have the next token "else", we need to re-take it without saving state
                    if (nextToken.getType() == TOKEN_KEYWORD && GlslLang.KW_ELSE.equals(nextToken.getText())) {
                        nextToken = findToken(content, TOKEN_WORD, TOKEN_KEYWORD);
                    }
                }

                if (nextToken.getType() == TOKEN_KEYWORD && GlslLang.KW_ELSE.equals(nextToken.getText())) {
                    node.setElseNode(parseValue(nextToken));

                    saveState();
                    try {
                        nextToken = findToken(content, TOKEN_LEFT_BRACE, TOKEN_WORD, TOKEN_KEYWORD);
                    } finally {
                        restoreState();
                    }

                    if (nextToken.getType() == TOKEN_KEYWORD) {

                        final BodyAstNode bodyNode = new BodyAstNode();
                        bodyNode.setParent(node);

                        nodeStack.addLast(bodyNode);
                        try {
                            parseIf(findToken(content, TOKEN_KEYWORD), content);
                        } finally {
                            nodeStack.removeLast();
                        }

                        AstUtils.updateOffsetAndLengthAndText(bodyNode, content);

                        node.addChild(bodyNode);
                        node.setBody(bodyNode);

                    } else if (nextToken.getType() == TOKEN_LEFT_BRACE) {
                        node.setElseBody(parseBody(content, AstUtils.RIGHT_BRACE));
                        parseSymbol(findToken(content, TOKEN_RIGHT_BRACE));
                    } else if (nextToken.getType() == TOKEN_LEFT_BRACE) {
                        node.setElseBody(parseBody(content, AstUtils.ELSE_WITHOUT_BRACES));
                    }
                }

            } else {
                node.setBody(parseBody(content, AstUtils.IF_WITHOUT_BRACES));
            }

        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse an 'for' AST node.
     *
     * @param token   the 'for' token.
     * @param content the content.
     * @return the 'for' AST node.
     */
    private ForAstNode parseFor(final Token token, final char[] content) {

        final Token leftToken = findToken(content, TOKEN_LEFT_PARENTHESIS);
        final Token rightToken = findToken(content, TOKEN_RIGHT_PARENTHESIS);

        final AstNode parent = nodeStack.getLast();
        final ForAstNode node = new ForAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());

        nodeStack.addLast(node);
        try {

            final Token startBodyToken = findToken(content, TOKEN_LEFT_BRACE);

            parseSymbol(leftToken);
            parseSymbol(rightToken);
            parseSymbol(startBodyToken);

            node.setBody(parseBody(content, AstUtils.RIGHT_BRACE));

            final Token endBodyToken = findToken(content, TOKEN_RIGHT_BRACE);

            parseSymbol(endBodyToken);

        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse a body AST node.
     *
     * @param content       the content.
     * @param exitCondition the exit condition.
     * @return the body AST node.
     */
    private BodyAstNode parseBody(final char[] content, final BiPredicate<GlslParser, char[]> exitCondition) {

        final AstNode parent = nodeStack.getLast();
        final BodyAstNode node = new BodyAstNode();
        node.setParent(parent);

        nodeStack.addLast(node);
        try {
            parseContent(content, AstUtils.getParseLevel(node), exitCondition);
        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateOffsetAndLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse conditions.
     *
     * @param preprocessorType the type of a preprocessor or null.
     * @param content          the content.
     * @return the condition AST node or null.
     */
    private ConditionAstNode parseCondition(final String preprocessorType, final char[] content) {

        if (GlslLang.PR_TYPE_IFDEF.equals(preprocessorType) || GlslLang.PR_TYPE_IFNDEF.equals(preprocessorType)) {
            return parseSimpleDefPreprocessorCondition(content);
        }

        final Token token = findToken(content, TOKEN_KEYWORD, TOKEN_WORD, TOKEN_LEFT_PARENTHESIS, TOKEN_EXCLAMATION_MARK);

        switch (token.getType()) {
            case TOKEN_WORD: {

                final Token compare = findToken(content, TOKEN_COMPARE);
                final Token second = findToken(content, TOKEN_WORD);

                final AstNode parent = nodeStack.getLast();
                final ConditionIsAstNode node = new ConditionIsAstNode();
                node.setParent(parent);
                node.setLine(token.getLine());
                node.setOffset(token.getOffset());

                nodeStack.addLast(node);
                try {
                    parseValue(token);
                    parseSymbol(compare);
                    parseValue(second);
                } finally {
                    nodeStack.removeLast();
                }

                parent.addChild(node);

                AstUtils.updateLengthAndText(node, content);

                if (isLastConditionPart(content)) {
                    return node;
                }

                return appendCondition(content, node);
            }
            case TOKEN_EXCLAMATION_MARK: {
                return parseNegativeCondition(content, token);
            }
            case TOKEN_KEYWORD: {

                final ConditionAstNode node = parseDefinePreprocessorCondition(content, token);

                if (isLastConditionPart(content)) {
                    return node;
                }

                return appendCondition(content, node);
            }
            case TOKEN_LEFT_PARENTHESIS: {

                final AstNode parent = nodeStack.getLast();
                final ConditionIsAstNode node = new ConditionIsAstNode();
                node.setParent(parent);
                node.setLine(token.getLine());
                node.setOffset(token.getOffset());

                nodeStack.addLast(node);
                try {
                    parseSymbol(token);
                    node.setExpression(parseCondition(null, content));
                    parseSymbol(findToken(content, TOKEN_RIGHT_PARENTHESIS));
                } finally {
                    nodeStack.removeLast();
                }

                AstUtils.updateLengthAndText(node, content);

                parent.addChild(node);

                if (isLastConditionPart(content)) {
                    return node;
                }

                return appendCondition(content, node);
            }
        }

        return null;
    }

    /**
     * Append an additional condition.
     *
     * @param content        the content.
     * @param firstCondition the first condition.
     * @return the result condition.
     */
    private ConditionAstNode appendCondition(final char[] content, final ConditionAstNode firstCondition) {

        final Token logicToken = findToken(content, TOKEN_OR, TOKEN_AND);

        final AstNode parent = firstCondition.getParent();
        parent.removeChild(firstCondition);

        final MultiConditionAstNode conditionsNode =
                logicToken.getType() == TOKEN_AND ? new ConditionAndAstNode() : new ConditionOrAstNode();
        conditionsNode.setParent(parent);
        conditionsNode.addChild(firstCondition);
        conditionsNode.addExpression(firstCondition);
        conditionsNode.setLine(firstCondition.getLine());
        conditionsNode.setOffset(firstCondition.getOffset());

        firstCondition.setParent(conditionsNode);

        nodeStack.addLast(conditionsNode);
        try {

            conditionsNode.addExpression(parseSymbol(logicToken));

            final ConditionAstNode secondCondition = requireNonNull(parseCondition(null, content));
            conditionsNode.addExpression(secondCondition);

        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(conditionsNode, content);

        parent.addChild(conditionsNode);

        return conditionsNode;
    }

    /**
     * Checks of existing a next part of the current condition.
     *
     * @param content the content.
     * @return false if the next part is exists.
     */
    private boolean isLastConditionPart(final char[] content) {

        saveState();
        try {

            final Token check = findToken(content, TOKEN_KEYWORD, TOKEN_WORD, TOKEN_OR, TOKEN_AND, TOKEN_RIGHT_PARENTHESIS);

            if (check.getType() == TOKEN_WORD || check.getType() == TOKEN_KEYWORD ||
                    check.getType() == TOKEN_RIGHT_PARENTHESIS) {
                throw new IllegalArgumentException();
            }

        } catch (final IllegalArgumentException e) {
            return true;
        } finally {
            restoreState();
        }

        return false;
    }

    /**
     * Parse a define preprocessor condition AST node.
     *
     * @param content the content.
     * @param token   the token.
     * @return the condition AST node.
     */
    private ConditionAstNode parseDefinePreprocessorCondition(final char[] content, final Token token) {

        final Token nextToken = findToken(content, TOKEN_LEFT_PARENTHESIS, TOKEN_WORD);
        final Token defineValueToken = nextToken.getType() == TOKEN_LEFT_PARENTHESIS ? findToken(content, TOKEN_WORD) : null;
        final Token rightToken = nextToken.getType() == TOKEN_LEFT_PARENTHESIS ? findToken(content, TOKEN_RIGHT_PARENTHESIS) : null;

        final AstNode parent = nodeStack.getLast();
        final DefineConditionIsAstNode node = new DefineConditionIsAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());

        nodeStack.addLast(node);
        try {
            if (defineValueToken != null) {
                parseSymbol(nextToken);
                node.setExpression(parseDefineValue(defineValueToken));
                parseSymbol(rightToken);
            } else {
                node.setExpression(parseDefineValue(nextToken));
            }
        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse a condition is not AST node.
     *
     * @param content the content.
     * @param token   the condition token.
     * @return the result condition node.
     */
    private ConditionAstNode parseNegativeCondition(final char[] content, final Token token) {

        final AstNode parent = nodeStack.getLast();
        final ConditionIsNotAstNode node = new ConditionIsNotAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());

        nodeStack.addLast(node);
        try {
            parseSymbol(token);
            node.setExpression(parseCondition(null, content));
        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse the condition of ifdef/idndef constructions.
     *
     * @param content the content.
     * @return the condition.
     */
    private ConditionAstNode parseSimpleDefPreprocessorCondition(final char[] content) {

        final Token token = findToken(content, TOKEN_WORD);
        final AstNode parent = nodeStack.getLast();
        final ConditionIsAstNode node = new ConditionIsAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());

        nodeStack.addLast(node);
        try {
            node.setExpression(parseDefineValue(token));
        } finally {
            nodeStack.removeLast();
        }

        parent.addChild(node);

        AstUtils.updateLengthAndText(node, content);

        return node;
    }

    /**
     * Parse an import preprocessor AST node.
     *
     * @param content the content.
     * @param token   the token.
     * @return the import preprocessor AST node.
     */
    private ImportPreprocessorAstNode parseImportPreprocessor(final char[] content, final Token token) {

        final Token valueToken = findToken(content, TOKEN_STRING);

        final AstNode parent = nodeStack.getLast();
        final ImportPreprocessorAstNode node = new ImportPreprocessorAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());

        nodeStack.addLast(node);
        try {
            node.setValue(parseStringValue(valueToken));
        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse an extension preprocessor AST node.
     *
     * @param content the content.
     * @param token   the token.
     * @return the extension preprocessor AST node.
     */
    private ExtensionPreprocessorAstNode parseExtensionPreprocessor(final char[] content, final Token token) {

        final Token extensionToken = findToken(content, TOKEN_WORD);
        final Token splitToken = findToken(content, TOKEN_COLON);
        final Token statusToken = findToken(content, TOKEN_WORD);

        final AstNode parent = nodeStack.getLast();
        final ExtensionPreprocessorAstNode node = new ExtensionPreprocessorAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());

        nodeStack.addLast(node);
        try {
            node.setExtension(parseName(extensionToken));
            parseSymbol(splitToken);
            node.setStatus(parseExtensionStatusValue(statusToken));
        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse a define preprocessor AST node.
     *
     * @param content the content.
     * @param token   the token.
     * @return the define preprocessor AST node.
     */
    private DefinePreprocessorAstNode parseDefinePreprocessor(final char[] content, final Token token) {

        final Token nameToken = findToken(content, TOKEN_WORD);
        final Token valueToken = findToken(content, TOKEN_WORD);

        Token secondValue;

        saveState();
        try {
            secondValue = findToken(content, TOKEN_WORD);
        } catch (IllegalStateException e) {
            secondValue = null;
        } finally {
            restoreState();
        }

        if (secondValue != null && secondValue.getLine() == valueToken.getLine()) {
            secondValue = findToken(content, TOKEN_WORD);
        } else {
            secondValue = null;
        }

        final AstNode parent = nodeStack.getLast();
        final DefinePreprocessorAstNode node = new DefinePreprocessorAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());

        nodeStack.addLast(node);
        try {

            node.setName(parseName(nameToken));
            node.setValue(parseValue(valueToken));

            //FIXME
            if (secondValue != null) {
                parseValue(secondValue);
            }

        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse an error preprocessor AST node.
     *
     * @param content the content.
     * @param token   the token.
     * @return the error preprocessor AST node.
     */
    private PreprocessorAstNode parseErrorPreprocessor(final char[] content, final Token token) {

        final AstNode parent = nodeStack.getLast();
        final PreprocessorAstNode node = new PreprocessorAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());

        nodeStack.addLast(node);
        try {

            while (true) {
                saveState();
                try {

                    Token nextToken = findToken(content, TOKEN_WORD);

                    if (nextToken.getLine() != token.getLine()) {
                        restoreState();
                        break;
                    }

                    parseValue(nextToken);

                } catch (final IllegalArgumentException e) {
                    restoreState();
                }
            }

        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse an external field declaration AST node.
     *
     * @param fieldTypeToken the field type token.
     * @param content        the content.
     * @return the external field declaration AST node.
     */
    private ExternalFieldDeclarationAstNode parseExternalFieldDeclaration(final Token fieldTypeToken,
                                                                          final char[] content) {

        final Token typeToken = findToken(content, TOKEN_WORD);
        final Token nameToken = findToken(content, TOKEN_WORD);
        final Token semicolonToken = findToken(content, TOKEN_SEMICOLON);

        final AstNode parent = nodeStack.getLast();
        final ExternalFieldDeclarationAstNode node = new ExternalFieldDeclarationAstNode();
        node.setParent(parent);
        node.setLine(fieldTypeToken.getLine());
        node.setOffset(fieldTypeToken.getOffset());
        node.setFieldType(ExternalFieldType.forKeyWord(fieldTypeToken.getText()));

        nodeStack.addLast(node);
        try {
            node.setType(parseType(typeToken));
            node.setName(parseName(nameToken));
            parseSymbol(semicolonToken);
        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse a method declaration AST node.
     *
     * @param returnTypeToken the type
     * @param content         the content.
     */
    private void parseMethodDeclaration(final Token returnTypeToken, final char[] content) {

        final Token nameToken = findToken(content, TOKEN_WORD);
        final Token startArgumentsToken = findToken(content, TOKEN_LEFT_PARENTHESIS);
        final Token finishArgumentsToken = findToken(content, TOKEN_RIGHT_PARENTHESIS);
        final Token startBodyToken = findToken(content, TOKEN_LEFT_BRACE);

        final AstNode parent = nodeStack.getLast();
        final MethodDeclarationAstNode node = new MethodDeclarationAstNode();
        node.setParent(parent);
        node.setLine(returnTypeToken.getLine());
        node.setOffset(returnTypeToken.getOffset());

        nodeStack.addLast(node);
        try {

            node.setReturnType(parseType(returnTypeToken));
            node.setName(parseName(nameToken));

            parseSymbol(startArgumentsToken);
            parseSymbol(finishArgumentsToken);
            parseSymbol(startBodyToken);

            node.setBody(parseBody(content, AstUtils.RIGHT_BRACE));

            final Token endBodyToken = findToken(content, TOKEN_RIGHT_BRACE);

            parseSymbol(endBodyToken);

        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);
    }

    /**
     * Parse a local variable declaration AST node.
     *
     * @param typeToken the var type token.
     * @param content   the content.
     * @return the local variable declaration AST node.
     */
    private LocalVarDeclarationAstNode parseLocalVarDeclaration(final Token typeToken, final char[] content) {

        final Token nameToken = findToken(content, TOKEN_WORD);
        final Token semicolonOrAssignToken = findToken(content, TOKEN_SEMICOLON, TOKEN_ASSIGN);

        final AstNode parent = nodeStack.getLast();
        final LocalVarDeclarationAstNode node = new LocalVarDeclarationAstNode();
        node.setParent(parent);
        node.setLine(typeToken.getLine());
        node.setOffset(typeToken.getOffset());

        nodeStack.addLast(node);
        try {

            node.setType(parseType(typeToken));
            node.setName(parseName(nameToken));

            if (semicolonOrAssignToken.getType() == TOKEN_SEMICOLON) {
                parseSymbol(semicolonOrAssignToken);
            } else {
                node.setAssign(null);
                parseSymbol(findToken(content, TOKEN_SEMICOLON));
            }

        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse an assign expression AST node.
     *
     * @param firstToken the first token.
     * @param content    the content.
     * @return the assign expression AST node.
     */
    private AssignExpressionAstNode parseAssignExpression(final Token firstToken, final char[] content) {

        final Token assignToken = findToken(content, TOKEN_ASSIGN);
        final Token endToken = findToken(content, TOKEN_SEMICOLON);

        final AstNode parent = nodeStack.getLast();
        final AssignExpressionAstNode node = new AssignExpressionAstNode();
        node.setParent(parent);
        node.setLine(firstToken.getLine());
        node.setOffset(firstToken.getOffset());

        nodeStack.addLast(node);
        try {
            parseName(firstToken);
            parseSymbol(assignToken);
            parseSymbol(endToken);
        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse a method call expression AST node.
     *
     * @param firstToken the first token.
     * @param content    the content.
     * @return the method call expression AST node.
     */
    private MethodCallAstNode parseMethodCall(final Token firstToken, final char[] content) {

        final Token endToken = findToken(content, TOKEN_SEMICOLON);

        final AstNode parent = nodeStack.getLast();
        final MethodCallAstNode node = new MethodCallAstNode();
        node.setParent(parent);
        node.setLine(firstToken.getLine());
        node.setOffset(firstToken.getOffset());

        nodeStack.addLast(node);
        try {
            parseName(firstToken);
            parseSymbol(endToken);
        } finally {
            nodeStack.removeLast();
        }

        AstUtils.updateLengthAndText(node, content);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse a define value AST node.
     *
     * @param token the define value token.
     */
    private DefineValueAstNode parseDefineValue(final Token token) {

        final String text = token.getText();
        final AstNode parent = nodeStack.getLast();
        final DefineValueAstNode node = new DefineValueAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());
        node.setLength(token.getLength());
        node.setText(text);
        node.setValue(text);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse a type AST node.
     *
     * @param typeToken the type token.
     */
    private TypeAstNode parseType(final Token typeToken) {

        final String text = typeToken.getText();
        final AstNode parent = nodeStack.getLast();
        final TypeAstNode node = new TypeAstNode();
        node.setParent(parent);
        node.setLine(typeToken.getLine());
        node.setOffset(typeToken.getOffset());
        node.setLength(typeToken.getLength());
        node.setText(text);
        node.setName(text);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse a name AST node.
     *
     * @param nameToken the name token.
     */
    private NameAstNode parseName(final Token nameToken) {

        final String text = nameToken.getText();
        final AstNode parent = nodeStack.getLast();
        final NameAstNode node = new NameAstNode();
        node.setParent(parent);
        node.setLine(nameToken.getLine());
        node.setOffset(nameToken.getOffset());
        node.setLength(nameToken.getLength());
        node.setText(text);
        node.setName(text);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse a value AST node.
     *
     * @param token the token.
     * @return the value AST node.
     */
    private ValueAstNode parseValue(final Token token) {

        final String text = token.getText();
        final AstNode parent = nodeStack.getLast();
        final ValueAstNode node = new ValueAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());
        node.setLength(token.getLength());
        node.setText(text);
        node.setValue(text);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse a discard AST node.
     *
     * @param token the token.
     * @return the discard AST node.
     */
    private DiscardAstNode parseDiscard(final Token token) {

        final String text = token.getText();
        final AstNode parent = nodeStack.getLast();
        final DiscardAstNode node = new DiscardAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());
        node.setLength(token.getLength());
        node.setText(text);

        parent.addChild(node);

        return node;
    }

    /**
     * Parse a string value AST node.
     *
     * @param token the token.
     * @return the string value AST node.
     */
    private StringValueAstNode parseStringValue(final Token token) {

        final String text = token.getText();
        final AstNode parent = nodeStack.getLast();
        final StringValueAstNode node = new StringValueAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());
        node.setLength(token.getLength());
        node.setText(text);

        if (text.equals("\"\"")) {
            node.setValue("");
        } else {
            node.setValue(text.substring(1, text.length() - 1));
        }

        parent.addChild(node);

        return node;
    }

    /**
     * Parse an extension status value AST node.
     *
     * @param token the token.
     * @return the extension status value AST node.
     */
    private ExtensionStatusValueAstNode parseExtensionStatusValue(final Token token) {

        final String text = token.getText();
        final AstNode parent = nodeStack.getLast();
        final ExtensionStatusValueAstNode node = new ExtensionStatusValueAstNode();
        node.setParent(parent);
        node.setLine(token.getLine());
        node.setOffset(token.getOffset());
        node.setLength(token.getLength());
        node.setValue(text);
        node.setText(text);
        node.setEnabled("enable".equals(text));

        parent.addChild(node);

        return node;
    }

    /**
     * Parse a symbol AST node.
     *
     * @param symbolToken the symbol token.
     * @return the symbol AST node.
     */
    private SymbolAstNode parseSymbol(final Token symbolToken) {

        final String text = symbolToken.getText();
        final AstNode parent = nodeStack.getLast();
        final SymbolAstNode node = new SymbolAstNode();
        node.setParent(parent);
        node.setLine(symbolToken.getLine());
        node.setOffset(symbolToken.getOffset());
        node.setLength(symbolToken.getLength());
        node.setText(text);

        parent.addChild(node);
        readTokens.add(symbolToken);

        return node;
    }

    /**
     * Gets the current token.
     *
     * @return the current token.
     */
    private Token getCurrentToken() {
        return currentToken;
    }

    /**
     * Sets the current token.
     *
     * @param currentToken the current token.
     */
    private void setCurrentToken(final Token currentToken) {
        this.currentToken = currentToken;
    }

    /**
     * Finds a token by the types.
     *
     * @param content    the content.
     * @param firstType  the first type.
     * @param secondType the second type.
     * @return the found token.
     * @throws RuntimeException when we didn't find a token with the types.
     */
    private Token findToken(final char[] content, final int firstType, final int secondType) {

        Token token;
        do {
            token = readToken(content);
        } while (token.getType() != firstType && token.getType() != secondType && token.getType() != Token.EOF);

        if (token.getType() == Token.EOF) {
            throw new RuntimeException("unexpected EOF token");
        }

        return token;
    }

    /**
     * Finds a token by the type.
     *
     * @param content the content.
     * @param type    the type.
     * @return the found token.
     * @throws RuntimeException when we didn't find a token with the type.
     */
    public Token findToken(final char[] content, final int type) {

        Token token;
        do {
            token = readToken(content);
        } while (token.getType() != type && token.getType() != Token.EOF);

        if (token.getType() == Token.EOF) {
            throw new RuntimeException("unexpected EOF token");
        }

        return token;
    }

    /**
     * Gets the previous read token.
     *
     * @return the previous read token.
     */
    public Token getPreviousReadToken() {
        return readTokens.getLast();
    }

    /**
     * Finds a token by the types.
     *
     * @param content    the content.
     * @param firstType  the first type.
     * @param secondType the second type.
     * @param thirdType  the third type.
     * @return the found token.
     * @throws RuntimeException when we didn't find a token with the types.
     */
    private Token findToken(final char[] content, final int firstType, final int secondType, final int thirdType) {

        Token token;
        do {
            token = readToken(content);
        } while (token.getType() != firstType && token.getType() != secondType && token.getType() != thirdType &&
                token.getType() != Token.EOF);

        if (token.getType() == Token.EOF) {
            throw new RuntimeException("unexpected EOF token");
        }

        return token;
    }

    /**
     * Finds a token by the types.
     *
     * @param content    the content.
     * @param firstType  the first type.
     * @param secondType the second type.
     * @param thirdType  the third type.
     * @param fourthType the fourth type.
     * @return the found token.
     * @throws RuntimeException when we didn't find a token with the types.
     */
    private Token findToken(final char[] content, final int firstType, final int secondType, final int thirdType,
                            final int fourthType) {

        Token token;
        do {
            token = readToken(content);
        } while (token.getType() != firstType && token.getType() != secondType && token.getType() != thirdType &&
                token.getType() != fourthType && token.getType() != Token.EOF);

        if (token.getType() == Token.EOF) {
            throw new IllegalArgumentException("unexpected EOF token");
        }

        return token;
    }

    /**
     * Finds a token by the types.
     *
     * @param content    the content.
     * @param firstType  the first type.
     * @param secondType the second type.
     * @param thirdType  the third type.
     * @param fourthType the fourth type.
     * @param fifthType  the fifth type.
     * @return the found token.
     * @throws RuntimeException when we didn't find a token with the types.
     */
    private Token findToken(final char[] content, final int firstType, final int secondType, final int thirdType,
                            final int fourthType, final int fifthType) {

        Token token;
        do {
            token = readToken(content);
        } while (token.getType() != firstType && token.getType() != secondType && token.getType() != thirdType &&
                token.getType() != fourthType && token.getType() != fifthType && token.getType() != Token.EOF);

        if (token.getType() == Token.EOF) {
            throw new IllegalArgumentException("unexpected EOF token");
        }

        return token;
    }

    /**
     * Reads a next token.
     *
     * @param content the content.
     * @return the next token.
     */
    public Token readToken(final char[] content) {

        String text = null;

        while (true) {

            final char ch = content[offset >= content.length ? content.length - 1 : offset];
            offset++;

            if (splitChars.contains(ch) || offset > content.length) {
                splitChars = SPLIT_CHARS;

                final Token token = getCurrentToken();

                if (token != null) {
                    offset--;
                    setCurrentToken(null);
                    token.setText(getText(text));
                    token.setLength(offset - token.getOffset());
                    return token;
                }

                final Token newToken = tryToGetToken(text);
                if (newToken != null) return newToken;

                if (offset >= content.length) {
                    return Token.EOF_TOKEN;
                }

                if (ch == '\n') {
                    line++;
                }

                return Token.SKIP_TOKEN;
            }

            if (currentToken == null) {
                switch (ch) {
                    case ';': {

                        final Token token = tryToGetToken(text);
                        if (token != null) return token;

                        return handleCharToken(text, ch, TOKEN_SEMICOLON);
                    }
                    case '[': {

                        final Token token = tryToGetToken(text);
                        if (token != null) return token;

                        return handleCharToken(text, ch, TOKEN_LEFT_BRACKET);
                    }
                    case ']': {

                        final Token token = tryToGetToken(text);
                        if (token != null) return token;

                        return handleCharToken(text, ch, TOKEN_RIGHT_BRACKET);
                    }
                    case '(': {

                        final Token token = tryToGetToken(text);
                        if (token != null) return token;

                        return handleCharToken(text, ch, TOKEN_LEFT_PARENTHESIS);
                    }
                    case '.': {

                        if (text != null && isNumber(text)) {
                            break;
                        }

                        final Token token = tryToGetToken(text);
                        if (token != null) return token;

                        return handleCharToken(text, ch, TOKEN_DOT);
                    }
                    case ')': {
                        return handleCharToken(text, ch, TOKEN_RIGHT_PARENTHESIS);
                    }
                    case '{': {
                        return handleCharToken(text, ch, TOKEN_LEFT_BRACE);
                    }
                    case '}': {
                        return handleCharToken(text, ch, TOKEN_RIGHT_BRACE);
                    }
                    case '!': {
                        return handleCharToken(text, ch, TOKEN_EXCLAMATION_MARK);
                    }
                    case '=': {

                        final Token token = tryToGetToken(text);
                        if (token != null) return token;

                        if (text == null && content[offset] != '=') {
                            return handleCharToken(text, ch, TOKEN_ASSIGN);
                        }
                        break;
                    }
                    case ':': {
                        return handleCharToken(text, ch, TOKEN_COLON);
                    }
                    case '#': {
                        return handleCharToken(text, ch, TOKEN_PREPROCESSOR);
                    }
                    case '<':
                    case '>': {
                        if (content[offset] != '=') {
                            return handleCharToken(text, ch, TOKEN_COMPARE);
                        }
                    }
                }
            }

            if (text == null) {
                text = Character.toString(ch);
            } else {
                text += ch;
            }

            switch (ch) {
                case '"': {

                    final Token currentToken = getCurrentToken();
                    if (currentToken != null) {
                        currentToken.setText(getText(text));
                        currentToken.setLength(offset - currentToken.getOffset());
                        setCurrentToken(null);
                        return currentToken;
                    }

                    setCurrentToken(new Token(TOKEN_STRING, line, offset - 1));
                    continue;
                }
            }

            if (currentToken != null) {
                continue;
            }

            if ("||".equals(text)) {
                return new Token(TOKEN_OR, offset - text.length(), line, text);
            } else if ("&&".equals(text)) {
                return new Token(TOKEN_AND, offset - text.length(), line, text);
            } else if (">=".equals(text)) {
                return new Token(TOKEN_COMPARE, offset - text.length(), line, text);
            } else if ("<=".equals(text)) {
                return new Token(TOKEN_COMPARE, offset - text.length(), line, text);
            } else if ("==".equals(text)) {
                return new Token(TOKEN_COMPARE, offset - text.length(), line, text);
            } else if ("-=".equals(text)) {
                return new Token(TOKEN_ASSIGN, offset - text.length(), line, text);
            } else if ("+=".equals(text)) {
                return new Token(TOKEN_ASSIGN, offset - text.length(), line, text);
            } else if ("*=".equals(text)) {
                return new Token(TOKEN_ASSIGN, offset - text.length(), line, text);
            } else if ("/=".equals(text)) {
                return new Token(TOKEN_ASSIGN, offset - text.length(), line, text);
            } else if ("//".equals(text)) {
                splitChars = SINGLE_COMMENT_SPLIT_CHARS;
                setCurrentToken(new Token(TOKEN_SINGLE_COMMENT, line, offset - 1));
            }
        }
    }

    /**
     * Try to get keyword/word token.
     *
     * @param text the current text.
     * @return the token or null.
     */
    private Token tryToGetToken(final String text) {

        if (KEYWORDS.contains(text)) {
            offset--;
            return new Token(TOKEN_KEYWORD, offset - text.length(), line, text);
        } else if (isWord(text)) {
            offset--;
            return new Token(TOKEN_WORD, offset - text.length(), line, text);
        }
        return null;
    }

    /**
     * Build a char token.
     *
     * @param text          the text of this token.
     * @param ch            the symbol of this token.
     * @param charTokenType the type of this token.
     * @return the new token.
     */
    private Token handleCharToken(final String text, final char ch, final int charTokenType) {

        if (isWord(text)) {
            offset--;
            return new Token(TOKEN_WORD, offset - text.length(), line, text);
        }

        return new Token(charTokenType, offset - 1, line, Character.toString(ch));
    }

    /**
     * Checks the text.
     *
     * @param text the text.
     * @return true if the text is number or is word.
     */
    private boolean isWord(final String text) {

        if (text == null || text.isEmpty()) {
            return false;
        }

        if (isNumber(text)) {
            return true;
        }

        for (int i = 0, length = text.length(); i < length; i++) {
            final char ch = text.charAt(i);
            if (!Character.isLetterOrDigit(ch) && ch != '_') {
                return false;
            }
        }

        return true;
    }

    private boolean isNumber(final String text) {
        if (text == null) return false;
        try {
            Double.parseDouble(text);
            return true;
        } catch (final NumberFormatException e) {
        }
        return false;
    }

    private String getText(final String text) {
        return text == null ? "" : text;
    }

    /**
     * Save the current state of the parser.
     */
    private void saveState() {
        savedLine = line;
        savedOffset = offset;
    }

    /**
     * Restore the saved state of this parser.
     */
    private void restoreState() {
        line = savedLine;
        offset = savedOffset;
    }
}
