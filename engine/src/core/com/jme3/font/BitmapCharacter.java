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

import com.jme3.export.*;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import java.io.IOException;

/**
 * Represents a single bitmap character.
 */
public class BitmapCharacter implements Savable, Cloneable {
    private char c;
    private int x;
    private int y;
    private int width;
    private int height;
    private int xOffset;
    private int yOffset;
    private int xAdvance;
    private IntMap<Integer> kerning = new IntMap<Integer>();
    private int page;
    
    public BitmapCharacter() {}
    
    public BitmapCharacter(char c) {
        this.c = c;
    }

    @Override
    public BitmapCharacter clone() {
        try {
            BitmapCharacter result = (BitmapCharacter) super.clone();
            result.kerning = kerning.clone();
            return result;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getXOffset() {
        return xOffset;
    }

    public void setXOffset(int offset) {
        xOffset = offset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public void setYOffset(int offset) {
        yOffset = offset;
    }

    public int getXAdvance() {
        return xAdvance;
    }

    public void setXAdvance(int advance) {
        xAdvance = advance;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }
    
    public char getChar() {
        return c;
    }
    
    public void setChar(char c) {
        this.c = c;
    }

    public void addKerning(int second, int amount){
        kerning.put(second, amount);
    }

    public int getKerning(int second){
        Integer i = kerning.get(second);
        if (i == null)
            return 0;
        else
            return i.intValue();
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(c, "c", 0);
        oc.write(x, "x", 0);
        oc.write(y, "y", 0);
        oc.write(width, "width", 0);
        oc.write(height, "height", 0);
        oc.write(xOffset, "xOffset", 0);
        oc.write(yOffset, "yOffset", 0);
        oc.write(xAdvance, "xAdvance", 0);

        int[] seconds = new int[kerning.size()];
        int[] amounts = new int[seconds.length];

        int i = 0;
        for (Entry<Integer> entry : kerning){
            seconds[i] = entry.getKey();
            amounts[i] = entry.getValue();
            i++;
        }

        oc.write(seconds, "seconds", null);
        oc.write(amounts, "amounts", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        c = (char) ic.readInt("c", 0);
        x = ic.readInt("x", 0);
        y = ic.readInt("y", 0);
        width = ic.readInt("width", 0);
        height = ic.readInt("height", 0);
        xOffset = ic.readInt("xOffset", 0);
        yOffset = ic.readInt("yOffset", 0);
        xAdvance = ic.readInt("xAdvance", 0);

        int[] seconds = ic.readIntArray("seconds", null);
        int[] amounts = ic.readIntArray("amounts", null);

        for (int i = 0; i < seconds.length; i++){
            kerning.put(seconds[i], amounts[i]);
        }
    }
}