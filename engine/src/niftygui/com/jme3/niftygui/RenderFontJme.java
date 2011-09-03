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

package com.jme3.niftygui;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import de.lessvoid.nifty.spi.render.RenderFont;

public class RenderFontJme implements RenderFont {

    private NiftyJmeDisplay display;
    private BitmapFont font;
    private BitmapText text;
    private float actualSize;

    /**
     * Initialize the font.
     * @param name font filename
     */
    public RenderFontJme(String name, NiftyJmeDisplay display) {
        this.display = display;
        font = display.getAssetManager().loadFont(name);
        if (font == null) {
            throw new RuntimeException( "Font not loaded:" + name );
        }
        text = new BitmapText(font);
        actualSize = font.getPreferredSize();
        text.setSize(actualSize);
    }

    public BitmapText getText(){
        return text;
    }

    /**
     * get font height.
     * @return height
     */
    public int getHeight() {
        return (int) text.getLineHeight();
    }

    /**
     * get font width of the given string.
     * @param text text
     * @return width of the given text for the current font
     */
    public int getWidth(final String str) {
        if (str.length() == 0)
            return 0;
 
        // Note: BitmapFont is now fixed to return the proper line width
        //       at least for now.  The older commented out (by someone else, not me)
        //       code below is arguably 'more accurate' if BitmapFont gets
        //       buggy again.  The issue is that the BitmapText and BitmapFont
        //       use a different algorithm for calculating size and both must
        //       be modified in sync.       
        int result = (int) font.getLineWidth(str);
//        text.setText(str);
//        text.updateLogicalState(0);
//        int result = (int) text.getLineWidth();

        return result;
    }

    /**
     * Return the width of the given character including kerning information.
     * @param currentCharacter current character
     * @param nextCharacter next character
     * @param size font size
     * @return width of the character or null when no information for the character is available
     */
    public Integer getCharacterAdvance(final char currentCharacter, final char nextCharacter, final float size) {
        return Integer.valueOf( Math.round(font.getCharacterAdvance(currentCharacter, nextCharacter, size)) );
    }

    public void dispose() {
    }
}