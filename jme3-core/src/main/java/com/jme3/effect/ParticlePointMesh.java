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
package com.jme3.effect;

import com.jme3.math.Matrix3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;
import java.nio.ByteBuffer;

public class ParticlePointMesh extends ParticleMesh {

    private static final int POS_SIZE = 3 * 4;
    private static final int COLOR_SIZE = 4 * 1;
    private static final int SIZE_SIZE = 1 * 4;
    private static final int UV_SIZE = 4 * 4;
    private static final int TOTAL_SIZE = POS_SIZE + COLOR_SIZE + SIZE_SIZE + UV_SIZE;

    private ParticleEmitter emitter;

    private int imagesX = 1;
    private int imagesY = 1;

    @Override
    public void setImagesXY(int imagesX, int imagesY) {
        this.imagesX = imagesX;
        this.imagesY = imagesY;
    }

    @Override
    public void initParticleData(ParticleEmitter emitter, int numParticles) {
        setMode(Mode.Points);

        this.emitter = emitter;

        ByteBuffer eb = BufferUtils.createByteBuffer(TOTAL_SIZE * numParticles);
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.InterleavedData);
        vb.setupData(Usage.Stream, 1, Format.Byte, eb);
        setBuffer(vb);

        VertexBuffer pb = new VertexBuffer(VertexBuffer.Type.Position);
        pb.setupData(Usage.Stream, 3, Format.Float, eb);
        pb.updateData(null);
        pb.setOffset(0);
        pb.setStride(TOTAL_SIZE);
        setBuffer(pb);

        VertexBuffer cb = new VertexBuffer(VertexBuffer.Type.Color);
        cb.setupData(Usage.Stream, 4, Format.UnsignedByte, eb);
        cb.updateData(null);
        cb.setNormalized(true);
        cb.setOffset(POS_SIZE);
        cb.setStride(TOTAL_SIZE);
        setBuffer(cb);

        VertexBuffer sb = new VertexBuffer(VertexBuffer.Type.Size);
        sb.setupData(Usage.Stream, 1, Format.Float, eb);
        sb.updateData(null);
        sb.setOffset(POS_SIZE + COLOR_SIZE);
        sb.setStride(TOTAL_SIZE);
        setBuffer(sb);

        VertexBuffer tb = new VertexBuffer(VertexBuffer.Type.TexCoord);
        tb.setupData(Usage.Stream, 4, Format.Float, eb);
        tb.updateData(null);
        tb.setOffset(POS_SIZE + COLOR_SIZE + SIZE_SIZE);
        tb.setStride(TOTAL_SIZE);
        setBuffer(tb);

        updateCounts();
    }

    @Override
    public void updateParticleData(RenderManager rm, Particle[] particles, Camera cam, Matrix3f inverseRotation) {
        VertexBuffer eb = getBuffer(VertexBuffer.Type.InterleavedData);
        ByteBuffer elements = (ByteBuffer) eb.getData();

        float sizeScale = emitter.getWorldScale().x;

        TempVars vars = TempVars.get();
        try {
            float[] temp = vars.skinTangents;
            int index = 0;

            for (int i = 0; i < particles.length; i++) {
                Particle p = particles[i];

                temp[index++] = p.position.x;
                temp[index++] = p.position.y;
                temp[index++] = p.position.z;
                temp[index++] = Float.intBitsToFloat(p.color.asIntABGR());
                temp[index++] = p.size * sizeScale;

                int imgX = p.imageIndex % imagesX;
                int imgY = (p.imageIndex - imgX) / imagesY;

                float startX = ((float) imgX) / imagesX;
                float startY = ((float) imgY) / imagesY;
                float endX = startX + (1f / imagesX);
                float endY = startY + (1f / imagesY);

                temp[index++] = startX;
                temp[index++] = startY;
                temp[index++] = endX;
                temp[index++] = endY;
            }

            elements.asFloatBuffer().put(temp, 0, (TOTAL_SIZE / 4) * particles.length).flip();

            eb.updateData(elements);

            // cheating!
            rm.getRenderer().updateBufferData(eb);
        } finally {
            vars.release();
        }
    }
}
