package com.jme3.shader.glsl.parser;

/**
 * The class to present a some logic text token.
 *
 * @author JavaSaBr
 */
public class Token {

    final static int EOF = -1;
    final static int INVALID = 0;
    final static int SKIP = 1;

    final static Token EOF_TOKEN = new Token(EOF, "EOF");
    final static Token INVALID_TOKEN = new Token(INVALID, "INVALID");
    final static Token SKIP_TOKEN = new Token(SKIP, "SKIP");

    /**
     * The token text.
     */
    private String text;

    /**
     * The offset.
     */
    private int offset;

    /**
     * The length.
     */
    private int length;

    /**
     * The line.
     */
    private int line;

    /**
     * The type.
     */
    private int type;

    private Token() {
        this(0, "INVALID");
    }

    public Token(final int type) {
        this(type, null);
    }

    public Token(final int type, final String text) {
        this(type, -1, -1, text);
    }

    public Token(final int type, final int line, final int offset) {
        this(type, offset, line, null);
    }

    public Token(final int type, final int offset, final int line, final String text) {
        this.text = text;
        this.offset = offset;
        this.length = text == null ? -1 : text.length();
        this.line = line;
        this.type = type;
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
     * Sets the text.
     *
     * @param text the text.
     */
    void setText(final String text) {
        this.text = text;
    }

    /**
     * Gets the length.
     *
     * @return the length.
     */
    int getLength() {
        return length;
    }

    /**
     * Sets the length.
     *
     * @param length the length.
     */
    void setLength(final int length) {
        this.length = length;
    }

    /**
     * Gets the type.
     *
     * @return the type.
     */
    public int getType() {
        return type;
    }

    /**
     * Gets the line.
     *
     * @return the line.
     */
    int getLine() {
        return line;
    }

    /**
     * Sets the line.
     *
     * @param line the line.
     */
    void setLine(int line) {
        this.line = line;
    }

    /**
     * Gets the offset.
     *
     * @return the offset.
     */
    int getOffset() {
        return offset;
    }

    /**
     * Sets the offset.
     *
     * @param offset the offset.
     */
    void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public String toString() {
        return "Token:\n" +
                "  text = '" + text + "\'\n" +
                "  offset = " + offset + "\n" +
                "  length = " + length + "\n" +
                "  line = " + line + "\n" +
                "  type = " + type;
    }
}
