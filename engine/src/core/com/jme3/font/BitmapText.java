/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author YongHoon
 */
public class BitmapText extends Node {
    private BitmapFont font;
    private StringBlock block;
    private boolean needRefresh = true;
    private final BitmapTextPage[] textPages;
    private Letters letters;

    public BitmapText(BitmapFont font) {
        this(font, false, false);
    }

    public BitmapText(BitmapFont font, boolean rightToLeft) {
        this(font, rightToLeft, false);
    }

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
        BitmapText clone = (BitmapText) super.clone();
        for (int i = 0; i < textPages.length; i++) {
            clone.textPages[i] = textPages[i].clone();
        }
        clone.block = block.clone();
        clone.needRefresh = true;
        return clone;
    }

    public BitmapFont getFont() {
        return font;
    }
    
    /**
     * Changes text size
     * @param size text size
     */
    public void setSize(float size) {
        block.setSize(size);
        needRefresh = true;
        letters.invalidate();
    }

    /**
     *
     * @param text charsequence to change text to
     */
    public void setText(CharSequence text) {
        // note: text.toString() is free if text is already a java.lang.String.
        setText( text != null ? text.toString() : null );
    }

    /**
     *
     * @param text String to change text to
     */
    public void setText(String text) {
        text = text == null ? "" : text;
        if (text == block.getText() || block.getText().equals(text)) {
            return;
        }

        block.setText(text);
        letters.setText(text);
        needRefresh = true;
    }

    /**
     * @return returns text
     */
    public String getText() {
        return block.getText();
    }

    /**
     * @return color of the text
     */
    public ColorRGBA getColor() {
        return letters.getBaseColor();
    }

    /**
     * changes text color. all substring colors are deleted.
     * @param color new color of text
     */
    public void setColor(ColorRGBA color) {
        letters.setColor(color);
        letters.invalidate(); // TODO: Don't have to align.
        needRefresh = true;
    }

    /**
     * Define area where bitmaptext will be rendered
     * @param rect position and size box where text is rendered
     */
    public void setBox(Rectangle rect) {
        block.setTextBox(rect);
        letters.invalidate();
        needRefresh = true;
    }
    
    /**
     * @return height of the line
     */
    public float getLineHeight() {
        return font.getLineHeight(block);
    }
    
    /**
     * @return height of whole textblock
     */
    public float getHeight() {
        if (needRefresh) {
            assemble();
        }
        float height = getLineHeight()*block.getLineCount();
        Rectangle textBox = block.getTextBox();
        if (textBox != null) {
            return Math.max(height, textBox.height);
        }
        return height;
    }
    
    /**
     * @return width of line
     */
    public float getLineWidth() {
        if (needRefresh) {
            assemble();
        }
        Rectangle textBox = block.getTextBox();
        if (textBox != null) {
            return Math.max(letters.getTotalWidth(), textBox.width);
        }
        return letters.getTotalWidth();
    }
    
    /**
     * @return line count
     */
    public int getLineCount() {
        if (needRefresh) {
            assemble();
        }
        return block.getLineCount();
    }
    
    public LineWrapMode getLineWrapMode() {
        return block.getLineWrapMode();
    }
    
    /**
     * Set horizontal alignment. Applicable only when text bound is set.
     * @param align
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
     * Set vertical alignment. Applicable only when text bound is set.
     * @param align
     */
    public void setVerticalAlignment(BitmapFont.VAlign align) {
        if (block.getTextBox() == null && align != VAlign.Top) {
            throw new RuntimeException("Bound is not set");
        }
        block.setVerticalAlignment(align);
        letters.invalidate();
        needRefresh = true;
    }
    
    public BitmapFont.Align getAlignment() {
        return block.getAlignment();
    }
    
    public BitmapFont.VAlign getVerticalAlignment() {
        return block.getVerticalAlignment();
    }
    
    /**
     * Set the font style of substring. If font doesn't contain style, default style is used
     * @param start start index to set style. inclusive.
     * @param end   end index to set style. EXCLUSIVE.
     * @param style
     */
    public void setStyle(int start, int end, int style) {
        letters.setStyle(start, end, style);
    }
    
    /**
     * Set the font style of substring. If font doesn't contain style, default style is applied
     * @param regexp regular expression
     * @param style
     */
    public void setStyle(String regexp, int style) {
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(block.getText());
        while (m.find()) {
            setStyle(m.start(), m.end(), style);
        }
    }
    
    /**
     * Set the color of substring.
     * @param start start index to set style. inclusive.
     * @param end   end index to set style. EXCLUSIVE.
     * @param color
     */
    public void setColor(int start, int end, ColorRGBA color) {
        letters.setColor(start, end, color);
        letters.invalidate();
        needRefresh = true;
    }
    
    /**
     * Set the color of substring.
     * @param regexp regular expression
     * @param color
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
     * @param tabs tab positions
     */
    public void setTabPosition(float... tabs) {
        block.setTabPosition(tabs);
        letters.invalidate();
        needRefresh = false;
    }
    
    /**
     * used for the tabs over the last tab position.
     * @param width tab size
     */
    public void setTabWidth(float width) {
        block.setTabWidth(width);
        letters.invalidate();
        needRefresh = false;
    }
    
    /**
     * for setLineWrapType(LineWrapType.NoWrap),
     * set the last character when the text exceeds the bound.
     * @param c
     */
    public void setEllipsisChar(char c) {
        block.setEllipsisChar(c);
        letters.invalidate();
        needRefresh = false;
    }

    /**
     * Available only when bounding is set. <code>setBox()</code> method call is needed in advance.
     * true when
     * @param wrap NoWrap   : Letters over the text bound is not shown. the last character is set to '...'(0x2026)
     *             Character: Character is split at the end of the line.
     *             Word     : Word is split at the end of the line.
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

    private void assemble() {
        // first generate quadlist
        letters.update();
        
        for (int i = 0; i < textPages.length; i++) {
            textPages[i].assemble(letters);
        }
        needRefresh = false;
    }
    
    public void render(RenderManager rm) {
        for (BitmapTextPage page : textPages) {
            Material mat = page.getMaterial();
            mat.setTexture("Texture", page.getTexture());
            mat.render(page, rm);
        }
    }
}
