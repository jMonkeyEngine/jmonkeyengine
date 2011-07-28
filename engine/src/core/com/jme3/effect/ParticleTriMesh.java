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

package com.jme3.effect;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import com.jme3.util.SortUtil;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class ParticleTriMesh extends ParticleMesh {

    private int imagesX = 1;
    private int imagesY = 1;
    private boolean uniqueTexCoords = false;
    private ParticleComparator comparator = new ParticleComparator();
    private ParticleEmitter emitter;
    private Particle[] particlesCopy;

    @Override
    public void initParticleData(ParticleEmitter emitter, int numParticles) {
        setMode(Mode.Triangles);

        this.emitter = emitter;

        particlesCopy = new Particle[numParticles];

        // set positions
        FloatBuffer pb = BufferUtils.createVector3Buffer(numParticles * 4);
        VertexBuffer pvb = new VertexBuffer(VertexBuffer.Type.Position);
        pvb.setupData(Usage.Stream, 3, Format.Float, pb);
        
        //if the buffer is already set only update the data
        VertexBuffer buf = getBuffer(VertexBuffer.Type.Position);
        if (buf != null) {
            buf.updateData(pb);
        } else {
            setBuffer(pvb);
        }
        
        // set colors
        ByteBuffer cb = BufferUtils.createByteBuffer(numParticles * 4 * 4);
        VertexBuffer cvb = new VertexBuffer(VertexBuffer.Type.Color);
        cvb.setupData(Usage.Stream, 4, Format.UnsignedByte, cb);
        cvb.setNormalized(true);
        
        buf = getBuffer(VertexBuffer.Type.Color);
        if (buf != null) {
            buf.updateData(cb);
        } else {
            setBuffer(cvb);
        }

        // set texcoords
        VertexBuffer tvb = new VertexBuffer(VertexBuffer.Type.TexCoord);
        FloatBuffer tb = BufferUtils.createVector2Buffer(numParticles * 4);
        
        uniqueTexCoords = false;
        for (int i = 0; i < numParticles; i++){
            tb.put(0f).put(1f);
            tb.put(1f).put(1f);
            tb.put(0f).put(0f);
            tb.put(1f).put(0f);
        }
        tb.flip();
        tvb.setupData(Usage.Static, 2, Format.Float, tb);
        
        buf = getBuffer(VertexBuffer.Type.TexCoord);
        if (buf != null) {
            buf.updateData(tb);
        } else {
            setBuffer(tvb);
        }

        // set indices
        ShortBuffer ib = BufferUtils.createShortBuffer(numParticles * 6);
        for (int i = 0; i < numParticles; i++){
            int startIdx = (i * 4);

            // triangle 1
            ib.put((short)(startIdx + 1))
              .put((short)(startIdx + 0))
              .put((short)(startIdx + 2));

            // triangle 2
            ib.put((short)(startIdx + 1))
              .put((short)(startIdx + 2))
              .put((short)(startIdx + 3));
        }
        ib.flip();
        
        VertexBuffer ivb = new VertexBuffer(VertexBuffer.Type.Index);
        ivb.setupData(Usage.Static, 3, Format.UnsignedShort, ib);
        
        buf = getBuffer(VertexBuffer.Type.Index);
        if (buf != null) {
            buf.updateData(ib);
        } else {
            setBuffer(ivb);
        }
        
    }
    
    @Override
    public void setImagesXY(int imagesX, int imagesY) {
        this.imagesX = imagesX;
        this.imagesY = imagesY;
        if (imagesX != 1 || imagesY != 1){
            uniqueTexCoords = true;
            getBuffer(VertexBuffer.Type.TexCoord).setUsage(Usage.Stream);
        }
    }

    @Override
    public void updateParticleData(Particle[] particles, Camera cam, Matrix3f inverseRotation) {
        System.arraycopy(particles, 0, particlesCopy, 0, particlesCopy.length);
        comparator.setCamera(cam);
//        Arrays.sort(particlesCopy, comparator);
//        SortUtil.qsort(particlesCopy, comparator);
        SortUtil.msort(particles, particlesCopy, comparator);
        particles = particlesCopy;

        VertexBuffer pvb = getBuffer(VertexBuffer.Type.Position);
        FloatBuffer positions = (FloatBuffer) pvb.getData();

        VertexBuffer cvb = getBuffer(VertexBuffer.Type.Color);
        ByteBuffer colors = (ByteBuffer) cvb.getData();

        VertexBuffer tvb = getBuffer(VertexBuffer.Type.TexCoord);
        FloatBuffer texcoords = (FloatBuffer) tvb.getData();

        Vector3f camUp   = cam.getUp();
        Vector3f camLeft = cam.getLeft();
        Vector3f camDir  = cam.getDirection();

        inverseRotation.multLocal(camUp);
        inverseRotation.multLocal(camLeft);
        inverseRotation.multLocal(camDir);

        boolean facingVelocity = emitter.isFacingVelocity();

        Vector3f up = new Vector3f(),
                 left = new Vector3f();

        if (!facingVelocity){
            up.set(camUp);
            left.set(camLeft);
        }

        // update data in vertex buffers
        positions.clear();
        colors.clear();
        texcoords.clear();
        Vector3f faceNormal = emitter.getFaceNormal();
        
        for (int i = 0; i < particles.length; i++){
            Particle p = particles[i];
            boolean dead = p.life == 0;
            if (dead){
                positions.put(0).put(0).put(0);
                positions.put(0).put(0).put(0);
                positions.put(0).put(0).put(0);
                positions.put(0).put(0).put(0);
                continue;
            }
            
            if (facingVelocity){
                left.set(p.velocity).normalizeLocal();
                camDir.cross(left, up);
                up.multLocal(p.size);
                left.multLocal(p.size);
            }else if (faceNormal != null){
                up.set(faceNormal).crossLocal(Vector3f.UNIT_X);
                faceNormal.cross(up, left);
                up.multLocal(p.size);
                left.multLocal(p.size);
            }else if (p.angle != 0){
                float cos = FastMath.cos(p.angle) * p.size;
                float sin = FastMath.sin(p.angle) * p.size;

                left.x = camLeft.x * cos + camUp.x * sin;
                left.y = camLeft.y * cos + camUp.y * sin;
                left.z = camLeft.z * cos + camUp.z * sin;

                up.x = camLeft.x * -sin + camUp.x * cos;
                up.y = camLeft.y * -sin + camUp.y * cos;
                up.z = camLeft.z * -sin + camUp.z * cos;
            }else{
                up.set(camUp);
                left.set(camLeft);
                up.multLocal(p.size);
                left.multLocal(p.size);
            }

            positions.put(p.position.x + left.x + up.x)
                     .put(p.position.y + left.y + up.y)
                     .put(p.position.z + left.z + up.z);

            positions.put(p.position.x - left.x + up.x)
                     .put(p.position.y - left.y + up.y)
                     .put(p.position.z - left.z + up.z);

            positions.put(p.position.x + left.x - up.x)
                     .put(p.position.y + left.y - up.y)
                     .put(p.position.z + left.z - up.z);

            positions.put(p.position.x - left.x - up.x)
                     .put(p.position.y - left.y - up.y)
                     .put(p.position.z - left.z - up.z);

            if (uniqueTexCoords){
                int imgX = p.imageIndex % imagesX;
                int imgY = (p.imageIndex - imgX) / imagesY;

                float startX = ((float) imgX) / imagesX;
                float startY = ((float) imgY) / imagesY;
                float endX   = startX + (1f / imagesX);
                float endY   = startY + (1f / imagesY);

                texcoords.put(startX).put(endY);
                texcoords.put(endX).put(endY);
                texcoords.put(startX).put(startY);
                texcoords.put(endX).put(startY);
            }

            int abgr = p.color.asIntABGR();
            colors.putInt(abgr);
            colors.putInt(abgr);
            colors.putInt(abgr);
            colors.putInt(abgr);
        }

        positions.clear();
        colors.clear();
        if (!uniqueTexCoords)
            texcoords.clear();
        else{
            texcoords.clear();
            tvb.updateData(texcoords);
        }

        // force renderer to re-send data to GPU
        pvb.updateData(positions);
        cvb.updateData(colors);
    }

}
