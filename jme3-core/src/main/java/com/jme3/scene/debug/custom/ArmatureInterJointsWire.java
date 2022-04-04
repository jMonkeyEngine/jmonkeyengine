package com.jme3.scene.debug.custom;

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


import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;

import java.nio.FloatBuffer;

/**
 * A class that displays a dotted line between a bone tail and its children's heads.
 *
 * @author Marcin Roguski (Kaelthas)
 */
public class ArmatureInterJointsWire extends Mesh {
    final private Vector3f tmp = new Vector3f();


    public ArmatureInterJointsWire(Vector3f start, Vector3f[] ends) {
        setMode(Mode.Lines);
        updateGeometry(start, ends);
    }

    /**
     * For serialization only. Do not use.
     */
    protected ArmatureInterJointsWire() {
    }

    protected void updateGeometry(Vector3f start, Vector3f[] ends) {
        float[] pos = new float[ends.length * 3 + 3];
        pos[0] = start.x;
        pos[1] = start.y;
        pos[2] = start.z;
        int index;
        for (int i = 0; i < ends.length; i++) {
            index = i * 3 + 3;
            pos[index] = ends[i].x;
            pos[index + 1] = ends[i].y;
            pos[index + 2] = ends[i].z;
        }
        setBuffer(Type.Position, 3, pos);

        float[] texCoord = new float[ends.length * 2 + 2];
        texCoord[0] = 0;
        texCoord[1] = 0;
        for (int i = 0; i < ends.length * 2; i++) {
            texCoord[i + 2] = tmp.set(start).subtractLocal(ends[i / 2]).length();
        }
        setBuffer(Type.TexCoord, 2, texCoord);

        float[] normal = new float[ends.length * 3 + 3];
        for (int i = 0; i < ends.length * 3 + 3; i += 3) {
            normal[i] = start.x;
            normal[i + 1] = start.y;
            normal[i + 2] = start.z;
        }
        setBuffer(Type.Normal, 3, normal);

        short[] id = new short[ends.length * 2];
        index = 1;
        for (int i = 0; i < ends.length * 2; i += 2) {
            id[i] = 0;
            id[i + 1] = (short) (index);
            index++;
        }
        setBuffer(Type.Index, 2, id);
        updateBound();
    }

    /**
     * Update the start and end points of the line.
     * 
     * @param start location vector (not null, unaffected)
     * @param ends array of location vectors (not null, unaffected)
     */
    public void updatePoints(Vector3f start, Vector3f[] ends) {
        VertexBuffer posBuf = getBuffer(Type.Position);
        FloatBuffer fb = (FloatBuffer) posBuf.getData();
        fb.rewind();
        fb.put(start.x).put(start.y).put(start.z);
        for (int i = 0; i < ends.length; i++) {
            fb.put(ends[i].x);
            fb.put(ends[i].y);
            fb.put(ends[i].z);
        }
        posBuf.updateData(fb);

        VertexBuffer normBuf = getBuffer(Type.Normal);
        fb = (FloatBuffer) normBuf.getData();
        fb.rewind();
        for (int i = 0; i < ends.length * 3 + 3; i += 3) {
            fb.put(start.x);
            fb.put(start.y);
            fb.put(start.z);
        }
        normBuf.updateData(fb);
    }

}
