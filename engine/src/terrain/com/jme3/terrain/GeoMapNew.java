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
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Constructs heightfields to be used in Terrain.
 */
public class GeoMapNew implements Savable {
    
    protected FloatBuffer hdata;
    protected ByteBuffer ndata;
    protected int width, height, maxval;
    
    public GeoMapNew() {}
    
    public GeoMapNew(FloatBuffer heightData, ByteBuffer normalData, int width, int height, int maxval){
        this.hdata = heightData;
        this.ndata = normalData;
        this.width = width;
        this.height = height;
        this.maxval = maxval;
    }

    public GeoMapNew(int width, int height, int maxval) {
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

    /**
     * @return The maximum possible value that <code>getValue()</code> can 
     * return. Mostly depends on the source data format (byte, short, int, etc).
     */
    public int getMaximumValue(){
        return maxval;
    }

    /**
     * Returns the height value for a given point.
     *
     * MUST return the same value as getHeight(y*getWidth()+x)
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @returns an arbitrary height looked up from the heightmap
     *
     * @throws NullPointerException If isLoaded() is false
     */
    public float getValue(int x, int y) {
        return hdata.get(y*width+x);
    }

    /**
     * Returns the height value at the given index.
     *
     * zero index is top left of map,
     * getWidth()*getHeight() index is lower right
     *
     * @param i The index
     * @returns an arbitrary height looked up from the heightmap
     *
     * @throws NullPointerException If isLoaded() is false
     */
    public float getValue(int i) {
        return hdata.get(i);
    }

    /**
     * Returns the normal at a point
     *
     * If store is null, then a new vector is returned,
     * otherwise, the result is stored in the provided vector
     * and then returned from this method
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param store A preallocated vector for storing the normal data, optional
     * @returns store, or a new vector with the normal data if store is null
     *
     * @throws NullPointerException If isLoaded() or hasNormalmap() is false
     */
    public Vector3f getNormal(int x, int y, Vector3f store) {
        return getNormal(y*width+x,store);
    }

    /**
     * Returns the normal at an index
     *
     * If store is null, then a new vector is returned,
     * otherwise, the result is stored in the provided vector
     * and then returned from this method
     *
     * See getHeight(int) for information about index lookup
     *
     * @param i the index
     * @param store A preallocated vector for storing the normal data, optional
     * @returns store, or a new vector with the normal data if store is null
     *
     * @throws NullPointerException If isLoaded() or hasNormalmap() is false
     */
    public Vector3f getNormal(int i, Vector3f store) {
        ndata.position( i*3 );
        if (store==null) store = new Vector3f();
        store.setX( (((float)(ndata.get() & 0xFF)/255f)-0.5f)*2f );
        store.setY( (((float)(ndata.get() & 0xFF)/255f)-0.5f)*2f );
        store.setZ( (((float)(ndata.get() & 0xFF)/255f)-0.5f)*2f );
        return store;
    }

    /**
     * Returns the width of this Geomap
     *
     * @returns the width of this Geomap
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of this Geomap
     *
     * @returns the height of this Geomap
     */
    public int getHeight() {
        return height;
    }

    /**
     * Copies a section of this geomap as a new geomap
     */
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

    /**
     * Returns true if this Geomap has a normalmap associated with it
     */
    public boolean hasNormalmap() {
        return ndata != null;
    }

    /**
     * Returns true if the Geomap data is loaded in memory
     * If false, then the data is unavailable- must be loaded with load()
     * before the methods getHeight/getNormal can be used
     *
     * @returns wether the geomap data is loaded in system memory
     */
    public boolean isLoaded() {
        return true;
    }

    /**
     * Creates a normal array from the normal data in this Geomap
     *
     * @param store A preallocated FloatBuffer where to store the data (optional), size must be >= getWidth()*getHeight()*3
     * @returns store, or a new FloatBuffer if store is null
     *
     * @throws NullPointerException If isLoaded() or hasNormalmap() is false
     */
    public FloatBuffer writeNormalArray(FloatBuffer store, Vector3f scale) {
        if (!isLoaded())
            throw new NullPointerException();
        
        if (store!=null){
            if (store.remaining() < getWidth()*getHeight()*3)
                throw new BufferUnderflowException();
        }else{
            store = BufferUtils.createFloatBuffer(getWidth()*getHeight()*3);
        }
        store.rewind();
        
        if (!hasNormalmap()){
            Vector3f oppositePoint = new Vector3f();
            Vector3f adjacentPoint = new Vector3f();
            Vector3f rootPoint = new Vector3f();
            Vector3f tempNorm = new Vector3f();
            int normalIndex = 0;

            for (int y = 0; y < getHeight(); y++) {
                for (int x = 0; x < getWidth(); x++) {
                    rootPoint.set(x, getValue(x,y), y);
                    if (y == getHeight() - 1) {
                        if (x == getWidth() - 1) {  // case #4 : last row, last col
                            // left cross up
//                            adj = normalIndex - getWidth();
//                            opp = normalIndex - 1;
                            adjacentPoint.set(x, getValue(x,y-1), y-1);
                            oppositePoint.set(x-1, getValue(x-1, y), y);
                        } else {                    // case #3 : last row, except for last col
                            // right cross up
//                            adj = normalIndex + 1;
//                            opp = normalIndex - getWidth();
                            adjacentPoint.set(x+1, getValue(x+1,y), y);
                            oppositePoint.set(x, getValue(x,y-1), y-1);
                        }
                    } else {
                        if (x == getWidth() - 1) {  // case #2 : last column except for last row
                            // left cross down
                            adjacentPoint.set(x-1, getValue(x-1,y), y);
                            oppositePoint.set(x, getValue(x,y+1), y+1);
//                            adj = normalIndex - 1;
//                            opp = normalIndex + getWidth();
                        } else {                    // case #1 : most cases
                            // right cross down
                            adjacentPoint.set(x, getValue(x,y+1), y+1);
                            oppositePoint.set(x+1, getValue(x+1,y), y);
//                            adj = normalIndex + getWidth();
//                            opp = normalIndex + 1;
                        }
                    }



                    tempNorm.set(adjacentPoint).subtractLocal(rootPoint)
                            .crossLocal(oppositePoint.subtractLocal(rootPoint));
                    tempNorm.multLocal(scale).normalizeLocal();
//                    store.put(tempNorm.x).put(tempNorm.y).put(tempNorm.z);
                    BufferUtils.setInBuffer(tempNorm, store,
                            normalIndex);
                    normalIndex++;
                }
            }
        }else{
            Vector3f temp = new Vector3f();
            for (int z = 0; z < getHeight(); z++){
                for (int x = 0; x < getWidth(); x++){
                    getNormal(x,z,temp);
                    store.put(temp.x).put(temp.y).put(temp.z);
                }
            }
        }

        return store;
    }
    
    /**
     * Creates a vertex array from the height data in this Geomap
     *
     * The scale argument specifies the scale to use for the vertex buffer.
     * For example, if scale is 10,1,10, then the greatest X value is getWidth()*10
     *
     * @param store A preallocated FloatBuffer where to store the data (optional), size must be >= getWidth()*getHeight()*3
     * @param scale Created vertexes are scaled by this vector
     *
     * @returns store, or a new FloatBuffer if store is null
     *
     * @throws NullPointerException If isLoaded() is false
     */
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
    
    public Vector2f getUV(int x, int y, Vector2f store){
        store.set( (float)x / (float)getWidth(),
                   (float)y / (float)getHeight() );
        return store;
    }

    public Vector2f getUV(int i, Vector2f store){
        return store;
    }
    
    public FloatBuffer writeTexCoordArray(FloatBuffer store, Vector2f offset, Vector2f scale){
        if (store!=null){
            if (store.remaining() < getWidth()*getHeight()*2)
                throw new BufferUnderflowException();
        }else{
            store = BufferUtils.createFloatBuffer(getWidth()*getHeight()*2);
        }

        if (offset == null)
            offset = new Vector2f();

        Vector2f tcStore = new Vector2f();
        for (int y = 0; y < getHeight(); y++){
            for (int x = 0; x < getWidth(); x++){
                getUV(x,y,tcStore);
                store.put( offset.x + tcStore.x * scale.x );
                store.put( offset.y + tcStore.y * scale.y );
            }

        }

        return store;
    }
    
    public IntBuffer writeIndexArray(IntBuffer store){
        int faceN = (getWidth()-1)*(getHeight()-1)*2;

        if (store!=null){
            if (store.remaining() < faceN*3)
                throw new BufferUnderflowException();
        }else{
            store = BufferUtils.createIntBuffer(faceN*3);
        }

        int i = 0;
        for (int z = 0; z < getHeight()-1; z++){
            for (int x = 0; x < getWidth()-1; x++){
                store.put(i).put(i+getWidth()).put(i+getWidth()+1);
                store.put(i+getWidth()+1).put(i+1).put(i);
                i++;

                // TODO: There's probably a better way to do this..
                if (x==getWidth()-2) i++;
            }
        }
        store.flip();

        return store;
    }
    
    public Mesh createMesh(Vector3f scale, Vector2f tcScale, boolean center){
        FloatBuffer pb = writeVertexArray(null, scale, center);
        FloatBuffer tb = writeTexCoordArray(null, Vector2f.ZERO, tcScale);
        FloatBuffer nb = writeNormalArray(null, scale);
        IntBuffer ib = writeIndexArray(null);
        Mesh m = new Mesh();
        m.setBuffer(Type.Position, 3, pb);
        m.setBuffer(Type.Normal, 3, nb);
        m.setBuffer(Type.TexCoord, 2, tb);
        m.setBuffer(Type.Index, 3, ib);
        m.setStatic();
        m.updateBound();
        return m;
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

    
}
