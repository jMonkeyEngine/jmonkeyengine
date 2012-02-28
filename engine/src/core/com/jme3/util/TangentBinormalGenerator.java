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
package com.jme3.util;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.mesh.IndexBuffer;
import static com.jme3.util.BufferUtils.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Lex (Aleksey Nikiforov)
  */
public class TangentBinormalGenerator {
    
    private static final float ZERO_TOLERANCE = 0.0000001f;
    private static final Logger log = Logger.getLogger(
            TangentBinormalGenerator.class.getName());
    private static float toleranceAngle;
    private static float toleranceDot;
    
    static {
        setToleranceAngle(45);
    }
    
    
    private static class VertexInfo {
        public final Vector3f position;
        public final Vector3f normal;
        public final ArrayList<Integer> indices = new ArrayList<Integer>();
        
        public VertexInfo(Vector3f position, Vector3f normal) {
            this.position = position;
            this.normal = normal;
        }
    }
    
    /** Collects all the triangle data for one vertex.
     */
    private static class VertexData {
        public final ArrayList<TriangleData> triangles = new ArrayList<TriangleData>();
        
        public VertexData() { }
    }
    
    /** Keeps track of tangent, binormal, and normal for one triangle.
     */
    public static class TriangleData {
        public final Vector3f tangent;
        public final Vector3f binormal;
        public final Vector3f normal;
        
        public TriangleData(Vector3f tangent, Vector3f binormal, Vector3f normal) {
            this.tangent = tangent;
            this.binormal = binormal;
            this.normal = normal;
        }
    }
    
    private static VertexData[] initVertexData(int size) {
        VertexData[] vertices = new VertexData[size];
        for (int i = 0; i < size; i++) {
            vertices[i] = new VertexData();
        }
        return vertices;
    }
    
    public static void generate(Mesh mesh) {
        generate(mesh, true);
    }
    
    public static void generate(Spatial scene) {
        if (scene instanceof Node) {
            Node node = (Node) scene;
            for (Spatial child : node.getChildren()) {
                generate(child);
            }
        } else {
            Geometry geom = (Geometry) scene;
            Mesh mesh = geom.getMesh();
            
            // Check to ensure mesh has texcoords and normals before generating
            if (mesh.getBuffer(Type.TexCoord) != null 
             && mesh.getBuffer(Type.Normal) != null){
                generate(geom.getMesh());
            }
        }
    }
    
    public static void generate(Mesh mesh, boolean approxTangents) {
        int[] index = new int[3];
        Vector3f[] v = new Vector3f[3];
        Vector2f[] t = new Vector2f[3];
        for (int i = 0; i < 3; i++) {
            v[i] = new Vector3f();
            t[i] = new Vector2f();
        }
        
        if (mesh.getBuffer(Type.Normal) == null) {
            throw new IllegalArgumentException("The given mesh has no normal data!");
        }
        
        VertexData[] vertices;
        switch (mesh.getMode()) {
            case Triangles:
                vertices = processTriangles(mesh, index, v, t);
                break;
            case TriangleStrip:
                vertices = processTriangleStrip(mesh, index, v, t);
                break;
            case TriangleFan:
                vertices = processTriangleFan(mesh, index, v, t);
                break;
            default:
                throw new UnsupportedOperationException(
                        mesh.getMode() + " is not supported.");
        }
        
        processTriangleData(mesh, vertices, approxTangents);

        //if the mesh has a bind pose, we need to generate the bind pose for the tangent buffer
        if (mesh.getBuffer(Type.BindPosePosition) != null) {
            
            VertexBuffer tangents = mesh.getBuffer(Type.Tangent);
            if (tangents != null) {
                VertexBuffer bindTangents = new VertexBuffer(Type.BindPoseTangent);
                bindTangents.setupData(Usage.CpuOnly,
                        4,
                        Format.Float,
                        BufferUtils.clone(tangents.getData()));
                
                if (mesh.getBuffer(Type.BindPoseTangent) != null) {
                    mesh.clearBuffer(Type.BindPoseTangent);
                }
                mesh.setBuffer(bindTangents);
                tangents.setUsage(Usage.Stream);
            }
        }
    }
    
    private static VertexData[] processTriangles(Mesh mesh,
            int[] index, Vector3f[] v, Vector2f[] t) {
        IndexBuffer indexBuffer = mesh.getIndexBuffer();
        FloatBuffer vertexBuffer = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        if (mesh.getBuffer(Type.TexCoord) == null) {
            throw new IllegalArgumentException("Can only generate tangents for "
                    + "meshes with texture coordinates");
        }
        
        FloatBuffer textureBuffer = (FloatBuffer) mesh.getBuffer(Type.TexCoord).getData();
        
        VertexData[] vertices = initVertexData(vertexBuffer.capacity() / 3);
        
        for (int i = 0; i < indexBuffer.size() / 3; i++) {
            for (int j = 0; j < 3; j++) {
                index[j] = indexBuffer.get(i * 3 + j);
                populateFromBuffer(v[j], vertexBuffer, index[j]);
                populateFromBuffer(t[j], textureBuffer, index[j]);
            }
            
            TriangleData triData = processTriangle(index, v, t);
            if (triData != null) {
                vertices[index[0]].triangles.add(triData);
                vertices[index[1]].triangles.add(triData);
                vertices[index[2]].triangles.add(triData);
            }
        }
        
        return vertices;
    }
    
    private static VertexData[] processTriangleStrip(Mesh mesh,
            int[] index, Vector3f[] v, Vector2f[] t) {
        IndexBuffer indexBuffer = mesh.getIndexBuffer();
        FloatBuffer vertexBuffer = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        FloatBuffer textureBuffer = (FloatBuffer) mesh.getBuffer(Type.TexCoord).getData();
        
        VertexData[] vertices = initVertexData(vertexBuffer.capacity() / 3);
        
        index[0] = indexBuffer.get(0);
        index[1] = indexBuffer.get(1);
        
        populateFromBuffer(v[0], vertexBuffer, index[0]);
        populateFromBuffer(v[1], vertexBuffer, index[1]);
        
        populateFromBuffer(t[0], textureBuffer, index[0]);
        populateFromBuffer(t[1], textureBuffer, index[1]);
        
        for (int i = 2; i < indexBuffer.size(); i++) {
            index[2] = indexBuffer.get(i);
            BufferUtils.populateFromBuffer(v[2], vertexBuffer, index[2]);
            BufferUtils.populateFromBuffer(t[2], textureBuffer, index[2]);
            
            boolean isDegenerate = isDegenerateTriangle(v[0], v[1], v[2]);
            TriangleData triData = processTriangle(index, v, t);
            
            if (triData != null && !isDegenerate) {
                vertices[index[0]].triangles.add(triData);
                vertices[index[1]].triangles.add(triData);
                vertices[index[2]].triangles.add(triData);
            }
            
            Vector3f vTemp = v[0];
            v[0] = v[1];
            v[1] = v[2];
            v[2] = vTemp;
            
            Vector2f tTemp = t[0];
            t[0] = t[1];
            t[1] = t[2];
            t[2] = tTemp;
            
            index[0] = index[1];
            index[1] = index[2];
        }
        
        return vertices;
    }
    
    private static VertexData[] processTriangleFan(Mesh mesh,
            int[] index, Vector3f[] v, Vector2f[] t) {
        IndexBuffer indexBuffer = mesh.getIndexBuffer();
        FloatBuffer vertexBuffer = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        FloatBuffer textureBuffer = (FloatBuffer) mesh.getBuffer(Type.TexCoord).getData();
        
        VertexData[] vertices = initVertexData(vertexBuffer.capacity() / 3);
        
        index[0] = indexBuffer.get(0);
        index[1] = indexBuffer.get(1);
        
        populateFromBuffer(v[0], vertexBuffer, index[0]);
        populateFromBuffer(v[1], vertexBuffer, index[1]);
        
        populateFromBuffer(t[0], textureBuffer, index[0]);
        populateFromBuffer(t[1], textureBuffer, index[1]);
        
        for (int i = 2; i < vertexBuffer.capacity() / 3; i++) {
            index[2] = indexBuffer.get(i);
            populateFromBuffer(v[2], vertexBuffer, index[2]);
            populateFromBuffer(t[2], textureBuffer, index[2]);
            
            TriangleData triData = processTriangle(index, v, t);
            if (triData != null) {
                vertices[index[0]].triangles.add(triData);
                vertices[index[1]].triangles.add(triData);
                vertices[index[2]].triangles.add(triData);
            }
            
            Vector3f vTemp = v[1];
            v[1] = v[2];
            v[2] = vTemp;
            
            Vector2f tTemp = t[1];
            t[1] = t[2];
            t[2] = tTemp;
            
            index[1] = index[2];
        }
        
        return vertices;
    }

    // check if the area is greater than zero
    private static boolean isDegenerateTriangle(Vector3f a, Vector3f b, Vector3f c) {
        return (a.subtract(b).cross(c.subtract(b))).lengthSquared() == 0;
    }
    
    public static TriangleData processTriangle(int[] index,
            Vector3f[] v, Vector2f[] t) {
        Vector3f edge1 = new Vector3f();
        Vector3f edge2 = new Vector3f();
        Vector2f edge1uv = new Vector2f();
        Vector2f edge2uv = new Vector2f();
        
        Vector3f tangent = new Vector3f();
        Vector3f binormal = new Vector3f();
        Vector3f normal = new Vector3f();
        
        t[1].subtract(t[0], edge1uv);
        t[2].subtract(t[0], edge2uv);
        float det = edge1uv.x * edge2uv.y - edge1uv.y * edge2uv.x;
        
        boolean normalize = false;
        if (Math.abs(det) < ZERO_TOLERANCE) {
            log.log(Level.WARNING, "Colinear uv coordinates for triangle "
                    + "[{0}, {1}, {2}]; tex0 = [{3}, {4}], "
                    + "tex1 = [{5}, {6}], tex2 = [{7}, {8}]",
                    new Object[]{index[0], index[1], index[2],
                        t[0].x, t[0].y, t[1].x, t[1].y, t[2].x, t[2].y});
            det = 1;
            normalize = true;
        }
        
        v[1].subtract(v[0], edge1);
        v[2].subtract(v[0], edge2);
        
        tangent.set(edge1);
        tangent.normalizeLocal();
        binormal.set(edge2);
        binormal.normalizeLocal();
        
        if (Math.abs(Math.abs(tangent.dot(binormal)) - 1)
                < ZERO_TOLERANCE) {
            log.log(Level.WARNING, "Vertices are on the same line "
                    + "for triangle [{0}, {1}, {2}].",
                    new Object[]{index[0], index[1], index[2]});
        }
        
        float factor = 1 / det;
        tangent.x = (edge2uv.y * edge1.x - edge1uv.y * edge2.x) * factor;
        tangent.y = (edge2uv.y * edge1.y - edge1uv.y * edge2.y) * factor;
        tangent.z = (edge2uv.y * edge1.z - edge1uv.y * edge2.z) * factor;
        if (normalize) {
            tangent.normalizeLocal();
        }
        
        binormal.x = (edge1uv.x * edge2.x - edge2uv.x * edge1.x) * factor;
        binormal.y = (edge1uv.x * edge2.y - edge2uv.x * edge1.y) * factor;
        binormal.z = (edge1uv.x * edge2.z - edge2uv.x * edge1.z) * factor;
        if (normalize) {
            binormal.normalizeLocal();
        }
        
        tangent.cross(binormal, normal);
        normal.normalizeLocal();
        
        return new TriangleData(
                tangent,
                binormal,
                normal);
    }
    
    public static void setToleranceAngle(float angle) {
        if (angle < 0 || angle > 179) {
            throw new IllegalArgumentException(
                    "The angle must be between 0 and 179 degrees.");
        }
        toleranceDot = FastMath.cos(angle * FastMath.DEG_TO_RAD);
        toleranceAngle = angle;
    }
    
    
    private static boolean approxEqual(Vector3f u, Vector3f v) {
        float tolerance = 1E-4f;
        return (FastMath.abs(u.x - v.x) < tolerance) &&
               (FastMath.abs(u.y - v.y) < tolerance) &&
               (FastMath.abs(u.z - v.z) < tolerance);
    }
    
    private static ArrayList<VertexInfo> linkVertices(Mesh mesh) {
        ArrayList<VertexInfo> vertexMap = new ArrayList<VertexInfo>();
        
        FloatBuffer vertexBuffer = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        FloatBuffer normalBuffer = (FloatBuffer) mesh.getBuffer(Type.Normal).getData();
        
        Vector3f position = new Vector3f();
        Vector3f normal = new Vector3f();
        
        final int size = vertexBuffer.capacity() / 3;
        for (int i = 0; i < size; i++) {
            
            populateFromBuffer(position, vertexBuffer, i);
            populateFromBuffer(normal, normalBuffer, i);
            
            boolean found = false;
            
            for (int j = 0; j < vertexMap.size(); j++) {
                VertexInfo vertexInfo = vertexMap.get(j);
                if (approxEqual(vertexInfo.position, position) &&
                    approxEqual(vertexInfo.normal, normal))
                {
                    vertexInfo.indices.add(i);
                    found = true;
                    break;  
                }
            }
            
            if (!found) {
                VertexInfo vertexInfo = new VertexInfo(position.clone(), normal.clone());
                vertexInfo.indices.add(i);
                vertexMap.add(vertexInfo);
            }
        }
        
        return vertexMap;
    }
    
    private static void processTriangleData(Mesh mesh, VertexData[] vertices,
            boolean approxTangent)
    {
        ArrayList<VertexInfo> vertexMap = linkVertices(mesh);
        
        FloatBuffer normalBuffer = (FloatBuffer) mesh.getBuffer(Type.Normal).getData();
        
        FloatBuffer tangents = BufferUtils.createFloatBuffer(vertices.length * 4);
//        FloatBuffer binormals = BufferUtils.createFloatBuffer(vertices.length * 3);

        Vector3f tangent = new Vector3f();
        Vector3f binormal = new Vector3f();
        Vector3f normal = new Vector3f();
        Vector3f givenNormal = new Vector3f();
        
        Vector3f tangentUnit = new Vector3f();
        Vector3f binormalUnit = new Vector3f();
        
        for (int k = 0; k < vertexMap.size(); k++) {
            float wCoord = -1;
            
            VertexInfo vertexInfo = vertexMap.get(k);
            
            givenNormal.set(vertexInfo.normal);
            givenNormal.normalizeLocal();
            
            TriangleData firstTriangle = vertices[vertexInfo.indices.get(0)].triangles.get(0);

            // check tangent and binormal consistency
            tangent.set(firstTriangle.tangent);
            tangent.normalizeLocal();
            binormal.set(firstTriangle.binormal);
            binormal.normalizeLocal();
            
            for (int i : vertexInfo.indices) {
                ArrayList<TriangleData> triangles = vertices[i].triangles;
                
                for (int j = 0; j < triangles.size(); j++) {
                    TriangleData triangleData = triangles.get(j);

                    tangentUnit.set(triangleData.tangent);
                    tangentUnit.normalizeLocal();
                    if (tangent.dot(tangentUnit) < toleranceDot) {
                        log.log(Level.WARNING,
                                "Angle between tangents exceeds tolerance "
                                + "for vertex {0}.", i);
                        break;
                    }

                    if (!approxTangent) {
                        binormalUnit.set(triangleData.binormal);
                        binormalUnit.normalizeLocal();
                        if (binormal.dot(binormalUnit) < toleranceDot) {
                            log.log(Level.WARNING,
                                    "Angle between binormals exceeds tolerance "
                                    + "for vertex {0}.", i);
                            break;
                        }
                    }
                }
            }
            
            
            // find average tangent
            tangent.set(0, 0, 0);
            binormal.set(0, 0, 0);
            
            int triangleCount = 0;
            for (int i : vertexInfo.indices) {
                ArrayList<TriangleData> triangles = vertices[i].triangles;
                triangleCount += triangles.size();
                
                boolean flippedNormal = false;
                for (int j = 0; j < triangles.size(); j++) {
                    TriangleData triangleData = triangles.get(j);
                    tangent.addLocal(triangleData.tangent);
                    binormal.addLocal(triangleData.binormal);

                    if (givenNormal.dot(triangleData.normal) < 0) {
                        flippedNormal = true;
                    }
                }
                if (flippedNormal /*&& approxTangent*/) {
                    // Generated normal is flipped for this vertex,
                    // so binormal = normal.cross(tangent) will be flipped in the shader
    //                log.log(Level.WARNING,
    //                        "Binormal is flipped for vertex {0}.", i);

                    wCoord = 1;
                }
            }

            
            int blameVertex = vertexInfo.indices.get(0);
            
            if (tangent.length() < ZERO_TOLERANCE) {
                log.log(Level.WARNING,
                        "Shared tangent is zero for vertex {0}.", blameVertex);
                // attempt to fix from binormal
                if (binormal.length() >= ZERO_TOLERANCE) {
                    binormal.cross(givenNormal, tangent);
                    tangent.normalizeLocal();
                } // if all fails use the tangent from the first triangle
                else {
                    tangent.set(firstTriangle.tangent);
                }
            } else {
                tangent.divideLocal(triangleCount);
            }

            tangentUnit.set(tangent);
            tangentUnit.normalizeLocal();
            if (Math.abs(Math.abs(tangentUnit.dot(givenNormal)) - 1)
                    < ZERO_TOLERANCE) {
                log.log(Level.WARNING,
                        "Normal and tangent are parallel for vertex {0}.", blameVertex);
            }


            if (!approxTangent) {
                if (binormal.length() < ZERO_TOLERANCE) {
                    log.log(Level.WARNING,
                            "Shared binormal is zero for vertex {0}.", blameVertex);
                    // attempt to fix from tangent
                    if (tangent.length() >= ZERO_TOLERANCE) {
                        givenNormal.cross(tangent, binormal);
                        binormal.normalizeLocal();
                    } // if all fails use the binormal from the first triangle
                    else {
                        binormal.set(firstTriangle.binormal);
                    }
                } else {
                    binormal.divideLocal(triangleCount);
                }

                binormalUnit.set(binormal);
                binormalUnit.normalizeLocal();
                if (Math.abs(Math.abs(binormalUnit.dot(givenNormal)) - 1)
                        < ZERO_TOLERANCE) {
                    log.log(Level.WARNING,
                            "Normal and binormal are parallel for vertex {0}.", blameVertex);
                }

                if (Math.abs(Math.abs(binormalUnit.dot(tangentUnit)) - 1)
                        < ZERO_TOLERANCE) {
                    log.log(Level.WARNING,
                            "Tangent and binormal are parallel for vertex {0}.", blameVertex);
                }
            }
            
            for (int i : vertexInfo.indices) {
                if (approxTangent) {
                    // This calculation ensures that normal and tagent have a 90 degree angle.
                    // Removing this will lead to visual artifacts.
                    givenNormal.cross(tangent, binormal);
                    binormal.cross(givenNormal, tangent);

                    tangent.normalizeLocal();

                    tangents.put((i * 4), tangent.x);
                    tangents.put((i * 4) + 1, tangent.y);
                    tangents.put((i * 4) + 2, tangent.z);
                    tangents.put((i * 4) + 3, wCoord);
                } else {
                    tangents.put((i * 4), tangent.x);
                    tangents.put((i * 4) + 1, tangent.y);
                    tangents.put((i * 4) + 2, tangent.z);
                    tangents.put((i * 4) + 3, wCoord);

                    //setInBuffer(binormal, binormals, i);
                }
            }
        }
        
        mesh.setBuffer(Type.Tangent, 4, tangents);
//        if (!approxTangent) mesh.setBuffer(Type.Binormal, 3, binormals);
    }
    
    public static Mesh genTbnLines(Mesh mesh, float scale) {
        if (mesh.getBuffer(Type.Tangent) == null) {
            return genNormalLines(mesh, scale);
        } else {
            return genTangentLines(mesh, scale);
        }
    }
    
    public static Mesh genNormalLines(Mesh mesh, float scale) {
        FloatBuffer vertexBuffer = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        FloatBuffer normalBuffer = (FloatBuffer) mesh.getBuffer(Type.Normal).getData();
        
        ColorRGBA originColor = ColorRGBA.White;
        ColorRGBA normalColor = ColorRGBA.Blue;
        
        Mesh lineMesh = new Mesh();
        lineMesh.setMode(Mesh.Mode.Lines);
        
        Vector3f origin = new Vector3f();
        Vector3f point = new Vector3f();
        
        FloatBuffer lineVertex = BufferUtils.createFloatBuffer(vertexBuffer.capacity() * 2);
        FloatBuffer lineColor = BufferUtils.createFloatBuffer(vertexBuffer.capacity() / 3 * 4 * 2);
        
        for (int i = 0; i < vertexBuffer.capacity() / 3; i++) {
            populateFromBuffer(origin, vertexBuffer, i);
            populateFromBuffer(point, normalBuffer, i);
            
            int index = i * 2;
            
            setInBuffer(origin, lineVertex, index);
            setInBuffer(originColor, lineColor, index);
            
            point.multLocal(scale);
            point.addLocal(origin);
            setInBuffer(point, lineVertex, index + 1);
            setInBuffer(normalColor, lineColor, index + 1);
        }
        
        lineMesh.setBuffer(Type.Position, 3, lineVertex);
        lineMesh.setBuffer(Type.Color, 4, lineColor);
        
        lineMesh.setStatic();
        //lineMesh.setInterleaved();
        return lineMesh;
    }
    
    private static Mesh genTangentLines(Mesh mesh, float scale) {
        FloatBuffer vertexBuffer = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        FloatBuffer normalBuffer = (FloatBuffer) mesh.getBuffer(Type.Normal).getData();
        FloatBuffer tangentBuffer = (FloatBuffer) mesh.getBuffer(Type.Tangent).getData();
        
        FloatBuffer binormalBuffer = null;
        if (mesh.getBuffer(Type.Binormal) != null) {
            binormalBuffer = (FloatBuffer) mesh.getBuffer(Type.Binormal).getData();
        }
        
        ColorRGBA originColor = ColorRGBA.White;
        ColorRGBA tangentColor = ColorRGBA.Red;
        ColorRGBA binormalColor = ColorRGBA.Green;
        ColorRGBA normalColor = ColorRGBA.Blue;
        
        Mesh lineMesh = new Mesh();
        lineMesh.setMode(Mesh.Mode.Lines);
        
        Vector3f origin = new Vector3f();
        Vector3f point = new Vector3f();
        Vector3f tangent = new Vector3f();
        Vector3f normal = new Vector3f();
        
        IntBuffer lineIndex = BufferUtils.createIntBuffer(vertexBuffer.capacity() / 3 * 6);
        FloatBuffer lineVertex = BufferUtils.createFloatBuffer(vertexBuffer.capacity() * 4);
        FloatBuffer lineColor = BufferUtils.createFloatBuffer(vertexBuffer.capacity() / 3 * 4 * 4);
        
        boolean hasParity = mesh.getBuffer(Type.Tangent).getNumComponents() == 4;
        float tangentW = 1;
        
        for (int i = 0; i < vertexBuffer.capacity() / 3; i++) {
            populateFromBuffer(origin, vertexBuffer, i);
            populateFromBuffer(normal, normalBuffer, i);
            
            if (hasParity) {
                tangent.x = tangentBuffer.get(i * 4);
                tangent.y = tangentBuffer.get(i * 4 + 1);
                tangent.z = tangentBuffer.get(i * 4 + 2);
                tangentW = tangentBuffer.get(i * 4 + 3);
            } else {
                populateFromBuffer(tangent, tangentBuffer, i);
            }
            
            int index = i * 4;
            
            int id = i * 6;
            lineIndex.put(id, index);
            lineIndex.put(id + 1, index + 1);
            lineIndex.put(id + 2, index);
            lineIndex.put(id + 3, index + 2);
            lineIndex.put(id + 4, index);
            lineIndex.put(id + 5, index + 3);
            
            setInBuffer(origin, lineVertex, index);
            setInBuffer(originColor, lineColor, index);
            
            point.set(tangent);
            point.multLocal(scale);
            point.addLocal(origin);
            setInBuffer(point, lineVertex, index + 1);
            setInBuffer(tangentColor, lineColor, index + 1);

            // wvBinormal = cross(wvNormal, wvTangent) * -inTangent.w

            if (binormalBuffer == null) {
                normal.cross(tangent, point);
                point.multLocal(-tangentW);
                point.normalizeLocal();
            } else {
                populateFromBuffer(point, binormalBuffer, i);
            }
            
            point.multLocal(scale);
            point.addLocal(origin);
            setInBuffer(point, lineVertex, index + 2);
            setInBuffer(binormalColor, lineColor, index + 2);
            
            point.set(normal);
            point.multLocal(scale);
            point.addLocal(origin);
            setInBuffer(point, lineVertex, index + 3);
            setInBuffer(normalColor, lineColor, index + 3);
        }
        
        lineMesh.setBuffer(Type.Index, 1, lineIndex);
        lineMesh.setBuffer(Type.Position, 3, lineVertex);
        lineMesh.setBuffer(Type.Color, 4, lineColor);
        
        lineMesh.setStatic();
        //lineMesh.setInterleaved();
        return lineMesh;
    }
}
