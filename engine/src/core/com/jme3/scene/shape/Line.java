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

package com.jme3.scene.shape;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * A simple line implementation with a start and an end.
 * 
 * @author Brent Owens
 */
public class Line extends Mesh {

    private Vector3f start;
    private Vector3f end;
    
    public Line() {
    }

    public Line(Vector3f start, Vector3f end) {
        setMode(Mode.Lines);
        updateGeometry(start, end);
    }

    protected void updateGeometry(Vector3f start, Vector3f end) {
        this.start = start;
        this.end = end;
        setBuffer(Type.Position, 3, new float[]{start.x,    start.y,    start.z,
                                                end.x,      end.y,      end.z,});


        setBuffer(Type.TexCoord, 2, new float[]{0, 0,
                                                1, 1});

        setBuffer(Type.Normal, 3, new float[]{0, 0, 1,
                                              0, 0, 1});

        setBuffer(Type.Index, 3, new short[]{0, 1});

        updateBound();
    }

    /**
     * Update the start and end points of the line.
     */
    public void updatePoints(Vector3f start, Vector3f end) {
        VertexBuffer posBuf = getBuffer(Type.Position);

        FloatBuffer fb = (FloatBuffer) posBuf.getData();

        fb.put(start.x).put(start.y).put(start.z);
        fb.put(end.x).put(end.y).put(end.z);
        
        posBuf.updateData(fb);
        
        updateBound();
    }

    public Vector3f getEnd() {
        return end;
    }

    public Vector3f getStart() {
        return start;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);

        out.write(start, "startVertex", null);
        out.write(end, "endVertex", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);

        start = (Vector3f) in.readSavable("startVertex", null);
        end = (Vector3f) in.readSavable("endVertex", null);
    }
}
