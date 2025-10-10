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
import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.GlVertexBuffer.Format;
import com.jme3.scene.GlVertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class ParticlePointMesh extends ParticleMesh {

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

        // set positions
        FloatBuffer pb = BufferUtils.createVector3Buffer(numParticles);

        // if the buffer is already set only update the data
        GlVertexBuffer buf = getBuffer(GlVertexBuffer.Type.Position);
        if (buf != null) {
            buf.updateData(pb);
        } else {
            GlVertexBuffer pvb = new GlVertexBuffer(GlVertexBuffer.Type.Position);
            pvb.setupData(Usage.Stream, 3, Format.Float, pb);
            setBuffer(pvb);
        }

        // set colors
        ByteBuffer cb = BufferUtils.createByteBuffer(numParticles * 4);

        buf = getBuffer(GlVertexBuffer.Type.Color);
        if (buf != null) {
            buf.updateData(cb);
        } else {
            GlVertexBuffer cvb = new GlVertexBuffer(GlVertexBuffer.Type.Color);
            cvb.setupData(Usage.Stream, 4, Format.UnsignedByte, cb);
            cvb.setNormalized(true);
            setBuffer(cvb);
        }

        // set sizes
        FloatBuffer sb = BufferUtils.createFloatBuffer(numParticles);

        buf = getBuffer(GlVertexBuffer.Type.Size);
        if (buf != null) {
            buf.updateData(sb);
        } else {
            GlVertexBuffer svb = new GlVertexBuffer(GlVertexBuffer.Type.Size);
            svb.setupData(Usage.Stream, 1, Format.Float, sb);
            setBuffer(svb);
        }

        // set UV-scale
        FloatBuffer tb = BufferUtils.createFloatBuffer(numParticles * 4);

        buf = getBuffer(GlVertexBuffer.Type.TexCoord);
        if (buf != null) {
            buf.updateData(tb);
        } else {
            GlVertexBuffer tvb = new GlVertexBuffer(GlVertexBuffer.Type.TexCoord);
            tvb.setupData(Usage.Stream, 4, Format.Float, tb);
            setBuffer(tvb);
        }

        updateCounts();
    }

    @Override
    public void updateParticleData(Particle[] particles, Camera cam, Matrix3f inverseRotation) {
        GlVertexBuffer pvb = getBuffer(GlVertexBuffer.Type.Position);
        FloatBuffer positions = (FloatBuffer) pvb.getData();

        GlVertexBuffer cvb = getBuffer(GlVertexBuffer.Type.Color);
        ByteBuffer colors = (ByteBuffer) cvb.getData();

        GlVertexBuffer svb = getBuffer(GlVertexBuffer.Type.Size);
        FloatBuffer sizes = (FloatBuffer) svb.getData();

        GlVertexBuffer tvb = getBuffer(GlVertexBuffer.Type.TexCoord);
        FloatBuffer texcoords = (FloatBuffer) tvb.getData();

        float sizeScale = emitter.getWorldScale().x;

        // update data in vertex buffers
        positions.rewind();
        colors.rewind();
        sizes.rewind();
        texcoords.rewind();
        
        for (int i = 0; i < particles.length; i++) {
            Particle p = particles[i];

            positions.put(p.position.x)
                     .put(p.position.y)
                     .put(p.position.z);

            sizes.put(p.size * sizeScale);
            colors.putInt(p.color.asIntABGR());

            int imgX = p.imageIndex % imagesX;
            int imgY = p.imageIndex / imagesX;

            float startX = ((float) imgX) / imagesX;
            float startY = ((float) imgY) / imagesY;
            float endX = startX + (1f / imagesX);
            float endY = startY + (1f / imagesY);

            texcoords.put(startX).put(startY).put(endX).put(endY);
        }
        
        positions.flip();
        colors.flip();
        sizes.flip();
        texcoords.flip();

        // force renderer to re-send data to GPU
        pvb.updateData(positions);
        cvb.updateData(colors);
        svb.updateData(sizes);
        tvb.updateData(texcoords);
    }
}
