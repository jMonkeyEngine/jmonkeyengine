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

package com.jme3.scene.debug;

import com.jme3.bounding.BoundingBox;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;

public class WireBox extends Mesh {

    public WireBox(){
        this(1,1,1);
    }
    
    public WireBox(float xExt, float yExt, float zExt){
        updatePositions(xExt,yExt,zExt);
        setBuffer(Type.Index, 2,
                new short[]{
                     0, 1,
                     1, 2,
                     2, 3,
                     3, 0,

                     4, 5,
                     5, 6,
                     6, 7,
                     7, 4,

                     0, 4,
                     1, 5,
                     2, 6,
                     3, 7,
                }
        );
        setMode(Mode.Lines);

        updateCounts();
    }

    public void updatePositions(float xExt, float yExt, float zExt){
        VertexBuffer pvb = getBuffer(Type.Position);
        FloatBuffer pb;
        if (pvb == null){
            pvb = new VertexBuffer(Type.Position);
            pb = BufferUtils.createVector3Buffer(8);
            pvb.setupData(Usage.Dynamic, 3, Format.Float, pb);
            setBuffer(pvb);
        }else{
            pb = (FloatBuffer) pvb.getData();
            pvb.updateData(pb);
        }
        pb.rewind();
        pb.put(
            new float[]{
                -xExt, -yExt,  zExt,
                 xExt, -yExt,  zExt,
                 xExt,  yExt,  zExt,
                -xExt,  yExt,  zExt,

                -xExt, -yExt, -zExt,
                 xExt, -yExt, -zExt,
                 xExt,  yExt, -zExt,
                -xExt,  yExt, -zExt,
            }
        );
        updateBound();
    }

    public void fromBoundingBox(BoundingBox bbox){
        updatePositions(bbox.getXExtent(), bbox.getYExtent(), bbox.getZExtent());
    }

}
