package com.jme3.font;

/**
 * Used for selecting character shape in cursive bitmap text. In cursive scripts,
 * the appearance of a letter changes depending on its position:
 * isolated, initial (joined on the left), medial (joined on both sides)
 * and final (joined on the right) of a word.
 *
 * @author Ali-RS
 */
public interface GlyphParser {

    public CharSequence parse(CharSequence text);

}