/*
 * Copyright (c) 2009-2025 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.font;

import com.jme3.font.BitmapFont.Align;
import com.jme3.font.BitmapFont.VAlign;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.util.clone.Cloner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * `BitmapText` is a spatial node that displays text using a {@link BitmapFont}.
 * It handles text layout, alignment, wrapping, coloring, and styling based on
 * the properties set via its methods. The text is rendered as a series of
 * quads (rectangles) with character textures from the font's pages.
 *
 * @author YongHoon
 */
public class BitmapText extends Node {

    // The font used to render this text.
    private BitmapFont font;
    // Stores the text content and its layout properties (size, box, alignment, etc.).
    private StringBlock block;
    // A flag indicating whether the text needs to be re-assembled
    private boolean needRefresh = true;
    // An array of `BitmapTextPage` instances, each corresponding to a font page.
    private BitmapTextPage[] textPages;
    // Manages the individual letter quads, their positions, colors, and styles.
    private Letters letters;

    /**
     * Creates a new `BitmapText` instance using the specified font.
     * The text will be rendered left-to-right by default, unless the font itself
     * is configured for right-to-left rendering.
     *
     * @param font The {@link BitmapFont} to use for rendering the text (not null).
     */
    public BitmapText(BitmapFont font) {
        this(font, font.isRightToLeft(), false);
    }

    /**
     * @deprecated The "rightToLeft" flag should be specified in the font.
     * Use {@link BitmapText#BitmapText(com.jme3.font.BitmapFont)}
     *
     * @param font the font to use (not null, alias created)
     * @param rightToLeft true &rarr; right-to-left, false &rarr; left-to-right
     *     (default=false)
     */
    @Deprecated
    public BitmapText(BitmapFont font, boolean rightToLeft) {
        this(font, rightToLeft, false);
    }

    /**
     * Creates a new `BitmapText` instance with the specified font, text direction,
     * and a flag for array-based rendering.
     *
     * @param font The {@link BitmapFont} to use for rendering the text (not null).
     * @param rightToLeft true for right-to-left text rendering, false for left-to-right.
     * @param arrayBased If true, the internal text pages will use array-based buffers for rendering.
     * This might affect performance or compatibility depending on the renderer.
     */
    public BitmapText(BitmapFont font, boolean rightToLeft, boolean arrayBased) {
        textPages = new BitmapTextPage[font.getPageSize()];
        for (int page = 0; page < textPages.length; page++) {
            textPages[page] = new BitmapTextPage(font, arrayBased, page);
            attachChild(textPages[page]);
        }

        this.font = font;
        this.block = new StringBlock();
        block.setSize(font.getPreferredSize());
        letters = new Letters(font, block, rightToLeft);
    }

    @Override
    public BitmapText clone() {
        return (BitmapText) super.clone(false);
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        textPages = textPages.clone();
        for (int i = 0; i < textPages.length; i++) {
            textPages[i] = cloner.clone(textPages[i]);
        }

        // Cannot use the cloner to clone the StringBlock because it
        // is package private... so we'll forgo the (probably unnecessary)
        // reference fixup in this case and just clone it directly.
        //this.block = cloner.clone(block);
        this.block = block != null ? block.clone() : null;

        // Change in behavior: The 'letters' field was not cloned or recreated
        // before.  I'm not sure how this worked and suspect BitmapText was just
        // not cloneable if you planned to change the text later. -pspeed
        this.letters = new Letters(font, block, letters.getQuad().isRightToLeft());

        // Just noticed BitmapText is not even writable/readable really...
        // so I guess cloning doesn't come up that often.
    }

    /**
     * Returns the {@link BitmapFont} currently used by this `BitmapText` instance.
     *
     * @return The {@link BitmapFont} object.
     */
    public BitmapFont getFont() {
        return font;
    }

    /**
     * Sets the size of the text. This value scales the font's base character sizes.
     *
     * @param size The desired text size (e.g., in world units or pixels).
     */
    public void setSize(float size) {
        block.setSize(size);
        needRefresh = true;
        letters.invalidate();
    }

    /**
     * Returns the current size of the text.
     *
     * @return The text size.
     */
    public float getSize() {
        return block.getSize();
    }

    /**
     * Sets the text content to be displayed.
     *
     * @param text The `CharSequence` (e.g., `String` or `StringBuilder`) to display.
     * If null, the text will be set to an empty string.
     */
    public void setText(CharSequence text) {
        // note: text.toString() is free if text is already a java.lang.String.
        setText(text != null ? text.toString() : null);
    }

    /**
     * Sets the text content to be displayed.
     * If the new text is the same as the current text, no update occurs.
     * Otherwise, the internal `StringBlock` and `Letters` objects are updated,
     * and a refresh is flagged to re-layout the text.
     *
     * @param text The `String` to display. If null, the text will be set to an empty string.
     */
    public void setText(String text) {
        text = text == null ? "" : text;
        if (block.getText().equals(text)) {
            return;
        }

        // Update the text content
        block.setText(text);
        letters.setText(text);
        needRefresh = true;
    }

    /**
     * Returns the current text content displayed by this `BitmapText` instance.
     *
     * @return The text content as a `String`.
     */
    public String getText() {
        return block.getText();
    }

    /**
     * Returns the base color applied to the entire text.
     * Note: Substring colors set via `setColor(int, int, ColorRGBA)` or
     * `setColor(String, ColorRGBA)` will override this base color for their respective ranges.
     *
     * @return The base {@link ColorRGBA} of the text.
     */
    public ColorRGBA getColor() {
        return letters.getBaseColor();
    }

    /**
     * Sets the base color for the entire text.
     * This operation will clear any previously set substring colors.
     *
     * @param color The new base {@link ColorRGBA} for the text.
     */
    public void setColor(ColorRGBA color) {
        letters.setColor(color);
        letters.invalidate(); // TODO: Don't have to align.
        needRefresh = true;
    }

    /**
     * Sets an overall alpha (transparency) value that will be applied to all
     * letters in the text.
     * If the alpha passed is -1, the alpha reverts to its default behavior:
     * 1.0 for unspecified parts, and the encoded alpha from any color tags.
     *
     * @param alpha The desired alpha value (0.0 for fully transparent, 1.0 for fully opaque),
     * or -1 to revert to default alpha behavior.
     */
    public void setAlpha(float alpha) {
        letters.setBaseAlpha(alpha);
        needRefresh = true;
    }

    /**
     * Returns the current base alpha value applied to the text.
     *
     * @return The base alpha value, or -1 if default alpha behavior is active.
     */
    public float getAlpha() {
        return letters.getBaseAlpha();
    }

    /**
     * Defines a rectangular bounding box within which the text will be rendered.
     * This box is used for text wrapping and alignment.
     *
     * @param rect The {@link Rectangle} defining the position (x, y) and size (width, height)
     * of the text rendering area.
     */
    public void setBox(Rectangle rect) {
        block.setTextBox(rect);
        letters.invalidate();
        needRefresh = true;
    }

    /**
     * Returns the height of a single line of text, scaled by the current text size.
     *
     * @return The calculated line height.
     */
    public float getLineHeight() {
        return font.getLineHeight(block);
    }

    /**
     * Calculates and returns the total height of the entire text block,
     * considering all lines and the defined text box (if any).
     *
     * @return The total height of the text block.
     */
    public float getHeight() {
        if (needRefresh) {
            assemble();
        }
        float height = getLineHeight() * block.getLineCount();
        Rectangle textBox = block.getTextBox();
        if (textBox != null) {
            return Math.max(height, textBox.height);
        }
        return height;
    }

    /**
     * Calculates and returns the maximum width of any line in the text block.
     *
     * @return The maximum line width of the text.
     */
    public float getLineWidth() {
        if (needRefresh) {
            assemble();
        }
        Rectangle textBox = block.getTextBox();
        if (textBox != null) {
            return Math.max(letters.getTotalWidth(), textBox.width);
        }
        //  Please note that BitMaptext.getLineWidth() might differ from Font.getLineWidth()
        // -->   scale it with Font.getPreferredSize()/BitMaptext.getSize()
        return letters.getTotalWidth();
    }

    /**
     * Returns the number of lines the text currently occupies.
     *
     * @return The total number of lines.
     */
    public int getLineCount() {
        if (needRefresh) {
            assemble();
        }
        return block.getLineCount();
    }

    /**
     * Returns the current line wrapping mode set for this text.
     *
     * @return The {@link LineWrapMode} enum value.
     */
    public LineWrapMode getLineWrapMode() {
        return block.getLineWrapMode();
    }

    /**
     * Sets the horizontal alignment for the text within its bounding box.
     * This is only applicable if a text bounding box has been set using {@link #setBox(Rectangle)}.
     *
     * @param align The desired horizontal alignment (e.g., {@link Align#Left}, {@link Align#Center}, {@link Align#Right}).
     * @throws RuntimeException If a bounding box is not set and `align` is not `Align.Left`.
     */
    public void setAlignment(BitmapFont.Align align) {
        if (block.getTextBox() == null && align != Align.Left) {
            throw new RuntimeException("Bound is not set");
        }
        block.setAlignment(align);
        letters.invalidate();
        needRefresh = true;
    }

    /**
     * Sets the vertical alignment for the text within its bounding box.
     * This is only applicable if a text bounding box has been set using {@link #setBox(Rectangle)}.
     *
     * @param align The desired vertical alignment (e.g., {@link VAlign#Top}, {@link VAlign#Center}, {@link VAlign#Bottom}).
     * @throws RuntimeException If a bounding box is not set and `align` is not `VAlign.Top`.
     */
    public void setVerticalAlignment(BitmapFont.VAlign align) {
        if (block.getTextBox() == null && align != VAlign.Top) {
            throw new RuntimeException("Bound is not set");
        }
        block.setVerticalAlignment(align);
        letters.invalidate();
        needRefresh = true;
    }

    /**
     * Returns the current horizontal alignment set for the text.
     *
     * @return The current {@link Align} value.
     */
    public BitmapFont.Align getAlignment() {
        return block.getAlignment();
    }

    /**
     * Returns the current vertical alignment set for the text.
     *
     * @return The current {@link VAlign} value.
     */
    public BitmapFont.VAlign getVerticalAlignment() {
        return block.getVerticalAlignment();
    }

    /**
     * Sets the font style for a specific substring of the text.
     * If the font does not contain the specified style, the default style will be used.
     *
     * @param start The starting index of the substring (inclusive).
     * @param end   The ending index of the substring (exclusive).
     * @param style The integer style identifier to apply.
     */
    public void setStyle(int start, int end, int style) {
        letters.setStyle(start, end, style);
    }

    /**
     * Sets the font style for all substrings matching a given regular expression.
     * If the font does not contain the specified style, the default style will be used.
     *
     * @param regexp The regular expression string to match against the text.
     * @param style  The integer style identifier to apply.
     */
    public void setStyle(String regexp, int style) {
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(block.getText());
        while (m.find()) {
            setStyle(m.start(), m.end(), style);
        }
    }

    /**
     * Sets the color for a specific substring of the text.
     *
     * @param start The starting index of the substring (inclusive).
     * @param end   The ending index of the substring (exclusive).
     * @param color The desired {@link ColorRGBA} to apply to the substring.
     */
    public void setColor(int start, int end, ColorRGBA color) {
        letters.setColor(start, end, color);
        letters.invalidate();
        needRefresh = true;
    }

    /**
     * Sets the color for all substrings matching a given regular expression.
     *
     * @param regexp The regular expression string to match against the text.
     * @param color  The desired {@link ColorRGBA} to apply.
     */
    public void setColor(String regexp, ColorRGBA color) {
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(block.getText());
        while (m.find()) {
            letters.setColor(m.start(), m.end(), color);
        }
        letters.invalidate();
        needRefresh = true;
    }

    /**
     * Sets custom tab stop positions for the text.
     * Tab characters (`\t`) will align to these specified positions.
     *
     * @param tabs An array of float values representing the horizontal tab stop positions.
     */
    public void setTabPosition(float... tabs) {
        block.setTabPosition(tabs);
        letters.invalidate();
        needRefresh = true;
    }

    /**
     * Sets the default width for tabs that extend beyond the last defined tab position.
     * This value is used if a tab character is encountered after all custom tab stops have been passed.
     *
     * @param width The default width for tabs in font units.
     */
    public void setTabWidth(float width) {
        block.setTabWidth(width);
        letters.invalidate();
        needRefresh = true;
    }

    /**
     * When {@link LineWrapMode#NoWrap} is used and the text exceeds the bounding box,
     * this character will be appended to indicate truncation (e.g., '...').
     *
     * @param c The character to use as the ellipsis.
     */
    public void setEllipsisChar(char c) {
        block.setEllipsisChar(c);
        letters.invalidate();
        needRefresh = true;
    }

    /**
     * Sets the line wrapping mode for the text. This is only applicable when
     * a text bounding box has been set using {@link #setBox(Rectangle)}.
     *
     * @param wrap The desired {@link LineWrapMode}:
     * <ul>
     * <li>{@link LineWrapMode#NoWrap}: Letters exceeding the text bound are not shown.
     * The last visible character might be replaced by an ellipsis character
     * (set via {@link #setEllipsisChar(char)}).</li>
     * <li>{@link LineWrapMode#Character}: Text is split at the end of the line, even in the middle of a word.</li>
     * <li>{@link LineWrapMode#Word}: Words are split at the end of the line.</li>
     * <li>{@link LineWrapMode#Clip}: The text is hard-clipped at the border, potentially showing only a partial letter.</li>
     * </ul>
     */
    public void setLineWrapMode(LineWrapMode wrap) {
        if (block.getLineWrapMode() != wrap) {
            block.setLineWrapMode(wrap);
            letters.invalidate();
            needRefresh = true;
        }
    }

    @Override
    public void updateLogicalState(float tpf) {
        super.updateLogicalState(tpf);
        if (needRefresh) {
            assemble();
        }
    }

    /**
     * Assembles the text by generating the quad list (character positions and sizes)
     * and then populating the vertex buffers of each `BitmapTextPage`.
     * This method is called internally when `needRefresh` is true.
     */
    private void assemble() {
        // First, generate or update the list of letter quads
        // based on current text and layout properties.
        letters.update();
        // Then, for each font page, assemble its mesh data from the generated quads.
        for (BitmapTextPage textPage : textPages) {
            textPage.assemble(letters);
        }
        needRefresh = false;
    }

    /**
     * Renders the `BitmapText` spatial. This method iterates through each
     * `BitmapTextPage`, sets its texture, and renders it using the provided
     * `RenderManager`.
     *
     * @param rm The `RenderManager` responsible for drawing.
     * @param color The base color to apply during rendering. Note that colors
     * set per-substring will override this for those parts.
     */
    public void render(RenderManager rm, ColorRGBA color) {
        for (BitmapTextPage page : textPages) {
            Material mat = page.getMaterial();
            mat.setTexture("ColorMap", page.getTexture());
            // mat.setColor("Color", color); // If the material supports a "Color" parameter
            mat.render(page, rm);
        }
    }

}
