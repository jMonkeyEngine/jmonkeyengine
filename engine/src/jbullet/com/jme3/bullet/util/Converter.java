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
package com.jme3.bullet.util;

import com.bulletphysics.collision.shapes.IndexedMesh;
import com.bulletphysics.dom.HeightfieldTerrainShape;
import com.jme3.math.FastMath;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Nice convenience methods for conversion between javax.vecmath and com.jme3.math
 * Objects, also some jme to jbullet mesh conversion.
 * @author normenhansen
 */
public class Converter {

    private Converter() {
    }

    public static com.jme3.math.Vector3f convert(javax.vecmath.Vector3f oldVec) {
        com.jme3.math.Vector3f newVec = new com.jme3.math.Vector3f();
        convert(oldVec, newVec);
        return newVec;
    }

    public static com.jme3.math.Vector3f convert(javax.vecmath.Vector3f oldVec, com.jme3.math.Vector3f newVec) {
        newVec.x = oldVec.x;
        newVec.y = oldVec.y;
        newVec.z = oldVec.z;
        return newVec;
    }

    public static javax.vecmath.Vector3f convert(com.jme3.math.Vector3f oldVec) {
        javax.vecmath.Vector3f newVec = new javax.vecmath.Vector3f();
        convert(oldVec, newVec);
        return newVec;
    }

    public static javax.vecmath.Vector3f convert(com.jme3.math.Vector3f oldVec, javax.vecmath.Vector3f newVec) {
        newVec.x = oldVec.x;
        newVec.y = oldVec.y;
        newVec.z = oldVec.z;
        return newVec;
    }

    public static javax.vecmath.Quat4f convert(com.jme3.math.Quaternion oldQuat, javax.vecmath.Quat4f newQuat) {
        newQuat.w = oldQuat.getW();
        newQuat.x = oldQuat.getX();
        newQuat.y = oldQuat.getY();
        newQuat.z = oldQuat.getZ();
        return newQuat;
    }

    public static javax.vecmath.Quat4f convert(com.jme3.math.Quaternion oldQuat) {
        javax.vecmath.Quat4f newQuat = new javax.vecmath.Quat4f();
        convert(oldQuat, newQuat);
        return newQuat;
    }

    public static com.jme3.math.Quaternion convert(javax.vecmath.Quat4f oldQuat, com.jme3.math.Quaternion newQuat) {
        newQuat.set(oldQuat.x, oldQuat.y, oldQuat.z, oldQuat.w);
        return newQuat;
    }

    public static com.jme3.math.Quaternion convert(javax.vecmath.Quat4f oldQuat) {
        com.jme3.math.Quaternion newQuat = new com.jme3.math.Quaternion();
        convert(oldQuat, newQuat);
        return newQuat;
    }

    public static com.jme3.math.Quaternion convert(javax.vecmath.Matrix3f oldMatrix, com.jme3.math.Quaternion newQuaternion) {
        // the trace is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html
        float t = oldMatrix.m00 + oldMatrix.m11 + oldMatrix.m22;
        float w, x, y, z;
        // we protect the division by s by ensuring that s>=1
        if (t >= 0) { // |w| >= .5
            float s = FastMath.sqrt(t + 1); // |s|>=1 ...
            w = 0.5f * s;
            s = 0.5f / s;                 // so this division isn't bad
            x = (oldMatrix.m21 - oldMatrix.m12) * s;
            y = (oldMatrix.m02 - oldMatrix.m20) * s;
            z = (oldMatrix.m10 - oldMatrix.m01) * s;
        } else if ((oldMatrix.m00 > oldMatrix.m11) && (oldMatrix.m00 > oldMatrix.m22)) {
            float s = FastMath.sqrt(1.0f + oldMatrix.m00 - oldMatrix.m11 - oldMatrix.m22); // |s|>=1
            x = s * 0.5f; // |x| >= .5
            s = 0.5f / s;
            y = (oldMatrix.m10 + oldMatrix.m01) * s;
            z = (oldMatrix.m02 + oldMatrix.m20) * s;
            w = (oldMatrix.m21 - oldMatrix.m12) * s;
        } else if (oldMatrix.m11 > oldMatrix.m22) {
            float s = FastMath.sqrt(1.0f + oldMatrix.m11 - oldMatrix.m00 - oldMatrix.m22); // |s|>=1
            y = s * 0.5f; // |y| >= .5
            s = 0.5f / s;
            x = (oldMatrix.m10 + oldMatrix.m01) * s;
            z = (oldMatrix.m21 + oldMatrix.m12) * s;
            w = (oldMatrix.m02 - oldMatrix.m20) * s;
        } else {
            float s = FastMath.sqrt(1.0f + oldMatrix.m22 - oldMatrix.m00 - oldMatrix.m11); // |s|>=1
            z = s * 0.5f; // |z| >= .5
            s = 0.5f / s;
            x = (oldMatrix.m02 + oldMatrix.m20) * s;
            y = (oldMatrix.m21 + oldMatrix.m12) * s;
            w = (oldMatrix.m10 - oldMatrix.m01) * s;
        }
        return newQuaternion.set(x, y, z, w);
    }

    public static javax.vecmath.Matrix3f convert(com.jme3.math.Quaternion oldQuaternion, javax.vecmath.Matrix3f newMatrix) {
        float norm = oldQuaternion.getW() * oldQuaternion.getW() + oldQuaternion.getX() * oldQuaternion.getX() + oldQuaternion.getY() * oldQuaternion.getY() + oldQuaternion.getZ() * oldQuaternion.getZ();
        float s = (norm == 1f) ? 2f : (norm > 0f) ? 2f / norm : 0;

        // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
        // will be used 2-4 times each.
        float xs = oldQuaternion.getX() * s;
        float ys = oldQuaternion.getY() * s;
        float zs = oldQuaternion.getZ() * s;
        float xx = oldQuaternion.getX() * xs;
        float xy = oldQuaternion.getX() * ys;
        float xz = oldQuaternion.getX() * zs;
        float xw = oldQuaternion.getW() * xs;
        float yy = oldQuaternion.getY() * ys;
        float yz = oldQuaternion.getY() * zs;
        float yw = oldQuaternion.getW() * ys;
        float zz = oldQuaternion.getZ() * zs;
        float zw = oldQuaternion.getW() * zs;

        // using s=2/norm (instead of 1/norm) saves 9 multiplications by 2 here
        newMatrix.m00 = 1 - (yy + zz);
        newMatrix.m01 = (xy - zw);
        newMatrix.m02 = (xz + yw);
        newMatrix.m10 = (xy + zw);
        newMatrix.m11 = 1 - (xx + zz);
        newMatrix.m12 = (yz - xw);
        newMatrix.m20 = (xz - yw);
        newMatrix.m21 = (yz + xw);
        newMatrix.m22 = 1 - (xx + yy);

        return newMatrix;
    }

    public static com.jme3.math.Matrix3f convert(javax.vecmath.Matrix3f oldMatrix) {
        com.jme3.math.Matrix3f newMatrix = new com.jme3.math.Matrix3f();
        convert(oldMatrix, newMatrix);
        return newMatrix;
    }

    public static com.jme3.math.Matrix3f convert(javax.vecmath.Matrix3f oldMatrix, com.jme3.math.Matrix3f newMatrix) {
        newMatrix.set(0, 0, oldMatrix.m00);
        newMatrix.set(0, 1, oldMatrix.m01);
        newMatrix.set(0, 2, oldMatrix.m02);
        newMatrix.set(1, 0, oldMatrix.m10);
        newMatrix.set(1, 1, oldMatrix.m11);
        newMatrix.set(1, 2, oldMatrix.m12);
        newMatrix.set(2, 0, oldMatrix.m20);
        newMatrix.set(2, 1, oldMatrix.m21);
        newMatrix.set(2, 2, oldMatrix.m22);
        return newMatrix;
    }

    public static javax.vecmath.Matrix3f convert(com.jme3.math.Matrix3f oldMatrix) {
        javax.vecmath.Matrix3f newMatrix = new javax.vecmath.Matrix3f();
        convert(oldMatrix, newMatrix);
        return newMatrix;
    }

    public static javax.vecmath.Matrix3f convert(com.jme3.math.Matrix3f oldMatrix, javax.vecmath.Matrix3f newMatrix) {
        newMatrix.m00 = oldMatrix.get(0, 0);
        newMatrix.m01 = oldMatrix.get(0, 1);
        newMatrix.m02 = oldMatrix.get(0, 2);
        newMatrix.m10 = oldMatrix.get(1, 0);
        newMatrix.m11 = oldMatrix.get(1, 1);
        newMatrix.m12 = oldMatrix.get(1, 2);
        newMatrix.m20 = oldMatrix.get(2, 0);
        newMatrix.m21 = oldMatrix.get(2, 1);
        newMatrix.m22 = oldMatrix.get(2, 2);
        return newMatrix;
    }

    public static com.bulletphysics.linearmath.Transform convert(com.jme3.math.Transform in, com.bulletphysics.linearmath.Transform out) {
        convert(in.getTranslation(), out.origin);
        convert(in.getRotation(), out.basis);
        return out;
    }

    public static com.jme3.math.Transform convert(com.bulletphysics.linearmath.Transform in,  com.jme3.math.Transform out) {
        convert(in.origin, out.getTranslation());
        convert(in.basis, out.getRotation());
        return out;
    }

    public static IndexedMesh convert(Mesh mesh) {
        IndexedMesh jBulletIndexedMesh = new IndexedMesh();
        jBulletIndexedMesh.triangleIndexBase = ByteBuffer.allocate(mesh.getTriangleCount() * 3 * 4);
        jBulletIndexedMesh.vertexBase = ByteBuffer.allocate(mesh.getVertexCount() * 3 * 4);

        IndexBuffer indices = mesh.getIndicesAsList();
        
        FloatBuffer vertices = mesh.getFloatBuffer(Type.Position);
        vertices.rewind();

        int verticesLength = mesh.getVertexCount() * 3;
        jBulletIndexedMesh.numVertices = mesh.getVertexCount();
        jBulletIndexedMesh.vertexStride = 12; //3 verts * 4 bytes per.
        for (int i = 0; i < verticesLength; i++) {
            float tempFloat = vertices.get();
            jBulletIndexedMesh.vertexBase.putFloat(tempFloat);
        }

        int indicesLength = mesh.getTriangleCount() * 3;
        jBulletIndexedMesh.numTriangles = mesh.getTriangleCount();
        jBulletIndexedMesh.triangleIndexStride = 12; //3 index entries * 4 bytes each.
        for (int i = 0; i < indicesLength; i++) {
            jBulletIndexedMesh.triangleIndexBase.putInt(indices.get(i));
        }
        vertices.rewind();
        vertices.clear();

        return jBulletIndexedMesh;
    }

    public static Mesh convert(IndexedMesh mesh) {
        Mesh jmeMesh = new Mesh();

        jmeMesh.setBuffer(Type.Index, 3, BufferUtils.createShortBuffer(mesh.numTriangles * 3));
        jmeMesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(mesh.numVertices * 3));

        IndexBuffer indicess = jmeMesh.getIndexBuffer();
        FloatBuffer vertices = jmeMesh.getFloatBuffer(Type.Position);

        for (int i = 0; i < mesh.numTriangles * 3; i++) {
            indicess.put(i, mesh.triangleIndexBase.getInt(i * 4));
        }

        for (int i = 0; i < mesh.numVertices * 3; i++) {
            vertices.put(i, mesh.vertexBase.getFloat(i * 4));
        }
        jmeMesh.updateCounts();
        jmeMesh.updateBound();
        jmeMesh.getFloatBuffer(Type.Position).clear();

        return jmeMesh;
    }

    public static Mesh convert(HeightfieldTerrainShape heightfieldShape) {
        return null; //TODO!!
    }
}
