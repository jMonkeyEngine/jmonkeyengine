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
import com.jme3.math.ColorRGBA;

/**
 * Defines a String that is to be drawn in one block that can be constrained by a {@link Rectangle}. Also holds
 * formatting information for the StringBlock
 *
 * @author dhdd
 */
class StringBlock implements Cloneable {

    private String text;
    private Rectangle textBox;
    private Align alignment = Align.Left;
    private VAlign valignment = VAlign.Top;
    private float size;
    private ColorRGBA color = new ColorRGBA(ColorRGBA.White);
    private boolean kerning;
    private int lineCount;
    private LineWrapMode wrapType = LineWrapMode.Word;
    private float[] tabPos;
    private float tabWidth = 50;
    private char ellipsisChar = 0x2026;

    /**
     *
     * @param text the text that the StringBlock will hold
     * @param textBox the rectangle that constrains the text
     * @param alignment the initial alignment of the text
     * @param size the size in pixels (vertical size of a single line)
     * @param color the initial color of the text
     * @param kerning
     */
    StringBlock(String text, Rectangle textBox, BitmapFont.Align alignment, float size, ColorRGBA color,
            boolean kerning) {
        this.text = text;
        this.textBox = textBox;
        this.alignment = alignment;
        this.size = size;
        this.color.set(color);
        this.kerning = kerning;
    }

    StringBlock(){
        this.text = "";
        this.textBox = null;
        this.alignment = Align.Left;
        this.size = 100;
        this.color.set(ColorRGBA.White);
        this.kerning = true;
    }

    @Override
    public StringBlock clone(){
        try {
            StringBlock clone = (StringBlock) super.clone();
            clone.color = color.clone();
            if (textBox != null)
                clone.textBox = textBox.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    String getText() {
        return text;
    }

    void setText(String text){
        this.text = text == null ? "" : text;
    }

    Rectangle getTextBox() {
        return textBox;
    }

    void setTextBox(Rectangle textBox) {
        this.textBox = textBox;
    }

    BitmapFont.Align getAlignment() {
        return alignment;
    }
    
    BitmapFont.VAlign getVerticalAlignment() {
        return valignment;
    }

    void setAlignment(BitmapFont.Align alignment) {
        this.alignment = alignment;
    }
    
    void setVerticalAlignment(BitmapFont.VAlign alignment) {
        this.valignment = alignment;
    }

    float getSize() {
        return size;
    }

    void setSize(float size) {
        this.size = size;
    }

    ColorRGBA getColor() {
        return color;
    }

    void setColor(ColorRGBA color) {
        this.color.set(color);
    }

    boolean isKerning() {
        return kerning;
    }

    void setKerning(boolean kerning) {
        this.kerning = kerning;
    }

    int getLineCount() {
        return lineCount;
    }

    void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }
    
    LineWrapMode getLineWrapMode() {
        return wrapType;
    }
    
    /**
     * available only when bounding is set. <code>setBox()</code> method call is needed in advance. 
     * @param wrap true when word need not be split at the end of the line.
     */
    void setLineWrapMode(LineWrapMode wrap) {
        this.wrapType = wrap;
    }
    
    void setTabWidth(float tabWidth) {
        this.tabWidth = tabWidth;
    }

    void setTabPosition(float[] tabs) {
        this.tabPos = tabs;
    }
    
    float getTabWidth() {
        return tabWidth;
    }
    
    float[] getTabPosition() {
        return tabPos;
    }

    void setEllipsisChar(char c) {
        this.ellipsisChar = c;
    }

    int getEllipsisChar() {
        return ellipsisChar;
    }
}