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
package com.jme3.particles.particle;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.particles.Emitter;
import com.jme3.renderer.Camera;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Iterator;

/**
 * ParticleDataTriMesh
 *
 * @author t0neg0d
 * @author Jedic
 */
public class ParticleDataTriMesh extends ParticleDataMesh {

    private BoundingBox defaultArea = new BoundingBox(new Vector3f(), 1, 1, 1);

    private int imagesX = 1;
    private int imagesY = 1;
    private boolean uniqueTexCoords = false;
    private Emitter emitter;
    private Vector3f left = new Vector3f(), tempLeft = new Vector3f();
    private Vector3f up = new Vector3f(), tempUp = new Vector3f();
    private Vector3f dir = new Vector3f();
    private Vector3f tempV3 = new Vector3f();
    private Quaternion rotStore = new Quaternion();
    private Quaternion tempQ = new Quaternion();
    private Node tempN = new Node();
    private int imgX, imgY;
    private float startX, startY, endX, endY;
	
    @Override
    public void initParticleData(Emitter emitter, int numParticles) {
        setMode(Mode.Triangles);

        this.emitter = emitter;

//        particlesCopy = new ParticleData[numParticles];

        // set positions
        // we need an extra particle so we can force the bounding box to contain us
        FloatBuffer pb = BufferUtils.createVector3Buffer((numParticles + 1) * 4 );
        Vector3f worldLoc = emitter.getWorldTranslation();
        pb.put(numParticles * 12, worldLoc.x);
        pb.put(numParticles * 12 + 1, worldLoc.y);
        pb.put(numParticles * 12 + 2, worldLoc.z);

        // if the buffer is already set only update the data
        VertexBuffer buf = getBuffer(VertexBuffer.Type.Position);
        if (buf != null) {
            buf.updateData(pb);
        } else {
            VertexBuffer pvb = new VertexBuffer(VertexBuffer.Type.Position);
            pvb.setupData(Usage.Stream, 3, Format.Float, pb);
            setBuffer(pvb);
        }
        
        // set colors
        ByteBuffer cb = BufferUtils.createByteBuffer(numParticles * 4 * 4);
        buf = getBuffer(VertexBuffer.Type.Color);
        if (buf != null) {
            buf.updateData(cb);
        } else {
            VertexBuffer cvb = new VertexBuffer(VertexBuffer.Type.Color);
            cvb.setupData(Usage.Stream, 4, Format.UnsignedByte, cb);
            cvb.setNormalized(true);
            setBuffer(cvb);
        }

        // set texcoords
        FloatBuffer tb = BufferUtils.createVector2Buffer(numParticles * 4);
        uniqueTexCoords = false;
        for (int i = 0; i < numParticles; i++){
            tb.put(0f).put(1f);
            tb.put(1f).put(1f);
            tb.put(0f).put(0f);
            tb.put(1f).put(0f);
        }
        tb.flip();
        
        buf = getBuffer(VertexBuffer.Type.TexCoord);
        if (buf != null) {
            buf.updateData(tb);
        } else {
            VertexBuffer tvb = new VertexBuffer(VertexBuffer.Type.TexCoord);
            tvb.setupData(Usage.Static, 2, Format.Float, tb);
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

        buf = getBuffer(VertexBuffer.Type.Index);
        if (buf != null) {
            buf.updateData(ib);
        } else {
            VertexBuffer ivb = new VertexBuffer(VertexBuffer.Type.Index);
            ivb.setupData(Usage.Static, 3, Format.UnsignedShort, ib);
            setBuffer(ivb);
        }
        
        updateCounts();
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
	
    public int getSpriteCols() { return this.imagesX; }
    public int getSpriteRows() { return this.imagesY; }
	
    @Override
    public void updateParticleData(ParticleData[] particles, Camera cam, Matrix3f inverseRotation) {
        VertexBuffer pvb = getBuffer(VertexBuffer.Type.Position);
        FloatBuffer positions = (FloatBuffer) pvb.getData();

        VertexBuffer cvb = getBuffer(VertexBuffer.Type.Color);
        ByteBuffer colors = (ByteBuffer) cvb.getData();

        VertexBuffer tvb = getBuffer(VertexBuffer.Type.TexCoord);
        FloatBuffer texcoords = (FloatBuffer) tvb.getData();

        // update data in vertex buffers
        positions.clear();
        colors.clear();
        texcoords.clear();
        
        for (int i = 0; i < particles.length; i++){
            ParticleData p = particles[i];
            if (p.life == 0) {
                positions.put(0).put(0).put(0);
                positions.put(0).put(0).put(0);
                positions.put(0).put(0).put(0);
                positions.put(0).put(0).put(0);
                continue;
            }
			
            switch (emitter.getBillboardMode()) {
              case Velocity:
                up.set(p.velocity).crossLocal(Vector3f.UNIT_Y).normalizeLocal();
                left.set(p.velocity).crossLocal(up).normalizeLocal();
                dir.set(p.velocity);
                break;
              case Velocity_Z_Up:
                up.set(p.velocity).crossLocal(Vector3f.UNIT_Y).normalizeLocal();
                left.set(p.velocity).crossLocal(up).normalizeLocal();
                dir.set(p.velocity);
                rotStore = tempQ.fromAngleAxis(-90* FastMath.DEG_TO_RAD, left);
                left = rotStore.mult(left);
                up = rotStore.mult(up);
                break;
              case Normal:
                emitter.getShape().setNext(p.triangleIndex);
                tempV3.set(emitter.getShape().getNextDirection());
                up.set(tempV3).crossLocal(Vector3f.UNIT_Y).normalizeLocal();
                left.set(tempV3).crossLocal(up).normalizeLocal();
                dir.set(tempV3);
                break;
              case Normal_Y_Up:
                emitter.getShape().setNext(p.triangleIndex);
                tempV3.set(emitter.getShape().getNextDirection());
                up.set(Vector3f.UNIT_Y);
                left.set(tempV3).crossLocal(up).normalizeLocal();
                dir.set(tempV3);
                break;
              case Camera:
                up.set(cam.getUp());
                left.set(cam.getLeft());
                dir.set(cam.getDirection());
                break;
              case UNIT_X:
                up.set(Vector3f.UNIT_Y);
                left.set(Vector3f.UNIT_Z);
                dir.set(Vector3f.UNIT_X);
                break;
              case UNIT_Y:
                up.set(Vector3f.UNIT_Z);
                left.set(Vector3f.UNIT_X);
                dir.set(Vector3f.UNIT_Y);
                break;
              case UNIT_Z:
                up.set(Vector3f.UNIT_X);
                left.set(Vector3f.UNIT_Y);
                dir.set(Vector3f.UNIT_Z);
                break;
              case UNIT_FORWARD:
                up.set(new Vector3f(0, 0.8f, 0.2f).crossLocal(Vector3f.UNIT_X));
                left.set(Vector3f.UNIT_X);
                dir.set(0, 0.8f, 0.2f);
                break;
            }

            up.multLocal(p.size);
            left.multLocal(p.size);

            rotStore = tempQ.fromAngleAxis(p.angles.y, left);
            left = rotStore.mult(left);
            up = rotStore.mult(up);

            rotStore = tempQ.fromAngleAxis(p.angles.x, up);
            left = rotStore.mult(left);
            up = rotStore.mult(up);

            rotStore = tempQ.fromAngleAxis(p.angles.z, dir);
            left = rotStore.mult(left);
            up = rotStore.mult(up);

            if (emitter.getParticlesFollowEmitter()) {
              tempV3.set(p.position);
            } else {
              tempV3.set(p.position);
              //tempV3.set(p.position).subtractLocal(emitter.getWorldTranslation().subtract(p.initialPosition).divide(8f));
              //tempV3 = particles.getNode().getWorldRotation().inverse().multLocal(tempV3);
            }

            positions.put(tempV3.x + left.x + up.x)
                           .put(tempV3.y + left.y + up.y)
                           .put(tempV3.z + left.z + up.z);

                  positions.put(tempV3.x - left.x + up.x)
                           .put(tempV3.y - left.y + up.y)
                           .put(tempV3.z - left.z + up.z);

                  positions.put(tempV3.x + left.x - up.x)
                           .put(tempV3.y + left.y - up.y)
                           .put(tempV3.z + left.z - up.z);

                  positions.put(tempV3.x - left.x - up.x)
                           .put(tempV3.y - left.y - up.y)
                           .put(tempV3.z - left.z - up.z);

            if (uniqueTexCoords){
              imgX = p.spriteCol;
              imgY = p.spriteRow;

              startX = 1f/imagesX*imgX;
              startY = 1f/imagesY*imgY;
              endX   = startX + 1f/imagesX;
              endY   = startY + 1f/imagesY;

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
		
	//	this.setBuffer(VertexBuffer.Type.Position, 3, positions);
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
		
		  updateBound();

		  // this is needed because particles can be a ways from the emitter and new particles won't emit
      //getBound().mergeLocal(defaultArea);
    }

	@Override
	public void extractTemplateFromMesh(Mesh mesh) {
    Iterator<VertexBuffer> itr = mesh.getBufferList().iterator();
    while (itr.hasNext()) {
      this.setBuffer(itr.next().clone());
    }
  }
}