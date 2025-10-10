/*
 * Copyright (c) 2016-2021 jMonkeyEngine
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

import static com.jme3.util.BufferUtils.populateFromBuffer;
import static com.jme3.util.BufferUtils.setInBuffer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.*;
import com.jme3.scene.GlVertexBuffer.Type;

/**
 * Created by Nehon on 03/10/2016.
 */
public class TangentUtils {

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private TangentUtils() {
    }

    public static void generateBindPoseTangentsIfNecessary(Mesh mesh){
        if (mesh.getBuffer(GlVertexBuffer.Type.BindPosePosition) != null) {

            GlVertexBuffer tangents = mesh.getBuffer(GlVertexBuffer.Type.Tangent);
            if (tangents != null) {
                GlVertexBuffer bindTangents = new GlVertexBuffer(GlVertexBuffer.Type.BindPoseTangent);
                bindTangents.setupData(GlVertexBuffer.Usage.CpuOnly,
                        4,
                        GlVertexBuffer.Format.Float,
                        BufferUtils.clone(tangents.getData()));

                if (mesh.getBuffer(GlVertexBuffer.Type.BindPoseTangent) != null) {
                    mesh.clearBuffer(GlVertexBuffer.Type.BindPoseTangent);
                }
                mesh.setBuffer(bindTangents);
                tangents.setUsage(GlVertexBuffer.Usage.Stream);
            }
        }
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

        FloatBuffer lineVertex = BufferUtils.createFloatBuffer(vertexBuffer.limit() * 2);
        FloatBuffer lineColor = BufferUtils.createFloatBuffer(vertexBuffer.limit() / 3 * 4 * 2);

        for (int i = 0; i < vertexBuffer.limit() / 3; i++) {
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
        // lineMesh.setInterleaved();
        return lineMesh;
    }

    public static Mesh genTangentLines(Mesh mesh, float scale) {
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

        IntBuffer lineIndex = BufferUtils.createIntBuffer(vertexBuffer.limit() / 3 * 6);
        FloatBuffer lineVertex = BufferUtils.createFloatBuffer(vertexBuffer.limit() * 4);
        FloatBuffer lineColor = BufferUtils.createFloatBuffer(vertexBuffer.limit() / 3 * 4 * 4);

        boolean hasParity = mesh.getBuffer(Type.Tangent).getNumComponents() == 4;
        float tangentW = 1;

        for (int i = 0; i < vertexBuffer.limit() / 3; i++) {
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
        // lineMesh.setInterleaved();
        return lineMesh;
    }
}
