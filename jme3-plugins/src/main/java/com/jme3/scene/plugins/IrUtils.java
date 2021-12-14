/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.scene.plugins;

import com.jme3.math.Vector4f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.mesh.IndexIntBuffer;
import com.jme3.scene.mesh.IndexShortBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class IrUtils {
    
    private static final Logger logger = Logger.getLogger(IrUtils.class.getName());
    
    private IrUtils() { }
    
    private static IrPolygon[] quadToTri(IrPolygon quad) {
        if (quad.vertices.length == 3) {
            throw new IllegalStateException("Already a triangle");
        } 
        
        IrPolygon[] t = new IrPolygon[]{ new IrPolygon(), new IrPolygon() };
        t[0].vertices = new IrVertex[3];
        t[1].vertices = new IrVertex[3];
        
        IrVertex v0 = quad.vertices[0];
        IrVertex v1 = quad.vertices[1];
        IrVertex v2 = quad.vertices[2];
        IrVertex v3 = quad.vertices[3];
        
        // find the pair of vertices that is closest to each over
        // v0 and v2
        // OR
        // v1 and v3
        float d1 = v0.pos.distanceSquared(v2.pos);
        float d2 = v1.pos.distanceSquared(v3.pos);
        if (d1 < d2) {
            // v0 is close to v2
            // put an edge in v0, v2
            t[0].vertices[0] = v0;
            t[0].vertices[1] = v1;
            t[0].vertices[2] = v3;

            t[1].vertices[0] = v1;
            t[1].vertices[1] = v2;
            t[1].vertices[2] = v3;
        } else {
            // put an edge in v1, v3
            t[0].vertices[0] = v0;
            t[0].vertices[1] = v1;
            t[0].vertices[2] = v2;

            t[1].vertices[0] = v0;
            t[1].vertices[1] = v2;
            t[1].vertices[2] = v3;
        }
        
        return t;
    }
    
    /**
     * Applies smoothing groups to vertex normals. XXX not implemented!
     * 
     * @param mesh ignored
     * @return null
     */
    public static IrMesh applySmoothingGroups(IrMesh mesh) {
        return null;
    }
    
    private static void toTangentsWithParity(IrVertex vertex) {
        if (vertex.tang != null && vertex.bitang != null) {
            float wCoord = vertex.norm.cross(vertex.tang).dot(vertex.bitang) < 0f ? -1f : 1f;
            vertex.tang4d = new Vector4f(vertex.tang.x, vertex.tang.y, vertex.tang.z, wCoord);
            vertex.tang = null;
            vertex.bitang = null;
        }
    }
    
    public static void toTangentsWithParity(IrMesh mesh) {
        for (IrPolygon polygon : mesh.polygons) {
            for (IrVertex vertex : polygon.vertices) {
                toTangentsWithParity(vertex);
            }
        }
    }
    
    /**
     * Removes low bone weights from mesh, leaving only 4 bone weights at max.
     * 
     * @param vertex the IrVertex to modify (not null)
     */
    private static void trimBoneWeights(IrVertex vertex) {
        if (vertex.boneWeightsIndices == null) {
            return;
        }
        
        IrBoneWeightIndex[] boneWeightsIndices = vertex.boneWeightsIndices;
        
        if (boneWeightsIndices.length <= 4) {
            return;
        }
        
        // Sort by weight
        boneWeightsIndices = Arrays.copyOf(boneWeightsIndices, boneWeightsIndices.length);
        Arrays.sort(boneWeightsIndices);
        
        // Trim to four weights at most
        boneWeightsIndices = Arrays.copyOf(boneWeightsIndices, 4);
        
        // Renormalize weights
        float sum = 0;
        
        for (int i = 0; i < boneWeightsIndices.length; i++) {
            sum += boneWeightsIndices[i].boneWeight;
        }
        
        if (sum != 1f) {
            float sumToB = sum == 0 ? 0 : 1f / sum;
            for (int i = 0; i < boneWeightsIndices.length; i++) {
                IrBoneWeightIndex original = boneWeightsIndices[i];
                boneWeightsIndices[i] = new IrBoneWeightIndex(original.boneIndex, original.boneWeight * sumToB);
            }
        }
        
        vertex.boneWeightsIndices = boneWeightsIndices;
    }
    
    /**
     * Removes low bone weights from mesh, leaving only 4 bone weights at max.
     * 
     * @param mesh the IrMesh to modify (not null)
     */
    public static void trimBoneWeights(IrMesh mesh) {
        for (IrPolygon polygon : mesh.polygons) {
            for (IrVertex vertex : polygon.vertices) {
                trimBoneWeights(vertex);
            }
        }
    }
    
    /**
     * Convert mesh from quads / triangles to triangles only.
     * 
     * @param mesh the input IrMesh (not null)
     */
    public static void triangulate(IrMesh mesh) {
        List<IrPolygon> newPolygons = new ArrayList<>(mesh.polygons.length);
        for (IrPolygon inputPoly : mesh.polygons) {
            if (inputPoly.vertices.length == 4) {
                IrPolygon[] tris = quadToTri(inputPoly);
                newPolygons.add(tris[0]);
                newPolygons.add(tris[1]);
            } else if (inputPoly.vertices.length == 3) {
                newPolygons.add(inputPoly);
            } else {
                // N-gon. We have to ignore it.
                logger.log(Level.WARNING, "N-gon encountered, ignoring. "
                                        + "The mesh may not appear correctly. "
                                        + "Triangulate your model prior to export.");
            }
        }
        mesh.polygons = new IrPolygon[newPolygons.size()];
        newPolygons.toArray(mesh.polygons);
    }
    
    /**
     * Separate mesh with multiple materials into multiple meshes each with 
     * one material each.
     * 
     * Polygons without a material will be added to key = -1.
     * 
     * @param mesh the input IrMesh (not null)
     * @return a new IntMap containing the resulting meshes
     */
    public static IntMap<IrMesh> splitByMaterial(IrMesh mesh) {
        IntMap<List<IrPolygon>> materialToPolyList = new IntMap<>();
        for (IrPolygon polygon : mesh.polygons) {
            int materialIndex = -1;
            for (IrVertex vertex : polygon.vertices) {
                if (vertex.material == null) {
                    continue;
                }
                if (materialIndex == -1) {
                    materialIndex = vertex.material;
                } else if (materialIndex != vertex.material) {
                    throw new UnsupportedOperationException("Multiple materials "
                                                 + "assigned to the same polygon");
                }
            }
            List<IrPolygon> polyList = materialToPolyList.get(materialIndex);
            if (polyList == null) {
                polyList = new ArrayList<IrPolygon>();
                materialToPolyList.put(materialIndex, polyList);
            }
            polyList.add(polygon);
        }
        IntMap<IrMesh> materialToMesh = new IntMap<>();
        for (IntMap.Entry<List<IrPolygon>> entry : materialToPolyList) {
            int key = entry.getKey();
            List<IrPolygon> polygons = entry.getValue();
            if (polygons.size() > 0) {
                IrMesh newMesh = new IrMesh();
                newMesh.polygons = new IrPolygon[polygons.size()];
                polygons.toArray(newMesh.polygons);
                materialToMesh.put(key, newMesh);
            }
        }
        return materialToMesh;
    }
     
    /**
     * Convert IrMesh to jME3 mesh.
     *
     * @param mesh the input IrMesh (not null)
     * @return a new Mesh
     */
    public static Mesh convertIrMeshToJmeMesh(IrMesh mesh) {
        Map<IrVertex, Integer> vertexToVertexIndex = new HashMap<>();
        List<IrVertex> vertices = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        
        int vertexIndex = 0;
        for (IrPolygon polygon : mesh.polygons) {
            if (polygon.vertices.length != 3) {
                throw new UnsupportedOperationException("IrMesh must be triangulated first");
            }
            for (IrVertex vertex : polygon.vertices) {
                // Is this vertex already indexed?
                Integer existingIndex = vertexToVertexIndex.get(vertex);
                if (existingIndex == null) {
                    // Not indexed yet, allocate index.
                    indexes.add(vertexIndex);
                    vertexToVertexIndex.put(vertex, vertexIndex);
                    vertices.add(vertex);
                    vertexIndex++;
                } else {
                    // Index already allocated for this vertex, reuse it.
                    indexes.add(existingIndex);
                }
            }
        }
        
        Mesh jmeMesh = new Mesh();
        jmeMesh.setMode(Mesh.Mode.Triangles);
        
        FloatBuffer posBuf = null;
        FloatBuffer normBuf = null;
        FloatBuffer tangBuf = null;
        FloatBuffer uv0Buf = null;
        FloatBuffer uv1Buf = null;
        ByteBuffer colorBuf = null;
        ByteBuffer boneIndices = null;
        FloatBuffer boneWeights = null;
        IndexBuffer indexBuf = null;
        
        IrVertex inspectionVertex = vertices.get(0);
        if (inspectionVertex.pos != null) {
            posBuf = BufferUtils.createVector3Buffer(vertices.size());
            jmeMesh.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
        }
        if (inspectionVertex.norm != null) {
            normBuf = BufferUtils.createVector3Buffer(vertices.size());
            jmeMesh.setBuffer(VertexBuffer.Type.Normal, 3, normBuf);
        }
        if (inspectionVertex.tang4d != null) {
            tangBuf = BufferUtils.createFloatBuffer(vertices.size() * 4);
            jmeMesh.setBuffer(VertexBuffer.Type.Tangent, 4, tangBuf);
        }
        if (inspectionVertex.tang != null || inspectionVertex.bitang != null) {
            throw new IllegalStateException("Mesh is using 3D tangents, must be converted to 4D tangents first.");
        }
        if (inspectionVertex.uv0 != null) {
            uv0Buf = BufferUtils.createVector2Buffer(vertices.size());
            jmeMesh.setBuffer(VertexBuffer.Type.TexCoord, 2, uv0Buf);
        }
        if (inspectionVertex.uv1 != null) {
            uv1Buf = BufferUtils.createVector2Buffer(vertices.size());
            jmeMesh.setBuffer(VertexBuffer.Type.TexCoord2, 2, uv1Buf);
        }
        if (inspectionVertex.color != null) {
            colorBuf = BufferUtils.createByteBuffer(vertices.size() * 4);
            jmeMesh.setBuffer(VertexBuffer.Type.Color, 4, colorBuf);
            jmeMesh.getBuffer(VertexBuffer.Type.Color).setNormalized(true);
        }
        if (inspectionVertex.boneWeightsIndices != null) {
            boneIndices = BufferUtils.createByteBuffer(vertices.size() * 4);
            boneWeights = BufferUtils.createFloatBuffer(vertices.size() * 4);
            jmeMesh.setBuffer(VertexBuffer.Type.BoneIndex,  4, boneIndices);
            jmeMesh.setBuffer(VertexBuffer.Type.BoneWeight, 4, boneWeights);
            
            //creating empty buffers for HW skinning 
            //the buffers will be setup if ever used.
            VertexBuffer weightsHW = new VertexBuffer(VertexBuffer.Type.HWBoneWeight);
            VertexBuffer indicesHW = new VertexBuffer(VertexBuffer.Type.HWBoneIndex);
            //setting usage to cpuOnly so that the buffer is not send empty to the GPU
            indicesHW.setUsage(VertexBuffer.Usage.CpuOnly);
            weightsHW.setUsage(VertexBuffer.Usage.CpuOnly);
            
            jmeMesh.setBuffer(weightsHW);
            jmeMesh.setBuffer(indicesHW);
        }
        if (vertices.size() >= 65536) {
            // too many vertices: use IntBuffer instead of ShortBuffer
            IntBuffer ib = BufferUtils.createIntBuffer(indexes.size());
            jmeMesh.setBuffer(VertexBuffer.Type.Index, 3, ib);
            indexBuf = new IndexIntBuffer(ib);
        } else {
            ShortBuffer sb = BufferUtils.createShortBuffer(indexes.size());
            jmeMesh.setBuffer(VertexBuffer.Type.Index, 3, sb);
            indexBuf = new IndexShortBuffer(sb);
        }
        
        jmeMesh.setStatic();
        
        int maxBonesPerVertex = -1;
        
        for (IrVertex vertex : vertices) {
            if (posBuf != null) {
                posBuf.put(vertex.pos.x).put(vertex.pos.y).put(vertex.pos.z);
            }
            if (normBuf != null) {
                normBuf.put(vertex.norm.x).put(vertex.norm.y).put(vertex.norm.z);
            }
            if (tangBuf != null) {
                tangBuf.put(vertex.tang4d.x).put(vertex.tang4d.y).put(vertex.tang4d.z).put(vertex.tang4d.w);
            }
            if (uv0Buf != null) {
                uv0Buf.put(vertex.uv0.x).put(vertex.uv0.y);
            }
            if (uv1Buf != null) {
                uv1Buf.put(vertex.uv1.x).put(vertex.uv1.y);
            }
            if (colorBuf != null) {
                colorBuf.putInt(vertex.color.asIntABGR());
            }
            if (boneIndices != null) {
                if (vertex.boneWeightsIndices != null) {
                    if (vertex.boneWeightsIndices.length > 4) {
                        throw new UnsupportedOperationException("Mesh uses more than 4 weights per bone. " +
                                                                "Call trimBoneWeights() to alleviate this");
                    }
                    for (int i = 0; i < vertex.boneWeightsIndices.length; i++) {
                        boneIndices.put((byte) (vertex.boneWeightsIndices[i].boneIndex & 0xFF));
                        boneWeights.put(vertex.boneWeightsIndices[i].boneWeight);
                    }
                    for (int i = 0; i < 4 - vertex.boneWeightsIndices.length; i++) {
                        boneIndices.put((byte)0);
                        boneWeights.put(0f);
                    }
                } else {
                    boneIndices.putInt(0);
                    boneWeights.put(0f).put(0f).put(0f).put(0f);
                }
                
                maxBonesPerVertex = Math.max(maxBonesPerVertex, vertex.boneWeightsIndices.length);
            }
        }
        
        for (int i = 0; i < indexes.size(); i++) {
            indexBuf.put(i, indexes.get(i));
        }
        
        jmeMesh.updateCounts();
        jmeMesh.updateBound();
        
        if (boneIndices != null) {
            jmeMesh.setMaxNumWeights(maxBonesPerVertex);
            jmeMesh.prepareForAnim(true);
            jmeMesh.generateBindPose(true);
        }
        
        return jmeMesh;
    }
}
