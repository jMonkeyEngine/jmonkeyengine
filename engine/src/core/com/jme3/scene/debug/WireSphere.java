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

import com.jme3.bounding.BoundingSphere;
import com.jme3.math.FastMath;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class WireSphere extends Mesh {

    private static final int samples = 30;
    private static final int zSamples = 10;

    public WireSphere() {
        this(1);
    }

    public WireSphere(float radius) {
        updatePositions(radius);
        ShortBuffer ib = BufferUtils.createShortBuffer(samples * 2 * 2 + zSamples * samples * 2 /*+ 3 * 2*/);
        setBuffer(Type.Index, 2, ib);

//        ib.put(new byte[]{
//            (byte) 0, (byte) 1,
//            (byte) 2, (byte) 3,
//            (byte) 4, (byte) 5,
//        });

//        int curNum = 3 * 2;
        int curNum = 0;
        for (int j = 0; j < 2 + zSamples; j++) {
            for (int i = curNum; i < curNum + samples - 1; i++) {
                ib.put((short) i).put((short) (i + 1));
            }
            ib.put((short) (curNum + samples - 1)).put((short) curNum);
            curNum += samples;
        }

        setMode(Mode.Lines);

        updateBound();
        updateCounts();
    }

    public void updatePositions(float radius) {
        VertexBuffer pvb = getBuffer(Type.Position);
        FloatBuffer pb;

        if (pvb == null) {
            pvb = new VertexBuffer(Type.Position);
            pb = BufferUtils.createVector3Buffer(samples * 2 + samples * zSamples /*+ 6 * 3*/);
            pvb.setupData(Usage.Dynamic, 3, Format.Float, pb);
            setBuffer(pvb);
        } else {
            pb = (FloatBuffer) pvb.getData();
        }

        pb.rewind();

        // X axis
//        pb.put(radius).put(0).put(0);
//        pb.put(-radius).put(0).put(0);
//
//        // Y axis
//        pb.put(0).put(radius).put(0);
//        pb.put(0).put(-radius).put(0);
//
//        // Z axis
//        pb.put(0).put(0).put(radius);
//        pb.put(0).put(0).put(-radius);

        float rate = FastMath.TWO_PI / (float) samples;
        float angle = 0;
        for (int i = 0; i < samples; i++) {
            float x = radius * FastMath.cos(angle);
            float y = radius * FastMath.sin(angle);
            pb.put(x).put(y).put(0);
            angle += rate;
        }

        angle = 0;
        for (int i = 0; i < samples; i++) {
            float x = radius * FastMath.cos(angle);
            float y = radius * FastMath.sin(angle);
            pb.put(0).put(x).put(y);
            angle += rate;
        }

        float zRate = (radius * 2) / (float) (zSamples);
        float zHeight = -radius + (zRate / 2f);


        float rb = 1f / zSamples;
        float b = rb / 2f;

        for (int k = 0; k < zSamples; k++) {
            angle = 0;
            float scale = FastMath.sin(b * FastMath.PI);
            for (int i = 0; i < samples; i++) {
                float x = radius * FastMath.cos(angle);
                float y = radius * FastMath.sin(angle);

                pb.put(x * scale).put(zHeight).put(y * scale);

                angle += rate;
            }
            zHeight += zRate;
            b += rb;
        }
    }

    /**
     * Create a WireSphere from a BoundingSphere
     *
     * @param bsph
     *     BoundingSphere used to create the WireSphere
     *
     */
    public void fromBoundingSphere(BoundingSphere bsph) {
        updatePositions(bsph.getRadius());
    }
}
