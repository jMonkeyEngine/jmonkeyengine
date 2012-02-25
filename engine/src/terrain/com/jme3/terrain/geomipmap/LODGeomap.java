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
package com.jme3.terrain.geomipmap;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Triangle;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.terrain.GeoMap;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Produces the mesh for the TerrainPatch.
 * This LOD algorithm generates a single triangle strip by first building the center of the
 * mesh, minus one outer edge around it. Then it builds the edges in counter-clockwise order,
 * starting at the bottom right and working up, then left across the top, then down across the
 * left, then right across the bottom.
 * It needs to know what its neighbour's LOD's are so it can stitch the edges.
 * It creates degenerate polygons in order to keep the winding order of the polygons and to move
 * the strip to a new position while still maintaining the continuity of the overall mesh. These
 * degenerates are removed quickly by the video card.
 *
 * @author Brent Owens
 */
public class LODGeomap extends GeoMap {

    public LODGeomap() {
    }

    @Deprecated
    public LODGeomap(int size, FloatBuffer heightMap) {
        super(heightMap, size, size, 1);
    }
    
    public LODGeomap(int size, float[] heightMap) {
        super(heightMap, size, size, 1);
    }

    public Mesh createMesh(Vector3f scale, Vector2f tcScale, Vector2f tcOffset, float offsetAmount, int totalSize, boolean center) {
        return this.createMesh(scale, tcScale, tcOffset, offsetAmount, totalSize, center, 1, false, false, false, false);
    }

    public Mesh createMesh(Vector3f scale, Vector2f tcScale, Vector2f tcOffset, float offsetAmount, int totalSize, boolean center, int lod, boolean rightLod, boolean topLod, boolean leftLod, boolean bottomLod) {
        FloatBuffer pb = writeVertexArray(null, scale, center);
        FloatBuffer texb = writeTexCoordArray(null, tcOffset, tcScale, offsetAmount, totalSize);
        FloatBuffer nb = writeNormalArray(null, scale);
        IntBuffer ib = writeIndexArrayLodDiff(null, lod, rightLod, topLod, leftLod, bottomLod);
        FloatBuffer bb = BufferUtils.createFloatBuffer(getWidth() * getHeight() * 3);
        FloatBuffer tanb = BufferUtils.createFloatBuffer(getWidth() * getHeight() * 3);
        writeTangentArray(nb, tanb, bb, texb, scale);
        Mesh m = new Mesh();
        m.setMode(Mode.TriangleStrip);
        m.setBuffer(Type.Position, 3, pb);
        m.setBuffer(Type.Normal, 3, nb);
        m.setBuffer(Type.Tangent, 3, tanb);
        m.setBuffer(Type.Binormal, 3, bb);
        m.setBuffer(Type.TexCoord, 2, texb);
        m.setBuffer(Type.Index, 3, ib);
        m.setStatic();
        m.updateBound();
        return m;
    }

    public FloatBuffer writeTexCoordArray(FloatBuffer store, Vector2f offset, Vector2f scale, float offsetAmount, int totalSize) {
        if (store != null) {
            if (store.remaining() < getWidth() * getHeight() * 2) {
                throw new BufferUnderflowException();
            }
        } else {
            store = BufferUtils.createFloatBuffer(getWidth() * getHeight() * 2);
        }

        if (offset == null) {
            offset = new Vector2f();
        }

        Vector2f tcStore = new Vector2f();

        // work from bottom of heightmap up, so we don't flip the coords
        for (int y = getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < getWidth(); x++) {
                getUV(x, y, tcStore, offset, offsetAmount, totalSize);
                float tx = tcStore.x * scale.x;
                float ty = tcStore.y * scale.y;
                store.put(tx);
                store.put(ty);
            }
        }

        return store;
    }

    public Vector2f getUV(int x, int y, Vector2f store, Vector2f offset, float offsetAmount, int totalSize) {
        float offsetX = offset.x + (offsetAmount * 1.0f);
        float offsetY = -offset.y + (offsetAmount * 1.0f);//note the -, we flip the tex coords

        store.set((((float) x) + offsetX) / (float) (totalSize - 1), // calculates percentage of texture here
                (((float) y) + offsetY) / (float) (totalSize - 1));
        return store;
    }

    /**
     * Create the LOD index array that will seam its edges with its neighbour's LOD.
     * This is a scary method!!! It will break your mind.
     *
     * @param store to store the index buffer
     * @param lod level of detail of the mesh
     * @param rightLod LOD of the right neighbour
     * @param topLod LOD of the top neighbour
     * @param leftLod LOD of the left neighbour
     * @param bottomLod LOD of the bottom neighbour
     * @return the LOD-ified index buffer
     */
    public IntBuffer writeIndexArrayLodDiff(IntBuffer store, int lod, boolean rightLod, boolean topLod, boolean leftLod, boolean bottomLod) {

        IntBuffer buffer2 = store;
        int numIndexes = calculateNumIndexesLodDiff(lod);
        if (store == null) {
            buffer2 = BufferUtils.createIntBuffer(numIndexes);
        }
        VerboseIntBuffer buffer = new VerboseIntBuffer(buffer2);


        // generate center squares minus the edges
        //System.out.println("for (x="+lod+"; x<"+(getWidth()-(2*lod))+"; x+="+lod+")");
        //System.out.println("	for (z="+lod+"; z<"+(getWidth()-(1*lod))+"; z+="+lod+")");
        for (int r = lod; r < getWidth() - (2 * lod); r += lod) { // row
            int rowIdx = r * getWidth();
            int nextRowIdx = (r + 1 * lod) * getWidth();
            for (int c = lod; c < getWidth() - (1 * lod); c += lod) { // column
                int idx = rowIdx + c;
                buffer.put(idx);
                idx = nextRowIdx + c;
                buffer.put(idx);
            }

            // add degenerate triangles
            if (r < getWidth() - (3 * lod)) {
                int idx = nextRowIdx + getWidth() - (1 * lod) - 1;
                buffer.put(idx);
                idx = nextRowIdx + (1 * lod); // inset by 1
                buffer.put(idx);
                //System.out.println("");
            }
        }
        //System.out.println("\nright:");

        //int runningBufferCount = buffer.getCount();
        //System.out.println("buffer start: "+runningBufferCount);


        // right
        int br = getWidth() * (getWidth() - lod) - 1 - lod;
        buffer.put(br); // bottom right -1
        int corner = getWidth() * getWidth() - 1;
        buffer.put(corner);	// bottom right corner
        if (rightLod) { // if lower LOD
            for (int row = getWidth() - lod; row >= 1 + lod; row -= 2 * lod) {
                int idx = (row) * getWidth() - 1 - lod;
                buffer.put(idx);
                idx = (row - lod) * getWidth() - 1;
                buffer.put(idx);
                if (row > lod + 1) { //if not the last one
                    idx = (row - lod) * getWidth() - 1 - lod;
                    buffer.put(idx);
                    idx = (row - lod) * getWidth() - 1;
                    buffer.put(idx);
                } else {
                }
            }
        } else {
            buffer.put(corner);//br+1);//degenerate to flip winding order
            for (int row = getWidth() - lod; row > lod; row -= lod) {
                int idx = row * getWidth() - 1; // mult to get row
                buffer.put(idx);
                buffer.put(idx - lod);
            }

        }

        buffer.put(getWidth() - 1);


        //System.out.println("\nbuffer right: "+(buffer.getCount()-runningBufferCount));
        //runningBufferCount = buffer.getCount();


        //System.out.println("\ntop:");

        // top 			(the order gets reversed here so the diagonals line up)
        if (topLod) { // if lower LOD
            if (rightLod) {
                buffer.put(getWidth() - 1);
            }
            for (int col = getWidth() - 1; col >= lod; col -= 2 * lod) {
                int idx = (lod * getWidth()) + col - lod; // next row
                buffer.put(idx);
                idx = col - 2 * lod;
                buffer.put(idx);
                if (col > lod * 2) { //if not the last one
                    idx = (lod * getWidth()) + col - 2 * lod;
                    buffer.put(idx);
                    idx = col - 2 * lod;
                    buffer.put(idx);
                } else {
                }
            }
        } else {
            if (rightLod) {
                buffer.put(getWidth() - 1);
            }
            for (int col = getWidth() - 1 - lod; col > 0; col -= lod) {
                int idx = col + (lod * getWidth());
                buffer.put(idx);
                idx = col;
                buffer.put(idx);
            }
            buffer.put(0);
        }
        buffer.put(0);

        //System.out.println("\nbuffer top: "+(buffer.getCount()-runningBufferCount));
        //runningBufferCount = buffer.getCount();

        //System.out.println("\nleft:");

        // left
        if (leftLod) { // if lower LOD
            if (topLod) {
                buffer.put(0);
            }
            for (int row = 0; row < getWidth() - lod; row += 2 * lod) {
                int idx = (row + lod) * getWidth() + lod;
                buffer.put(idx);
                idx = (row + 2 * lod) * getWidth();
                buffer.put(idx);
                if (row < getWidth() - lod - 2 - 1) { //if not the last one
                    idx = (row + 2 * lod) * getWidth() + lod;
                    buffer.put(idx);
                    idx = (row + 2 * lod) * getWidth();
                    buffer.put(idx);
                } else {
                }
            }
        } else {
            if (!topLod) {
                buffer.put(0);
            }
            //buffer.put(getWidth()+1); // degenerate
            //buffer.put(0); // degenerate winding-flip
            for (int row = lod; row < getWidth() - lod; row += lod) {
                int idx = row * getWidth();
                buffer.put(idx);
                idx = row * getWidth() + lod;
                buffer.put(idx);
            }

        }
        buffer.put(getWidth() * (getWidth() - 1));


        //System.out.println("\nbuffer left: "+(buffer.getCount()-runningBufferCount));
        //runningBufferCount = buffer.getCount();

        //if (true) return buffer.delegate;
        //System.out.println("\nbottom");

        // bottom
        if (bottomLod) { // if lower LOD
            if (leftLod) {
                buffer.put(getWidth() * (getWidth() - 1));
            }
            // there was a slight bug here when really high LOD near maxLod
            // far right has extra index one row up and all the way to the right, need to skip last index entered
            // seemed to be fixed by making "getWidth()-1-2-lod" this: "getWidth()-1-2*lod", which seems more correct
            for (int col = 0; col < getWidth() - lod; col += 2 * lod) {
                int idx = getWidth() * (getWidth() - 1 - lod) + col + lod;
                buffer.put(idx);
                idx = getWidth() * (getWidth() - 1) + col + 2 * lod;
                buffer.put(idx);
                if (col < getWidth() - 1 - 2 * lod) { //if not the last one
                    idx = getWidth() * (getWidth() - 1 - lod) + col + 2 * lod;
                    buffer.put(idx);
                    idx = getWidth() * (getWidth() - 1) + col + 2 * lod;
                    buffer.put(idx);
                } else {
                }
            }
        } else {
            if (leftLod) {
                buffer.put(getWidth() * (getWidth() - 1));
            }
            for (int col = lod; col < getWidth() - lod; col += lod) {
                int idx = getWidth() * (getWidth() - 1 - lod) + col; // up
                buffer.put(idx);
                idx = getWidth() * (getWidth() - 1) + col; // down
                buffer.put(idx);
            }
            //buffer.put(getWidth()*getWidth()-1-lod); // <-- THIS caused holes at the end!
        }

        buffer.put(getWidth() * getWidth() - 1);

        //System.out.println("\nbuffer bottom: "+(buffer.getCount()-runningBufferCount));
        //runningBufferCount = buffer.getCount();

        //System.out.println("\nBuffer size: "+buffer.getCount());

        // fill in the rest of the buffer with degenerates, there should only be a couple
        for (int i = buffer.getCount(); i < numIndexes; i++) {
            buffer.put(getWidth() * getWidth() - 1);
        }

        return buffer.delegate;
    }

    public IntBuffer writeIndexArrayLodVariable(IntBuffer store, int lod, int rightLod, int topLod, int leftLod, int bottomLod) {

        IntBuffer buffer2 = store;
        int numIndexes = calculateNumIndexesLodDiff(lod);
        if (store == null) {
            buffer2 = BufferUtils.createIntBuffer(numIndexes);
        }
        VerboseIntBuffer buffer = new VerboseIntBuffer(buffer2);


        // generate center squares minus the edges
        //System.out.println("for (x="+lod+"; x<"+(getWidth()-(2*lod))+"; x+="+lod+")");
        //System.out.println("	for (z="+lod+"; z<"+(getWidth()-(1*lod))+"; z+="+lod+")");
        for (int r = lod; r < getWidth() - (2 * lod); r += lod) { // row
            int rowIdx = r * getWidth();
            int nextRowIdx = (r + 1 * lod) * getWidth();
            for (int c = lod; c < getWidth() - (1 * lod); c += lod) { // column
                int idx = rowIdx + c;
                buffer.put(idx);
                idx = nextRowIdx + c;
                buffer.put(idx);
            }

            // add degenerate triangles
            if (r < getWidth() - (3 * lod)) {
                int idx = nextRowIdx + getWidth() - (1 * lod) - 1;
                buffer.put(idx);
                idx = nextRowIdx + (1 * lod); // inset by 1
                buffer.put(idx);
                //System.out.println("");
            }
        }
        //System.out.println("\nright:");

        //int runningBufferCount = buffer.getCount();
        //System.out.println("buffer start: "+runningBufferCount);


        // right
        int br = getWidth() * (getWidth() - lod) - 1 - lod;
        buffer.put(br); // bottom right -1
        int corner = getWidth() * getWidth() - 1;
        buffer.put(corner);	// bottom right corner
        if (rightLod > lod) { // if lower LOD
            int idx = corner;
            int it = (getWidth() - 1) / rightLod; // iterations
            int lodDiff = rightLod / lod;
            for (int i = it; i > 0; i--) { // for each lod level of the neighbour
                idx = getWidth() * (i * rightLod + 1) - 1;
                for (int j = 1; j <= lodDiff; j++) { // for each section in that lod level
                    int idxB = idx - (getWidth() * (j * lod)) - lod;

                    if (j == lodDiff && i == 1) {// the last one
                        buffer.put(getWidth() - 1);
                    } else if (j == lodDiff) {
                        buffer.put(idxB);
                        buffer.put(idxB + lod);
                    } else {
                        buffer.put(idxB);
                        buffer.put(idx);
                    }
                }
            }
            // reset winding order
            buffer.put(getWidth() * (lod + 1) - lod - 1); // top-right +1row
            buffer.put(getWidth() - 1);// top-right

        } else {
            buffer.put(corner);//br+1);//degenerate to flip winding order
            for (int row = getWidth() - lod; row > lod; row -= lod) {
                int idx = row * getWidth() - 1; // mult to get row
                buffer.put(idx);
                buffer.put(idx - lod);
            }
            buffer.put(getWidth() - 1);
        }


        //System.out.println("\nbuffer right: "+(buffer.getCount()-runningBufferCount));
        //runningBufferCount = buffer.getCount();


        //System.out.println("\ntop:");

        // top 			(the order gets reversed here so the diagonals line up)
        if (topLod > lod) { // if lower LOD
            if (rightLod > lod) {
                // need to flip winding order
                buffer.put(getWidth() - 1);
                buffer.put(getWidth() * lod - 1);
                buffer.put(getWidth() - 1);
            }
            int idx = getWidth() - 1;
            int it = (getWidth() - 1) / topLod; // iterations
            int lodDiff = topLod / lod;
            for (int i = it; i > 0; i--) { // for each lod level of the neighbour
                idx = (i * topLod);
                for (int j = 1; j <= lodDiff; j++) { // for each section in that lod level
                    int idxB = lod * getWidth() + (i * topLod) - (j * lod);

                    if (j == lodDiff && i == 1) {// the last one
                        buffer.put(0);
                    } else if (j == lodDiff) {
                        buffer.put(idxB);
                        buffer.put(idx - topLod);
                    } else {
                        buffer.put(idxB);
                        buffer.put(idx);
                    }
                }
            }
        } else {
            if (rightLod > lod) {
                buffer.put(getWidth() - 1);
            }
            for (int col = getWidth() - 1 - lod; col > 0; col -= lod) {
                int idx = col + (lod * getWidth());
                buffer.put(idx);
                idx = col;
                buffer.put(idx);
            }
            buffer.put(0);
        }
        buffer.put(0);

        //System.out.println("\nbuffer top: "+(buffer.getCount()-runningBufferCount));
        //runningBufferCount = buffer.getCount();

        //System.out.println("\nleft:");

        // left
        if (leftLod > lod) { // if lower LOD

            int idx = 0;
            int it = (getWidth() - 1) / leftLod; // iterations
            int lodDiff = leftLod / lod;
            for (int i = 0; i < it; i++) { // for each lod level of the neighbour
                idx = getWidth() * (i * leftLod);
                for (int j = 1; j <= lodDiff; j++) { // for each section in that lod level
                    int idxB = idx + (getWidth() * (j * lod)) + lod;

                    if (j == lodDiff && i == it - 1) {// the last one
                        buffer.put(getWidth() * getWidth() - getWidth());
                    } else if (j == lodDiff) {
                        buffer.put(idxB);
                        buffer.put(idxB - lod);
                    } else {
                        buffer.put(idxB);
                        buffer.put(idx);
                    }
                }
            }

        } else {
            buffer.put(0);
            buffer.put(getWidth() * lod + lod);
            buffer.put(0);
            for (int row = lod; row < getWidth() - lod; row += lod) {
                int idx = row * getWidth();
                buffer.put(idx);
                idx = row * getWidth() + lod;
                buffer.put(idx);
            }
            buffer.put(getWidth() * (getWidth() - 1));
        }
        //buffer.put(getWidth()*(getWidth()-1));


        //System.out.println("\nbuffer left: "+(buffer.getCount()-runningBufferCount));
        //runningBufferCount = buffer.getCount();

        //if (true) return buffer.delegate;
        //System.out.println("\nbottom");

        // bottom
        if (bottomLod > lod) { // if lower LOD
            if (leftLod > lod) {
                buffer.put(getWidth() * (getWidth() - 1));
                buffer.put(getWidth() * (getWidth() - lod));
                buffer.put(getWidth() * (getWidth() - 1));
            }

            int idx = getWidth() * getWidth() - getWidth();
            int it = (getWidth() - 1) / bottomLod; // iterations
            int lodDiff = bottomLod / lod;
            for (int i = 0; i < it; i++) { // for each lod level of the neighbour
                idx = getWidth() * getWidth() - getWidth() + (i * bottomLod);
                for (int j = 1; j <= lodDiff; j++) { // for each section in that lod level
                    int idxB = idx - (getWidth() * lod) + j * lod;

                    if (j == lodDiff && i == it - 1) {// the last one
                        buffer.put(getWidth() * getWidth() - 1);
                    } else if (j == lodDiff) {
                        buffer.put(idxB);
                        buffer.put(idx + bottomLod);
                    } else {
                        buffer.put(idxB);
                        buffer.put(idx);
                    }
                }
            }
        } else {
            if (leftLod > lod) {
                buffer.put(getWidth() * (getWidth() - 1));
                buffer.put(getWidth() * getWidth() - (getWidth() * lod) + lod);
                buffer.put(getWidth() * (getWidth() - 1));
            }
            for (int col = lod; col < getWidth() - lod; col += lod) {
                int idx = getWidth() * (getWidth() - 1 - lod) + col; // up
                buffer.put(idx);
                idx = getWidth() * (getWidth() - 1) + col; // down
                buffer.put(idx);
            }
            //buffer.put(getWidth()*getWidth()-1-lod); // <-- THIS caused holes at the end!
        }

        buffer.put(getWidth() * getWidth() - 1);

        //System.out.println("\nbuffer bottom: "+(buffer.getCount()-runningBufferCount));
        //runningBufferCount = buffer.getCount();

        //System.out.println("\nBuffer size: "+buffer.getCount());

        // fill in the rest of the buffer with degenerates, there should only be a couple
        for (int i = buffer.getCount(); i < numIndexes; i++) {
            buffer.put(getWidth() * getWidth() - 1);
        }

        return buffer.delegate;
    }


    /*private int calculateNumIndexesNormal(int lod) {
    int length = getWidth()-1;
    int num = ((length/lod)+1)*((length/lod)+1)*2;
    System.out.println("num: "+num);
    num -= 2*((length/lod)+1);
    System.out.println("num2: "+num);
    // now get the degenerate indexes that exist between strip rows
    num += 2*(((length/lod)+1)-2); // every row except the first and last
    System.out.println("Index buffer size: "+num);
    return num;
    }*/
    /**
     * calculate how many indexes there will be.
     * This isn't that precise and there might be a couple extra.
     */
    private int calculateNumIndexesLodDiff(int lod) {
        if (lod == 0) {
            lod = 1;
        }
        int length = getWidth() - 1; // make it even for lod calc
        int side = (length / lod) + 1 - (2);
        //System.out.println("side: "+side);
        int num = side * side * 2;
        //System.out.println("num: "+num);
        num -= 2 * side;	// remove one first row and one last row (they are only hit once each)
        //System.out.println("num2: "+num);
        // now get the degenerate indexes that exist between strip rows
        int degenerates = 2 * (side - (2)); // every row except the first and last
        num += degenerates;
        //System.out.println("degenerates: "+degenerates);

        //System.out.println("center, before edges: "+num);

        num += (getWidth() / lod) * 2 * 4;
        num++;

        num += 10;// TODO remove me: extra
        //System.out.println("Index buffer size: "+num);
        return num;
    }

    public FloatBuffer[] writeTangentArray(FloatBuffer normalBuffer, FloatBuffer tangentStore, FloatBuffer binormalStore, FloatBuffer textureBuffer, Vector3f scale) {
        if (!isLoaded()) {
            throw new NullPointerException();
        }

        if (tangentStore != null) {
            if (tangentStore.remaining() < getWidth() * getHeight() * 3) {
                throw new BufferUnderflowException();
            }
        } else {
            tangentStore = BufferUtils.createFloatBuffer(getWidth() * getHeight() * 3);
        }
        tangentStore.rewind();

        if (binormalStore != null) {
            if (binormalStore.remaining() < getWidth() * getHeight() * 3) {
                throw new BufferUnderflowException();
            }
        } else {
            binormalStore = BufferUtils.createFloatBuffer(getWidth() * getHeight() * 3);
        }
        binormalStore.rewind();

        Vector3f normal = new Vector3f();
        Vector3f tangent = new Vector3f();
        Vector3f binormal = new Vector3f();
        /*Vector3f v1 = new Vector3f();
        Vector3f v2 = new Vector3f();
        Vector3f v3 = new Vector3f();
        Vector2f t1 = new Vector2f();
        Vector2f t2 = new Vector2f();
        Vector2f t3 = new Vector2f();*/

        for (int r = 0; r < getHeight(); r++) {
            for (int c = 0; c < getWidth(); c++) {
                
                int idx = (r * getWidth() + c) * 3;
                normal.set(normalBuffer.get(idx), normalBuffer.get(idx+1), normalBuffer.get(idx+2));
                tangent.set(normal.cross(new Vector3f(0,0,1)));
                binormal.set(new Vector3f(1,0,0).cross(normal));
                
                BufferUtils.setInBuffer(tangent.normalizeLocal(), tangentStore, (r * getWidth() + c)); // save the tangent
                BufferUtils.setInBuffer(binormal.normalizeLocal(), binormalStore, (r * getWidth() + c)); // save the binormal
            }
        }

/*        for (int r = 0; r < getHeight(); r++) {
            for (int c = 0; c < getWidth(); c++) {

                int texIdx = ((getHeight() - 1 - r) * getWidth() + c) * 2; // pull from the end
                int texIdxAbove = ((getHeight() - 1 - (r - 1)) * getWidth() + c) * 2; // pull from the end
                int texIdxNext = ((getHeight() - 1 - (r + 1)) * getWidth() + c) * 2; // pull from the end

                v1.set(c, getValue(c, r), r);
                t1.set(textureBuffer.get(texIdx), textureBuffer.get(texIdx + 1));

                // below
                if (r == getHeight()-1) { // last row
                    v3.set(c, getValue(c, r), r + 1);
                    float u = textureBuffer.get(texIdx) - textureBuffer.get(texIdxAbove);
                    u += textureBuffer.get(texIdx);
                    float v = textureBuffer.get(texIdx + 1) - textureBuffer.get(texIdxAbove + 1);
                    v += textureBuffer.get(texIdx + 1);
                    t3.set(u, v);
                } else {
                    v3.set(c, getValue(c, r + 1), r + 1);
                    t3.set(textureBuffer.get(texIdxNext), textureBuffer.get(texIdxNext + 1));
                }
                
                //right
                if (c == getWidth()-1) { // last column
                    v2.set(c + 1, getValue(c, r), r);
                    float u = textureBuffer.get(texIdx) - textureBuffer.get(texIdx - 2);
                    u += textureBuffer.get(texIdx);
                    float v = textureBuffer.get(texIdx + 1) - textureBuffer.get(texIdx - 1);
                    v += textureBuffer.get(texIdx - 1);
                    t2.set(u, v);
                } else {
                    v2.set(c + 1, getValue(c + 1, r), r); // one to the right
                    t2.set(textureBuffer.get(texIdx + 2), textureBuffer.get(texIdx + 3));
                }

                calculateTangent(new Vector3f[]{v1.mult(scale), v2.mult(scale), v3.mult(scale)}, new Vector2f[]{t1, t2, t3}, tangent, binormal);
                BufferUtils.setInBuffer(tangent, tangentStore, (r * getWidth() + c)); // save the tangent
                BufferUtils.setInBuffer(binormal, binormalStore, (r * getWidth() + c)); // save the binormal
            }
        }
        */
        return new FloatBuffer[]{tangentStore, binormalStore};
    }

    /**
     * 
     * @param v Takes 3 vertices: root, right, bottom
     * @param t Takes 3 tex coords: root, right, bottom
     * @param tangent that will store the result
     * @return the tangent store
     */
    public static Vector3f calculateTangent(Vector3f[] v, Vector2f[] t, Vector3f tangent, Vector3f binormal) {
        Vector3f edge1 = new Vector3f(); // y=0
        Vector3f edge2 = new Vector3f(); // x=0
        Vector2f edge1uv = new Vector2f(); // y=0
        Vector2f edge2uv = new Vector2f(); // x=0

        t[2].subtract(t[0], edge2uv);
        t[1].subtract(t[0], edge1uv);

        float det = edge1uv.x * edge2uv.y;// - edge1uv.y*edge2uv.x;  = 0

        boolean normalize = true;
        if (Math.abs(det) < 0.0000001f) {
            det = 1;
            normalize = true;
        }

        v[1].subtract(v[0], edge1);
        v[2].subtract(v[0], edge2);

        tangent.set(edge1);
        tangent.normalizeLocal();
        binormal.set(edge2);
        binormal.normalizeLocal();

        float factor = 1 / det;
        tangent.x = (edge2uv.y * edge1.x) * factor;
        tangent.y = 0;
        tangent.z = (edge2uv.y * edge1.z) * factor;
        if (normalize) {
            tangent.normalizeLocal();
        }

        binormal.x = 0;
        binormal.y = (edge1uv.x * edge2.y) * factor;
        binormal.z = (edge1uv.x * edge2.z) * factor;
        if (normalize) {
            binormal.normalizeLocal();
        }

        return tangent;
    }

    @Override
    public FloatBuffer writeNormalArray(FloatBuffer store, Vector3f scale) {
        if (!isLoaded()) {
            throw new NullPointerException();
        }

        if (store != null) {
            if (store.remaining() < getWidth() * getHeight() * 3) {
                throw new BufferUnderflowException();
            }
        } else {
            store = BufferUtils.createFloatBuffer(getWidth() * getHeight() * 3);
        }
        store.rewind();

        TempVars vars = TempVars.get();
        
        Vector3f rootPoint = vars.vect1;
        Vector3f rightPoint = vars.vect2;
        Vector3f leftPoint = vars.vect3;
        Vector3f topPoint = vars.vect4;
        Vector3f bottomPoint = vars.vect5;
        
        Vector3f tmp1 = vars.vect6;

        // calculate normals for each polygon
        for (int r = 0; r < getHeight(); r++) {
            for (int c = 0; c < getWidth(); c++) {

                rootPoint.set(c, getValue(c, r), r);
                Vector3f normal = vars.vect8;

                if (r == 0) { // first row
                    if (c == 0) { // first column
                        rightPoint.set(c + 1, getValue(c + 1, r), r);
                        bottomPoint.set(c, getValue(c, r + 1), r + 1);
                        getNormal(bottomPoint, rootPoint, rightPoint, scale, normal);
                    } else if (c == getWidth() - 1) { // last column
                        leftPoint.set(c - 1, getValue(c - 1, r), r);
                        bottomPoint.set(c, getValue(c, r + 1), r + 1);
                        getNormal(leftPoint, rootPoint, bottomPoint, scale, normal);
                    } else { // all middle columns
                        leftPoint.set(c - 1, getValue(c - 1, r), r);
                        rightPoint.set(c + 1, getValue(c + 1, r), r);
                        bottomPoint.set(c, getValue(c, r + 1), r + 1);
                        
                        normal.set( getNormal(leftPoint, rootPoint, bottomPoint, scale, tmp1) );
                        normal.add( getNormal(bottomPoint, rootPoint, rightPoint, scale, tmp1) );
                        normal.normalizeLocal();
                    }
                } else if (r == getHeight() - 1) { // last row
                    if (c == 0) { // first column
                        topPoint.set(c, getValue(c, r - 1), r - 1);
                        rightPoint.set(c + 1, getValue(c + 1, r), r);
                        getNormal(rightPoint, rootPoint, topPoint, scale, normal);
                    } else if (c == getWidth() - 1) { // last column
                        topPoint.set(c, getValue(c, r - 1), r - 1);
                        leftPoint.set(c - 1, getValue(c - 1, r), r);
                        getNormal(topPoint, rootPoint, leftPoint, scale, normal);
                    } else { // all middle columns
                        topPoint.set(c, getValue(c, r - 1), r - 1);
                        leftPoint.set(c - 1, getValue(c - 1, r), r);
                        rightPoint.set(c + 1, getValue(c + 1, r), r);
                        
                        normal.set( getNormal(topPoint, rootPoint, leftPoint, scale, tmp1) );
                        normal.add( getNormal(rightPoint, rootPoint, topPoint, scale, tmp1) );
                        normal.normalizeLocal();
                    }
                } else { // all middle rows
                    if (c == 0) { // first column
                        topPoint.set(c, getValue(c, r - 1), r - 1);
                        rightPoint.set(c + 1, getValue(c + 1, r), r);
                        bottomPoint.set(c, getValue(c, r + 1), r + 1);
                        
                        normal.set( getNormal(rightPoint, rootPoint, topPoint, scale, tmp1) );
                        normal.add( getNormal(bottomPoint, rootPoint, rightPoint, scale, tmp1) );
                        normal.normalizeLocal();
                    } else if (c == getWidth() - 1) { // last column
                        topPoint.set(c, getValue(c, r - 1), r - 1);
                        leftPoint.set(c - 1, getValue(c - 1, r), r);
                        bottomPoint.set(c, getValue(c, r + 1), r + 1); //XXX wrong

                        normal.set( getNormal(topPoint, rootPoint, leftPoint, scale, tmp1) );
                        normal.add( getNormal(leftPoint, rootPoint, bottomPoint, scale, tmp1) );
                        normal.normalizeLocal();
                    } else { // all middle columns
                        topPoint.set(c, getValue(c, r - 1), r - 1);
                        leftPoint.set(c - 1, getValue(c - 1, r), r);
                        rightPoint.set(c + 1, getValue(c + 1, r), r);
                        bottomPoint.set(c, getValue(c, r + 1), r + 1);
                        
                        normal.set( getNormal(topPoint,  rootPoint, leftPoint, scale, tmp1 ) );
                        normal.add( getNormal(leftPoint, rootPoint, bottomPoint, scale, tmp1) );
                        normal.add( getNormal(bottomPoint, rootPoint, rightPoint, scale, tmp1) );
                        normal.add( getNormal(rightPoint, rootPoint, topPoint, scale, tmp1) );
                        normal.normalizeLocal();
                    }
                }
                
                BufferUtils.setInBuffer(normal, store, (r * getWidth() + c)); // save the normal
            }
        }
        vars.release();
        
        return store;
    }

    private Vector3f getNormal(Vector3f firstPoint, Vector3f rootPoint, Vector3f secondPoint, Vector3f scale, Vector3f store) {
        float x1 = firstPoint.x - rootPoint.x;
        float y1 = firstPoint.y - rootPoint.y;
        float z1 = firstPoint.z - rootPoint.z;
        x1 *= scale.x;
        y1 *= scale.y;
        z1 *= scale.z;
        float x2 = secondPoint.x - rootPoint.x;
        float y2 = secondPoint.y - rootPoint.y;
        float z2 = secondPoint.z - rootPoint.z;
        x2 *= scale.x;
        y2 *= scale.y;
        z2 *= scale.z;
        float x3 = (y1 * z2) - (z1 * y2);
        float y3 = (z1 * x2) - (x1 * z2);
        float z3 = (x1 * y2) - (y1 * x2);
        
        float inv = 1.0f / FastMath.sqrt(x3 * x3 + y3 * y3 + z3 * z3);
        store.x = x3 * inv;
        store.y = y3 * inv;
        store.z = z3 * inv;
        return store;
        
        /*store.set( firstPoint.subtractLocal(rootPoint).multLocal(scale).crossLocal(secondPoint.subtractLocal(rootPoint).multLocal(scale)).normalizeLocal() );
        return store;*/
        
    }

    /**
     * Keeps a count of the number of indexes, good for debugging
     */
    public class VerboseIntBuffer {

        private IntBuffer delegate;
        int count = 0;

        public VerboseIntBuffer(IntBuffer d) {
            delegate = d;
        }

        public void put(int value) {
            try {
                delegate.put(value);
                count++;
            } catch (BufferOverflowException e) {
                //System.out.println("err buffer size: "+delegate.capacity());
            }
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * Get a representation of the underlying triangle at the given point,
     * translated to world coordinates.
     * 
     * @param x local x coordinate
     * @param z local z coordinate
     * @return a triangle in world space not local space
     */
    protected Triangle getTriangleAtPoint(float x, float z, Vector3f scale, Vector3f translation) {
        Triangle tri = getTriangleAtPoint(x, z);
        if (tri != null) {
            tri.get1().multLocal(scale).addLocal(translation);
            tri.get2().multLocal(scale).addLocal(translation);
            tri.get3().multLocal(scale).addLocal(translation);
        }
        return tri;
    }

    /**
     * Get the two triangles that make up the grid section at the specified point,
     * translated to world coordinates.
     *
     * @param x local x coordinate
     * @param z local z coordinate
     * @param scale
     * @param translation
     * @return two triangles in world space not local space
     */
    protected Triangle[] getGridTrianglesAtPoint(float x, float z, Vector3f scale, Vector3f translation) {
        Triangle[] tris = getGridTrianglesAtPoint(x, z);
        if (tris != null) {
            tris[0].get1().multLocal(scale).addLocal(translation);
            tris[0].get2().multLocal(scale).addLocal(translation);
            tris[0].get3().multLocal(scale).addLocal(translation);
            tris[1].get1().multLocal(scale).addLocal(translation);
            tris[1].get2().multLocal(scale).addLocal(translation);
            tris[1].get3().multLocal(scale).addLocal(translation);
        }
        return tris;
    }

    /**
     * Get the two triangles that make up the grid section at the specified point.
     *
     * For every grid space there are two triangles oriented like this:
     *  *----*
     *  |a / |
     *  | / b|
     *  *----*
     * The corners of the mesh have differently oriented triangles. The two
     * corners that we have to special-case are the top left and bottom right
     * corners. They are oriented inversely:
     *  *----*
     *  | \ b|
     *  |a \ |
     *  *----*
     *
     * @param x local x coordinate
     * @param z local z coordinate
     * @param scale
     * @param translation
     * @return
     */
    protected Triangle[] getGridTrianglesAtPoint(float x, float z) {
        int gridX = (int) x;
        int gridY = (int) z;

        int index = findClosestHeightIndex(gridX, gridY);
        if (index < 0) {
            return null;
        }

        Triangle t = new Triangle(new Vector3f(), new Vector3f(), new Vector3f());
        Triangle t2 = new Triangle(new Vector3f(), new Vector3f(), new Vector3f());

        float h1 = hdata[index];                // top left
        float h2 = hdata[index + 1];            // top right
        float h3 = hdata[index + width];        // bottom left
        float h4 = hdata[index + width + 1];    // bottom right


        if ((gridX == 0 && gridY == 0) || (gridX == width - 1 && gridY == width - 1)) {
            // top left or bottom right grid point
            t.get(0).x = (gridX);
            t.get(0).y = (h1);
            t.get(0).z = (gridY);

            t.get(1).x = (gridX);
            t.get(1).y = (h3);
            t.get(1).z = (gridY + 1);

            t.get(2).x = (gridX + 1);
            t.get(2).y = (h4);
            t.get(2).z = (gridY + 1);

            t2.get(0).x = (gridX);
            t2.get(0).y = (h1);
            t2.get(0).z = (gridY);

            t2.get(1).x = (gridX + 1);
            t2.get(1).y = (h4);
            t2.get(1).z = (gridY + 1);

            t2.get(2).x = (gridX + 1);
            t2.get(2).y = (h2);
            t2.get(2).z = (gridY);
        } else {
            // all other grid points
            t.get(0).x = (gridX);
            t.get(0).y = (h1);
            t.get(0).z = (gridY);

            t.get(1).x = (gridX);
            t.get(1).y = (h3);
            t.get(1).z = (gridY + 1);

            t.get(2).x = (gridX + 1);
            t.get(2).y = (h2);
            t.get(2).z = (gridY);

            t2.get(0).x = (gridX + 1);
            t2.get(0).y = (h2);
            t2.get(0).z = (gridY);

            t2.get(1).x = (gridX);
            t2.get(1).y = (h3);
            t2.get(1).z = (gridY + 1);

            t2.get(2).x = (gridX + 1);
            t2.get(2).y = (h4);
            t2.get(2).z = (gridY + 1);
        }

        return new Triangle[]{t, t2};
    }

    /**
     * Get the triangle that the point is on.
     * 
     * @param x coordinate in local space to the geomap
     * @param z coordinate in local space to the geomap
     * @return triangle in local space to the geomap
     */
    protected Triangle getTriangleAtPoint(float x, float z) {
        Triangle[] triangles = getGridTrianglesAtPoint(x, z);
        if (triangles == null) {
            //System.out.println("x,z: " + x + "," + z);
            return null;
        }
        Vector2f point = new Vector2f(x, z);
        Vector2f t1 = new Vector2f(triangles[0].get1().x, triangles[0].get1().z);
        Vector2f t2 = new Vector2f(triangles[0].get2().x, triangles[0].get2().z);
        Vector2f t3 = new Vector2f(triangles[0].get3().x, triangles[0].get3().z);

        if (0 != FastMath.pointInsideTriangle(t1, t2, t3, point)) {
            return triangles[0];
        }

        t1.set(triangles[1].get1().x, triangles[1].get1().z);
        t1.set(triangles[1].get2().x, triangles[1].get2().z);
        t1.set(triangles[1].get3().x, triangles[1].get3().z);

        if (0 != FastMath.pointInsideTriangle(t1, t2, t3, point)) {
            return triangles[1];
        }

        return null;
    }

    protected int findClosestHeightIndex(int x, int z) {

        if (x < 0 || x >= width - 1) {
            return -1;
        }
        if (z < 0 || z >= width - 1) {
            return -1;
        }

        return z * width + x;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
    }
}
