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
import java.nio.FloatBuffer;

public class ParticlePointMesh extends ParticleMesh {

    private static final int POS_SIZE = 3 * 4;
    private static final int COLOR_SIZE = 4 * 1;
    private static final int SIZE_SIZE = 1 * 4;
    private static final int UV_SIZE = 4 * 4;
    private static final int BYTES_PER_PARTICLE = POS_SIZE + COLOR_SIZE + SIZE_SIZE + UV_SIZE;
    private static final int FLOATS_PER_PARTICLE = BYTES_PER_PARTICLE / 4;

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

        ByteBuffer eb = BufferUtils.createByteBuffer(BYTES_PER_PARTICLE * numParticles);
        VertexBuffer vb = getBuffer(VertexBuffer.Type.InterleavedData);
        if (vb != null) {
            vb.updateData(eb);
        } else {
            vb = new VertexBuffer(VertexBuffer.Type.InterleavedData);
            vb.setupData(Usage.Stream, 1, Format.Byte, eb);
            setBuffer(vb);
        }

        if (getBuffer(VertexBuffer.Type.Position) == null) {
            VertexBuffer pb = new VertexBuffer(VertexBuffer.Type.Position);
            pb.setupData(Usage.Stream, 3, Format.Float, eb);
            pb.updateData(null);
            pb.setOffset(0);
            pb.setStride(BYTES_PER_PARTICLE);
            setBuffer(pb);

            VertexBuffer cb = new VertexBuffer(VertexBuffer.Type.Color);
            cb.setupData(Usage.Stream, 4, Format.UnsignedByte, eb);
            cb.updateData(null);
            cb.setNormalized(true);
            cb.setOffset(POS_SIZE);
            cb.setStride(BYTES_PER_PARTICLE);
            setBuffer(cb);

            VertexBuffer sb = new VertexBuffer(VertexBuffer.Type.Size);
            sb.setupData(Usage.Stream, 1, Format.Float, eb);
            sb.updateData(null);
            sb.setOffset(POS_SIZE + COLOR_SIZE);
            sb.setStride(BYTES_PER_PARTICLE);
            setBuffer(sb);

            VertexBuffer tb = new VertexBuffer(VertexBuffer.Type.TexCoord);
            tb.setupData(Usage.Stream, 4, Format.Float, eb);
            tb.updateData(null);
            tb.setOffset(POS_SIZE + COLOR_SIZE + SIZE_SIZE);
            tb.setStride(BYTES_PER_PARTICLE);
            setBuffer(tb);
        }

        updateCounts();
    }

    @Override
    public void updateParticleData(RenderManager rm, Particle[] particles, Camera cam, Matrix3f inverseRotation) {
        VertexBuffer eb = getBuffer(VertexBuffer.Type.InterleavedData);
        ByteBuffer elements = (ByteBuffer) eb.getData();
        FloatBuffer floatElements = elements.asFloatBuffer();

        float sizeScale = emitter.getWorldScale().x;

        TempVars vars = TempVars.get();
        try {
            float[] floatArray = vars.skinTangents;

            int particlesPerIteration = floatArray.length / FLOATS_PER_PARTICLE;
            int iterations = (particles.length + particlesPerIteration - 1) / particlesPerIteration;

            int particleIndex = 0;
            for (int iteration = 0; iteration < iterations; iteration++) {
                int particlesRemaining = Math.min(
                        particles.length - particleIndex,
                        particlesPerIteration);

                int floatIndex = 0;
                for (int i = 0; i < particlesRemaining; i++) {
                    Particle p = particles[particleIndex++];

                    floatArray[floatIndex++] = p.position.x;
                    floatArray[floatIndex++] = p.position.y;
                    floatArray[floatIndex++] = p.position.z;
                    floatArray[floatIndex++] = Float.intBitsToFloat(p.color.asIntABGR());
                    floatArray[floatIndex++] = p.size * sizeScale;

                    int imgX = p.imageIndex % imagesX;
                    int imgY = (p.imageIndex - imgX) / imagesY;

                    float startX = ((float) imgX) / imagesX;
                    float startY = ((float) imgY) / imagesY;
                    float endX = startX + (1f / imagesX);
                    float endY = startY + (1f / imagesY);

                    floatArray[floatIndex++] = startX;
                    floatArray[floatIndex++] = startY;
                    floatArray[floatIndex++] = endX;
                    floatArray[floatIndex++] = endY;
                }

                floatElements.put(floatArray, 0, FLOATS_PER_PARTICLE * particlesRemaining);
            }

            if (floatElements.remaining() != 0) {
                throw new IllegalStateException();
            }

            eb.updateData(elements);

            // cheating!
            rm.getRenderer().updateBufferData(eb);
        } finally {
            vars.release();
        }
    }
}
