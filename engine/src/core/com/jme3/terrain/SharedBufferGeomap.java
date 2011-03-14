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

package com.jme3.terrain;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class SharedBufferGeomap extends AbstractGeomap implements SharedGeomap {

    protected final BufferGeomap parent;
    protected final FloatBuffer hdata;
    protected final ByteBuffer ndata;
    protected final int startX, startY, width, height;

    public SharedBufferGeomap(BufferGeomap parent, int x, int y, int w, int h){
        this.parent = parent;
        hdata = parent.getHeightData();
        ndata = parent.getNormalData();
        startX = x;
        startY = y;
        width = w;
        height = h;
    }

    public boolean hasNormalmap() {
        return parent.hasNormalmap();
    }

    public boolean isLoaded() {
        return parent.isLoaded();
    }

    public int getMaximumValue(){
        return parent.getMaximumValue();
    }

    public Geomap getParent() {
        return parent;
    }

    public int getXOffset() {
        return startX;
    }

    public int getYOffset() {
        return startY;
    }

    public float getValue(int x, int y) {
        return parent.getValue(startX+x,startY+y);
    }

    public float getValue(int i) {
        int r = i % width;
        return getValue(r,(i-r)/width);
    }

    public Vector3f getNormal(int x, int y, Vector3f store) {
        return parent.getNormal(startX+x,startY+y,store);
    }

    public Vector3f getNormal(int i, Vector3f store) {
        int r = i % width;
        return getNormal(r,(i-r)/width,store);
    }

    @Override
    public Vector2f getUV(int x, int y, Vector2f store){
        return parent.getUV(startX+x, startY+y, store);
    }

    @Override
    public Vector2f getUV(int i, Vector2f store){
        int r = i % width;
        return getUV(r,(i-r)/width,store);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Geomap copy() {
        return parent.copySubGeomap(startX,startY,width,height);
    }

    public SharedGeomap getSubGeomap(int x, int y, int w, int h) {
        if (x<0 || y<0 || x>width || y>height || w+x>width || h+y>height)
            throw new IndexOutOfBoundsException();

        return parent.getSubGeomap(startX+x,startY+y,w,h);
    }

    public Geomap copySubGeomap(int x, int y, int w, int h) {
        if (x<0 || y<0 || x>width || y>height || w>width || h>height)
            throw new IndexOutOfBoundsException();

        return parent.copySubGeomap(startX+x,startY+y,w,h);
    }

}
