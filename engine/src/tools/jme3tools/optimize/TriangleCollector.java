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

package jme3tools.optimize;

import com.jme3.light.Light;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.util.*;

public class TriangleCollector {

    private static final GeomTriComparator comparator = new GeomTriComparator();

    private static class GeomTriComparator implements Comparator<OCTTriangle> {
        public int compare(OCTTriangle a, OCTTriangle b) {
            if (a.getGeometryIndex() < b.getGeometryIndex()){
                return -1;
            }else if (a.getGeometryIndex() > b.getGeometryIndex()){
                return 1;
            }else{
                return 0;
            }
        }
    }

    private static class Range {
        
        private int start, length;

        public Range(int start, int length) {
            this.start = start;
            this.length = length;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

    }

    /**
     * Grabs all the triangles specified in <code>tris</code> from the input array
     * (using the indices OCTTriangle.getGeometryIndex() & OCTTriangle.getTriangleIndex())
     * then organizes them into output geometry.
     *
     * @param inGeoms
     * @param tris
     * @return
     */
    public static final List<Geometry> gatherTris(Geometry[] inGeoms, List<OCTTriangle> tris){
        Collections.sort(tris, comparator);
        HashMap<Integer, Range> ranges = new HashMap<Integer, Range>();

        for (int i = 0; i < tris.size(); i++){
            Range r = ranges.get(tris.get(i).getGeometryIndex());
            if (r != null){
                // incremenet length
                r.setLength(r.getLength()+1);
            }else{
                // set offset, length is 1
                ranges.put(tris.get(i).getGeometryIndex(), new Range(i, 1));
            }
        }
        
        List<Geometry> newGeoms = new ArrayList<Geometry>();
        int[] vertIndicies = new int[3];
        int[] newIndices = new int[3];
        boolean[] vertexCreated = new boolean[3];
        HashMap<Integer, Integer> indexCache = new HashMap<Integer, Integer>();
        for (Map.Entry<Integer, Range> entry : ranges.entrySet()){
            int inGeomIndex = entry.getKey().intValue();
            int outOffset = entry.getValue().start;
            int outLength = entry.getValue().length;

            Geometry inGeom = inGeoms[inGeomIndex];
            Mesh in = inGeom.getMesh();
            Mesh out = new Mesh();

            int outElementCount = outLength * 3;
            ShortBuffer ib = BufferUtils.createShortBuffer(outElementCount);
            out.setBuffer(Type.Index, 3, ib);

            // generate output buffers based on input buffers
            IntMap<VertexBuffer> bufs = in.getBuffers();
            for (Entry<VertexBuffer> ent : bufs){
                VertexBuffer vb = ent.getValue();
                if (vb.getBufferType() == Type.Index)
                    continue;

                // NOTE: we are not actually sure
                // how many elements will be in this buffer.
                // It will be compacted later.
                Buffer b = VertexBuffer.createBuffer(vb.getFormat(), 
                                                     vb.getNumComponents(),
                                                     outElementCount);

                VertexBuffer outVb = new VertexBuffer(vb.getBufferType());
                outVb.setNormalized(vb.isNormalized());
                outVb.setupData(vb.getUsage(), vb.getNumComponents(), vb.getFormat(), b);
                out.setBuffer(outVb);
            }

            int currentVertex = 0;
            for (int i = outOffset; i < outOffset + outLength; i++){
                OCTTriangle t = tris.get(i);

                // find vertex indices for triangle t
                in.getTriangle(t.getTriangleIndex(), vertIndicies);

                // find indices in new buf
                Integer i0 = indexCache.get(vertIndicies[0]);
                Integer i1 = indexCache.get(vertIndicies[1]);
                Integer i2 = indexCache.get(vertIndicies[2]);

                // check which ones were not created
                // if not created in new IB, create them
                if (i0 == null){
                    vertexCreated[0] = true;
                    newIndices[0] = currentVertex++;
                    indexCache.put(vertIndicies[0], newIndices[0]);
                }else{
                    newIndices[0] = i0.intValue();
                    vertexCreated[0] = false;
                }
                if (i1 == null){
                    vertexCreated[1] = true;
                    newIndices[1] = currentVertex++;
                    indexCache.put(vertIndicies[1], newIndices[1]);
                }else{
                    newIndices[1] = i1.intValue();
                    vertexCreated[1] = false;
                }
                if (i2 == null){
                    vertexCreated[2] = true;
                    newIndices[2] = currentVertex++;
                    indexCache.put(vertIndicies[2], newIndices[2]);
                }else{
                    newIndices[2] = i2.intValue();
                    vertexCreated[2] = false;
                }

                // if any verticies were created for this triangle
                // copy them to the output mesh
                IntMap<VertexBuffer> inbufs = in.getBuffers();
                for (Entry<VertexBuffer> ent : inbufs){
                    VertexBuffer vb = ent.getValue();
                    if (vb.getBufferType() == Type.Index)
                        continue;
                    
                    VertexBuffer outVb = out.getBuffer(vb.getBufferType());
                    // copy verticies that were created for this triangle
                    for (int v = 0; v < 3; v++){
                        if (!vertexCreated[v])
                            continue;

                        // copy triangle's attribute from one
                        // buffer to another
                        vb.copyElement(vertIndicies[v], outVb, newIndices[v]);
                    }
                }

                // write the indices onto the output index buffer
                ib.put((short)newIndices[0])
                  .put((short)newIndices[1])
                  .put((short)newIndices[2]);
            }
            ib.clear();
            indexCache.clear();

            // since some verticies were cached, it means there's
            // extra data in some buffers
            IntMap<VertexBuffer> outbufs = out.getBuffers();
            for (Entry<VertexBuffer> ent : outbufs){
                VertexBuffer vb = ent.getValue();
                if (vb.getBufferType() == Type.Index)
                    continue;

                vb.compact(currentVertex);
            }

            out.updateBound();
            out.updateCounts();
            out.setStatic();
            //out.setInterleaved();
            Geometry outGeom = new Geometry("Geom"+entry.getKey(), out);
            outGeom.setLocalTransform(inGeom.getWorldTransform());
            outGeom.setMaterial(inGeom.getMaterial());
            for (Light light : inGeom.getWorldLightList()){
                outGeom.addLight(light);
            }

            outGeom.updateGeometricState();
            newGeoms.add(outGeom);
        }

        return newGeoms;
    }

}
