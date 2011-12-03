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

public class BitmapCharacterSet implements Savable {

    private int lineHeight;
    private int base;
    private int renderedSize;
    private int width;
    private int height;
    private IntMap<IntMap<BitmapCharacter>> characters;
    private int pageSize;

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(lineHeight, "lineHeight", 0);
        oc.write(base, "base", 0);
        oc.write(renderedSize, "renderedSize", 0);
        oc.write(width, "width", 0);
        oc.write(height, "height", 0);
        oc.write(pageSize, "pageSize", 0);

        int[] styles = new int[characters.size()];
        int index = 0;
        for (Entry<IntMap<BitmapCharacter>> entry : characters) {
            int style = entry.getKey();
            styles[index] = style;
            index++;
            IntMap<BitmapCharacter> charset = entry.getValue();
            writeCharset(oc, style, charset);
        }
        oc.write(styles, "styles", null);
    }

    protected void writeCharset(OutputCapsule oc, int style, IntMap<BitmapCharacter> charset) throws IOException {
        int size = charset.size();
        short[] indexes = new short[size];
        BitmapCharacter[] chars = new BitmapCharacter[size];
        int i = 0;
        for (Entry<BitmapCharacter> chr : charset){
            indexes[i] = (short) chr.getKey();
            chars[i] = chr.getValue();
            i++;
        }

        oc.write(indexes, "indexes"+style, null);
        oc.write(chars,   "chars"+style,   null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        lineHeight = ic.readInt("lineHeight", 0);
        base = ic.readInt("base", 0);
        renderedSize = ic.readInt("renderedSize", 0);
        width = ic.readInt("width", 0);
        height = ic.readInt("height", 0);
        pageSize = ic.readInt("pageSize", 0);
        int[] styles = ic.readIntArray("styles", null);

        for (int style : styles) {
            characters.put(style, readCharset(ic, style));
        }
    }

    private IntMap<BitmapCharacter> readCharset(InputCapsule ic, int style) throws IOException {
        IntMap<BitmapCharacter> charset = new IntMap<BitmapCharacter>();
        short[] indexes = ic.readShortArray("indexes"+style, null);
        Savable[] chars = ic.readSavableArray("chars"+style, null);

        for (int i = 0; i < indexes.length; i++){
            int index = indexes[i] & 0xFFFF;
            BitmapCharacter chr = (BitmapCharacter) chars[i];
            charset.put(index, chr);
        }
        return charset;
    }

    public BitmapCharacterSet() {
        characters = new IntMap<IntMap<BitmapCharacter>>();
    }

    public BitmapCharacter getCharacter(int index){
        return getCharacter(index, 0);
    }
    
    public BitmapCharacter getCharacter(int index, int style){
        IntMap<BitmapCharacter> map = getCharacterSet(style);
        return map.get(index);
    }

    private IntMap<BitmapCharacter> getCharacterSet(int style) {
        if (characters.size() == 0) {
            characters.put(style, new IntMap<BitmapCharacter>());
        }
        return characters.get(style);
    }

    public void addCharacter(int index, BitmapCharacter ch){
        getCharacterSet(0).put(index, ch);
    }

    public int getLineHeight() {
        return lineHeight;
    }

    public void setLineHeight(int lineHeight) {
        this.lineHeight = lineHeight;
    }

    public int getBase() {
        return base;
    }

    public void setBase(int base) {
        this.base = base;
    }

    public int getRenderedSize() {
        return renderedSize;
    }

    public void setRenderedSize(int renderedSize) {
        this.renderedSize = renderedSize;
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
    
    /**
     * Merge two fonts.
     * If two font have the same style, merge will fail.
     * @param styleSet Style must be assigned to this.
     * @author Yonghoon
     */
    public void merge(BitmapCharacterSet styleSet) {
        if (this.renderedSize != styleSet.renderedSize) {
            throw new RuntimeException("Only support same font size");
        }
        for (Entry<IntMap<BitmapCharacter>> entry : styleSet.characters) {
            int style = entry.getKey();
            if (style == 0) {
                throw new RuntimeException("Style must be set first. use setStyle(int)");
            }
            IntMap<BitmapCharacter> charset = entry.getValue();
            this.lineHeight = Math.max(this.lineHeight, styleSet.lineHeight);
            IntMap<BitmapCharacter> old = this.characters.put(style, charset);
            if (old != null) {
                throw new RuntimeException("Can't override old style");
            }
            
            for (Entry<BitmapCharacter> charEntry : charset) {
                BitmapCharacter ch = charEntry.getValue();
                ch.setPage(ch.getPage() + this.pageSize);
            }
        }
        this.pageSize += styleSet.pageSize;
    }

    public void setStyle(int style) {
        if (characters.size() > 1) {
            throw new RuntimeException("Applicable only for single style font");
        }
        Entry<IntMap<BitmapCharacter>> entry = characters.iterator().next();
        IntMap<BitmapCharacter> charset = entry.getValue();
        characters.remove(entry.getKey());
        characters.put(style, charset);
    }

    void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}