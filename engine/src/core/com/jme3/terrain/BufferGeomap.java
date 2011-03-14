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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Implementation of the Geomap interface which stores data in memory as native buffers
 */
public class BufferGeomap extends AbstractGeomap implements Geomap, Savable {

    protected FloatBuffer hdata;
    protected ByteBuffer ndata;
    protected int width, height, maxval;

    public BufferGeomap() {}
    
    public BufferGeomap(FloatBuffer heightData, ByteBuffer normalData, int width, int height, int maxval){
        this.hdata = heightData;
        this.ndata = normalData;
        this.width = width;
        this.height = height;
        this.maxval = maxval;
    }

    public BufferGeomap(int width, int height, int maxval) {
        this(ByteBuffer.allocateDirect(width*height*4).asFloatBuffer(),null,width,height,maxval);
    }

    public FloatBuffer getHeightData(){
        if (!isLoaded())
            return null;

        return hdata;
    }

    public ByteBuffer getNormalData(){
        if (!isLoaded() || !hasNormalmap())
            return null;

        return ndata;
    }

    public int getMaximumValue(){
        return maxval;
    }

    public float getValue(int x, int y) {
        return hdata.get(y*width+x);
    }

    public float getValue(int i) {
        return hdata.get(i);
    }

    public Vector3f getNormal(int x, int y, Vector3f store) {
        return getNormal(y*width+x,store);
    }

    public Vector3f getNormal(int i, Vector3f store) {
        ndata.position( i*3 );
        if (store==null) store = new Vector3f();
        store.setX( (((float)(ndata.get() & 0xFF)/255f)-0.5f)*2f );
        store.setY( (((float)(ndata.get() & 0xFF)/255f)-0.5f)*2f );
        store.setZ( (((float)(ndata.get() & 0xFF)/255f)-0.5f)*2f );
        return store;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public SharedGeomap getSubGeomap(int x, int y, int w, int h) {
        if (w+x > width)
            w = width - x;
        if (h+y > height)
            h = height - y;

        return new SharedBufferGeomap(this,x,y,w,h);
    }

    public Geomap copySubGeomap(int x, int y, int w, int h){
        FloatBuffer nhdata = ByteBuffer.allocateDirect(w*h*4).asFloatBuffer();
        hdata.position(y*width+x);
        for (int cy = 0; cy < height; cy++){
            hdata.limit(hdata.position()+w);
            nhdata.put(hdata);
            hdata.limit(hdata.capacity());
            hdata.position(hdata.position()+width);
        }
        nhdata.flip();

        ByteBuffer nndata = null;
        if (ndata!=null){
            nndata = ByteBuffer.allocateDirect(w*h*3);
            ndata.position( (y*width+x)*3 );
            for (int cy = 0; cy < height; cy++){
                ndata.limit(ndata.position()+w*3);
                nndata.put(ndata);
                ndata.limit(ndata.capacity());
                ndata.position(ndata.position()+width*3);
            }
            nndata.flip();
        }

        return new BufferGeomap(nhdata,nndata,w,h,maxval);
    }

    public boolean hasNormalmap() {
        return ndata != null;
    }

    public boolean isLoaded() {
        return true;
    }

//    @Override
//    public FloatBuffer writeNormalArray(FloatBuffer store) {
//        if (!isLoaded() || !hasNormalmap()) throw new NullPointerException();
//
//        if (store!=null){
//            if (store.remaining() < width*height*3)
//                throw new BufferUnderflowException();
//        }else{
//            store = BufferUtils.createFloatBuffer(width*height*3);
//        }
//        ndata.rewind();
//
//        for (int z = 0; z < height; z++){
//            for (int x = 0; x < width; x++){
//                float r = ((float)(ndata.get() & 0xFF)/255f -0.5f) * 2f;
//                float g = ((float)(ndata.get() & 0xFF)/255f -0.5f) * 2f;
//                float b = ((float)(ndata.get() & 0xFF)/255f -0.5f) * 2f;
//                store.put(r).put(b).put(g);
//            }
//        }
//
//        return store;
//    }

    @Override
    public FloatBuffer writeVertexArray(FloatBuffer store, Vector3f scale, boolean center) {
        if (!isLoaded()) throw new NullPointerException();

        if (store!=null){
            if (store.remaining() < width*height*3)
                throw new BufferUnderflowException();
        }else{
            store = BufferUtils.createFloatBuffer(width*height*3);
        }
        hdata.rewind();

        assert hdata.limit() == height*width;

        Vector3f offset = new Vector3f(-getWidth() * scale.x * 0.5f,
                                       0,
                                       -getWidth() * scale.z * 0.5f);
        if (!center)
            offset.zero();

        for (int z = 0; z < height; z++){
            for (int x = 0; x < width; x++){
                store.put( (float)x*scale.x + offset.x );
                store.put( (float)hdata.get()*scale.y );
                store.put( (float)z*scale.z + offset.z );
            }
        }

        return store;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(hdata, "hdata", null);
        oc.write(width, "width", 0);
        oc.write(height, "height", 0);
        oc.write(maxval, "maxval", 0);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        hdata = ic.readFloatBuffer("hdata", null);
        width = ic.readInt("width", 0);
        height = ic.readInt("height", 0);
        maxval = ic.readInt("maxval", 0);
    }

    /**
     * Populate the height data from the supplied mesh.
     * The mesh's dimensions should be the same as width and height
     * of this geomap
     */
    public void populateHdataFromMesh(Mesh mesh) {
        hdata = BufferUtils.createFloatBuffer(width*height);
        hdata.rewind();
        VertexBuffer pb = mesh.getBuffer(Type.Position);
        FloatBuffer fb = (FloatBuffer) pb.getData();
        for (int r=0; r<height; r++) {
            for (int c=0; c<width; c++) {
                float f = fb.get( (width*r) + c + 1);
                hdata.put( f );
            }
        }
    }
}
