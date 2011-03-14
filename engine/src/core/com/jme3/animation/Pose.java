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

package com.jme3.animation;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.io.Serializable;
import java.nio.FloatBuffer;

/**
 * A pose is a list of offsets that say where a mesh verticles should be for this pose.
 */
public final class Pose implements Serializable, Savable {

    private static final long serialVersionUID = 1L;

    private String name;
    private int targetMeshIndex;

    private Vector3f[] offsets;
    private int[] indices;

    private transient final Vector3f tempVec  = new Vector3f();
    private transient final Vector3f tempVec2 = new Vector3f();

    public Pose(String name, int targetMeshIndex, Vector3f[] offsets, int[] indices){
        this.name = name;
        this.targetMeshIndex = targetMeshIndex;
        this.offsets = offsets;
        this.indices = indices;
    }

    public int getTargetMeshIndex(){
        return targetMeshIndex;
    }


    /**
     * Applies the offsets of this pose to the vertex buffer given by the blend factor.
     *
     * @param blend Blend factor, 0 = no change to vert buf, 1 = apply full offsets
     * @param vertbuf Vertex buffer to apply this pose to
     */
    public void apply(float blend, FloatBuffer vertbuf){
        for (int i = 0; i < indices.length; i++){
            Vector3f offset = offsets[i];
            int vertIndex   = indices[i];

            tempVec.set(offset).multLocal(blend);

            // aquire vert
            BufferUtils.populateFromBuffer(tempVec2, vertbuf, vertIndex);

            // add offset multiplied by factor
            tempVec2.addLocal(tempVec);

            // write modified vert
            BufferUtils.setInBuffer(tempVec2, vertbuf, vertIndex);
        }
    }

    public void write(JmeExporter e) throws IOException {
        OutputCapsule out = e.getCapsule(this);
        out.write(name, "name", "");
        out.write(targetMeshIndex, "meshIndex", -1);
        out.write(offsets, "offsets", null);
        out.write(indices, "indices", null);
    }

    public void read(JmeImporter i) throws IOException {
        InputCapsule in = i.getCapsule(this);
        name = in.readString("name", "");
        targetMeshIndex = in.readInt("meshIndex", -1);
        offsets = (Vector3f[]) in.readSavableArray("offsets", null);
        indices = in.readIntArray("indices", null);
    }
}
