/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
import com.jme3.font.ColorTags.Range;
import com.jme3.math.ColorRGBA;

import java.util.LinkedList;

/**
 * Manage and align LetterQuads
 * @author YongHoon
 */
class Letters {
    private final LetterQuad head;
    private final LetterQuad tail;
    private final BitmapFont font;
    private LetterQuad current;
    private StringBlock block;
    private float totalWidth;
    private float totalHeight;
    private ColorTags colorTags = new ColorTags();
    private ColorRGBA baseColor = null;
    private float baseAlpha = -1;
    private String plainText;

    Letters(BitmapFont font, StringBlock bound, boolean rightToLeft) {
        final String text = bound.getText();
        this.block = bound;
        this.font = font;
        head = new LetterQuad(font, rightToLeft);
        tail = new LetterQuad(font, rightToLeft);
        setText(text);
    }

    void setText(final String text) {
        colorTags.setText(text);
        plainText = colorTags.getPlainText();

        head.setNext(tail);
        tail.setPrevious(head);
        current = head;
        if (text != null && plainText.length() > 0) {
            LetterQuad l = head;
            for (int i = 0; i < plainText.length(); i++) {
                l = l.addNextCharacter(plainText.charAt(i));
                if (baseColor != null) {
                    // Give the letter a default color if
                    // one has been provided.
                    l.setColor( baseColor );
                }
            }
        }

        LinkedList<Range> ranges = colorTags.getTags();
        if (!ranges.isEmpty()) {
            for (int i = 0; i < ranges.size()-1; i++) {
                Range start = ranges.get(i);
                Range end = ranges.get(i+1);
                setColor(start.start, end.start, start.color);
            }
            Range end = ranges.getLast();
            setColor(end.start, plainText.length(), end.color);
        }

        invalidate();
    }

    LetterQuad getHead() {
        return head;
    }

    LetterQuad getTail() {
        return tail;
    }

    void update() {
        LetterQuad l = head;
        int lineCount = 1;
        BitmapCharacter ellipsis = font.getCharSet().getCharacter(block.getEllipsisChar());
        float ellipsisWidth = ellipsis!=null? ellipsis.getWidth()*getScale(): 0;

        while (!l.isTail()) {
            if (l.isInvalid()) {
                l.update(block);

                if (l.isInvalid(block)) {
                    switch (block.getLineWrapMode()) {
                    case Character:
                        lineWrap(l);
                        lineCount++;
                        break;
                    case Word:
                        if (!l.isBlank()) {
                            // search last blank character before this word
                            LetterQuad blank = l;
                            while (!blank.isBlank()) {
                                if (blank.isLineStart() || blank.isHead()) {
                                    lineWrap(l);
                                    lineCount++;
                                    blank = null;
                                    break;
                                }
                                blank = blank.getPrevious();
                            }
                            if (blank != null) {
                                blank.setEndOfLine();
                                lineCount++;
                                while (blank != l) {
                                    blank = blank.getNext();
                                    blank.invalidate();
                                    blank.update(block);
                                }
                            }
                        }
                        break;
                    case NoWrap:
                        LetterQuad cursor = l.getPrevious();
                        while (cursor.isInvalid(block, ellipsisWidth) && !cursor.isLineStart()) {
                            cursor = cursor.getPrevious();
                        }
                        cursor.setBitmapChar(ellipsis);
                        cursor.update(block);
                        cursor = cursor.getNext();
                        while (!cursor.isTail() && !cursor.isLineFeed()) {
                            cursor.setBitmapChar(null);
                            cursor.update(block);
                            cursor = cursor.getNext();
                        }
                        break;
                    case Clip:
                        // Clip the character that falls out of bounds
                        l.clip(block);

                        // Clear the rest up to the next line feed.
                        for( LetterQuad q = l.getNext(); !q.isTail() && !q.isLineFeed(); q = q.getNext() ) {
                            q.setBitmapChar(null);
                            q.update(block);
                        }
                        break;
                    }
                }
            } else if (current.isInvalid(block)) {
                invalidate(current);
            }
            if (l.isEndOfLine()) {
                lineCount++;
            }
            l = l.getNext();
        }

        block.setLineCount(lineCount);
        align();
        rewind();
    }

    private void align() {
        final Align alignment = block.getAlignment();
        final VAlign valignment = block.getVerticalAlignment();
        if (block.getTextBox() == null || (alignment == Align.Left && valignment == VAlign.Top))
            return;
        LetterQuad cursor = tail.getPrevious();
        cursor.setEndOfLine();
        final float width = block.getTextBox().width;
        final float height = block.getTextBox().height;
        float lineWidth = 0;
        float gapX = 0;
        float gapY = 0;
        validateSize();
        if (totalHeight < height) { // align vertically only for no overflow
            switch (valignment) {
            case Top:
                gapY = 0;
                break;
            case Center:
                gapY = (height - totalHeight) * 0.5f;
                break;
            case Bottom:
                gapY = height - totalHeight;
                break;
            }
        }
        while (!cursor.isHead()) {
            if (cursor.isEndOfLine()) {
                lineWidth = cursor.getX1()-block.getTextBox().x;
                if (alignment == Align.Center) {
                    gapX = (width-lineWidth)/2;
                } else if (alignment == Align.Right) {
                    gapX = width-lineWidth;
                } else {
                    gapX = 0;
                }
            }
            cursor.setAlignment(gapX, gapY);
            cursor = cursor.getPrevious();
        }
    }

    private void lineWrap(LetterQuad l) {
        if (l.isHead() || l.isBlank())
            return;
        l.getPrevious().setEndOfLine();
        l.invalidate();
        l.update(block); // TODO: update from l
    }

    float getCharacterX0() {
        return current.getX0();
    }

    float getCharacterY0() {
        return current.getY0();
    }

    float getCharacterX1() {
        return current.getX1();
    }

    float getCharacterY1() {
        return current.getY1();
    }

    float getCharacterAlignX() {
        return current.getAlignX();
    }

    float getCharacterAlignY() {
        return current.getAlignY();
    }

    float getCharacterWidth() {
        return current.getWidth();
    }

    float getCharacterHeight() {
        return current.getHeight();
    }

    public boolean nextCharacter() {
        if (current.isTail())
            return false;
        current = current.getNext();
        return true;
    }

    public int getCharacterSetPage() {
        return current.getBitmapChar().getPage();
    }

    public LetterQuad getQuad() {
        return current;
    }

    public void rewind() {
        current = head;
    }

    public void invalidate() {
        invalidate(head);
    }

    public void invalidate(LetterQuad cursor) {
        totalWidth = -1;
        totalHeight = -1;

        while (!cursor.isTail() && !cursor.isInvalid()) {
            cursor.invalidate();
            cursor = cursor.getNext();
        }
    }

    float getScale() {
        return block.getSize() / font.getCharSet().getRenderedSize();
    }

    public boolean isPrintable() {
        return current.getBitmapChar() != null;
    }

    float getTotalWidth() {
        validateSize();
        return totalWidth;
    }

    float getTotalHeight() {
        validateSize();
        return totalHeight;
    }

    void validateSize() {
        if (totalWidth < 0) {
            LetterQuad l = head;
            while (!l.isTail()) {
                totalWidth = Math.max(totalWidth, l.getX1());
                l = l.getNext();
            }
        }
        totalHeight = font.getLineHeight(block) * block.getLineCount();
    }

    /**
     * @param start start index to set style. inclusive.
     * @param end   end index to set style. EXCLUSIVE.
     * @param style
     */
    void setStyle(int start, int end, int style) {
        LetterQuad cursor = head.getNext();
        while (!cursor.isTail()) {
            if (cursor.getIndex() >= start && cursor.getIndex() < end) {
                cursor.setStyle(style);
            }
            cursor = cursor.getNext();
        }
    }

    /**
     * Sets the base color for all new letter quads and resets
     * the color of existing letter quads.
     */
    void setColor( ColorRGBA color ) {
        baseColor = color;
        colorTags.setBaseColor(color);
        setColor( 0, block.getText().length(), color );
    }

    ColorRGBA getBaseColor() {
        return baseColor;
    }

    /**
     * @param start start index to set style. inclusive.
     * @param end   end index to set style. EXCLUSIVE.
     * @param color
     */
    void setColor(int start, int end, ColorRGBA color) {
        LetterQuad cursor = head.getNext();
        while (!cursor.isTail()) {
            if (cursor.getIndex() >= start && cursor.getIndex() < end) {
                cursor.setColor(color);
            }
            cursor = cursor.getNext();
        }
    }

    float getBaseAlpha() {
        return baseAlpha;
    }

    void setBaseAlpha( float alpha ) {        this.baseAlpha = alpha;
        colorTags.setBaseAlpha(alpha);

        if (alpha == -1) {
            alpha = baseColor != null ? baseColor.a : 1;
        }

        // Forward the new alpha to the letter quads
        LetterQuad cursor = head.getNext();
        while (!cursor.isTail()) {
            cursor.setAlpha(alpha);
            cursor = cursor.getNext();
        }

        // If the alpha was reset to "default", ie: -1
        // then the color tags are potentially reset and
        // we need to reapply them.  This has to be done
        // second since it may override any alpha values
        // set above... but you still need to do the above
        // since non-color tagged text is treated differently
        // even if part of a color tagged string.
        if (baseAlpha == -1) {
            LinkedList<Range> ranges = colorTags.getTags();
            if (!ranges.isEmpty()) {
                for (int i = 0; i < ranges.size()-1; i++) {
                    Range start = ranges.get(i);
                    Range end = ranges.get(i+1);
                    setColor(start.start, end.start, start.color);
                }
                Range end = ranges.getLast();
                setColor(end.start, plainText.length(), end.color);
            }
        }

        invalidate();
    }

}
