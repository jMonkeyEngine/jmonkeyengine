/*
 * GlslTokenId.java
 *
 * Created on 19.08.2007, 18:09:00
 *
 */

package net.java.nboglpack.glsleditor.lexer;

import org.netbeans.api.lexer.TokenId;

/**
 * Enumeration of GLSL token ids.
 * @author Michael Bien
 */
public enum GlslTokenId implements TokenId {

    IDENTIFIER("glsl-name"),
    INTEGER_LITERAL("glsl-literal"),
    FLOAT_LITERAL("glsl-literal"),
    FUNCTION("glsl-function"),
    KEYWORD("glsl-keyword"),
    BUILD_IN_FUNC("glsl-build-in-func"),
    BUILD_IN_VAR("glsl-build-in-var"),
    COMMENT("glsl-comment"),
    ML_COMMENT("glsl-comment"),

    PAREN("glsl-paren"),
    BRACE("glsl-brace"),
    BRACKET("glsl-bracket"),
    LEFT_ANGLE("glsl-angle"),
    RIGHT_ANGLE("glsl-angle"),

    SEMICOLON("glsl-separator"),
    COMMA("glsl-separator"),
    DOT("glsl-separator"),
    COLON("glsl-separator"),

    PERCENT("glsl-operation"),
    STAR("glsl-operation"),
    TILDE("glsl-operation"),
    QUESTION("glsl-operation"),
    BANG("glsl-operation"),
    SLASH("glsl-operation"),
    LEFT_BITSHIFT("glsl-operation"),
    RIGHT_BITSHIFT("glsl-operation"),
    PLUS("glsl-operation"),
    PLUSPLUS("glsl-operation"),
    MINUS("glsl-operation"),
    MINUSMINUS("glsl-operation"),
    AMP("glsl-operation"),
    AMPAMP("glsl-operation"),
    EQ("glsl-operation"),
    EQEQ("glsl-operation"),
    NE("glsl-operation"),
    LE("glsl-operation"),
    GE("glsl-operation"),
    BAR("glsl-operation"),
    BARBAR("glsl-operation"),
    CARET("glsl-operation"),
    CARETCARET("glsl-operation"),
    ADD_ASSIGN("glsl-operation"),
    SUB_ASSIGN("glsl-operation"),
    MUL_ASSIGN("glsl-operation"),
    DIV_ASSIGN("glsl-operation"),
    AND_ASSIGN("glsl-operation"),
    OR_ASSIGN("glsl-operation"),
    XOR_ASSIGN("glsl-operation"),
    MOD_ASSIGN("glsl-operation"),
    LEFT_BITSHIFT_ASSIGN("glsl-operation"),
    RIGHT_BITSHIFT_ASSIGN("glsl-operation"),

    WHITESPACE("glsl-whitespace"),
    END_OF_LINE("glsl-end-of-line"),
    PREPROCESSOR("glsl-preprocessor"),

    error("glsl-error");

    private final String primaryCategory;

    private GlslTokenId(String primaryCategory) {
        this.primaryCategory = primaryCategory;
    }

    public String primaryCategory() {
        return primaryCategory;
    }

}
