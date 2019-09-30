package com.jme3.particles.particle;

import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.particles.Emitter;
import com.jme3.renderer.Camera;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ParticleDataTrails extends ParticleDataMesh {

  private Emitter emitter;
  private int segmentsPerParticle = 100;
  private Vector3f tempOne = new Vector3f();
  private Vector3f left = new Vector3f();
  private Vector3f up = new Vector3f();
  private Vector3f dir = new Vector3f();
  private Vector3f tempV3 = new Vector3f();
  private Quaternion rotStore = new Quaternion();
  private Quaternion tempQ = new Quaternion();

  @Override
  public void extractTemplateFromMesh(Mesh mesh) {

  }

  @Override
  public void initParticleData(Emitter emitter, int particles) {
    setMode(Mesh.Mode.Triangles);
    this.emitter = emitter;

    // mesh setup
    // 1 -

    int numSegments = segmentsPerParticle * particles;

    // set positions
    FloatBuffer pb = BufferUtils.createVector3Buffer(numSegments * 4);
    // if the buffer is already set only update the data
    VertexBuffer buf = getBuffer(VertexBuffer.Type.Position);
    if (buf != null) {
      buf.updateData(pb);
    } else {
      VertexBuffer pvb = new VertexBuffer(VertexBuffer.Type.Position);
      pvb.setupData(VertexBuffer.Usage.Stream, 3, VertexBuffer.Format.Float, pb);
      setBuffer(pvb);
    }

    // set colors
    ByteBuffer cb = BufferUtils.createByteBuffer(numSegments * 4 * 4);
    buf = getBuffer(VertexBuffer.Type.Color);
    if (buf != null) {
      buf.updateData(cb);
    } else {
      VertexBuffer cvb = new VertexBuffer(VertexBuffer.Type.Color);
      cvb.setupData(VertexBuffer.Usage.Stream, 4, VertexBuffer.Format.UnsignedByte, cb);
      cvb.setNormalized(true);
      setBuffer(cvb);
    }

    // set texcoords
    FloatBuffer tb = BufferUtils.createVector2Buffer(numSegments * 4);
    //uniqueTexCoords = false;
    for (int i = 0; i < numSegments; i++){
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
      tvb.setupData(VertexBuffer.Usage.Stream, 2, VertexBuffer.Format.Float, tb);
      setBuffer(tvb);
    }

    // set indices
    IntBuffer ib = BufferUtils.createIntBuffer(numSegments * 6);
    for (int i = 0; i < numSegments; i++){
      int startIdx = (i * 4);

      // triangle 1
      ib.put(startIdx + 1)
          .put(startIdx + 0)
          .put(startIdx + 2);

      // triangle 2
      ib.put(startIdx + 1)
          .put(startIdx + 2)
          .put(startIdx + 3);
    }
    ib.flip();

    buf = getBuffer(VertexBuffer.Type.Index);
    if (buf != null) {
      buf.updateData(ib);
    } else {
      VertexBuffer ivb = new VertexBuffer(VertexBuffer.Type.Index);
      ivb.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.UnsignedInt, ib);
      setBuffer(ivb);
    }

    updateCounts();
  }

  @Override
  public void setImagesXY(int imagesX, int imagesY) {

  }

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

    // zero out all position data
    for (int i = 0; i < particles.length; i++) {
      // zero trail
      for (int j = 0; j < segmentsPerParticle; j++) {
        int index = 12 * (segmentsPerParticle * i + j);
        positions.position(index);
        positions.put(0.0f).put(0.0f).put(0.0f);
        positions.put(0.0f).put(0.0f).put(0.0f);
        positions.put(0.0f).put(0.0f).put(0.0f);
        positions.put(0.0f).put(0.0f).put(0.0f);

      }
    }


    // if no trail data - set new trail point.. draw one segment from that point to us
    // if one point - check if distance is far enough for another segment.... add one if it is... draw segments + last one to us
    int index = 0;
    for (int i = 0; i < particles.length; i++) {
      ParticleData p = particles[i];
      if (p.life <= 0) {
        continue;
      }
      // now draw the segments

      // face towards our velocity


      float size = p.size;

      // render all segments
      ParticleTrailPoint last = null;
      ParticleTrailPoint current = p.trailSegments.size() > 0 ? p.trailSegments.getFirst() : null;
      for (int j = 1; j < p.trailSegments.size(); j++, index += 12) {
        last = current;
        current = p.trailSegments.get(j);

        up.set(cam.getUp());
        left.set(cam.getLeft());
        dir.set(cam.getDirection());
        drawQuad(index, positions, p, last, current);


        int abgr = last.color.asIntABGR();
        colors.putInt(abgr);
        colors.putInt(abgr);
        abgr = current.color.asIntABGR();
        colors.putInt(abgr);
        colors.putInt(abgr);
      }

      if (current != null) {
        // draw from the point to the current particle position
        //drawQuad(positions, current.position, current.velocity, p.position, p.velocity, size, size);

      }


//      if (particles.getParticlesFollowEmitter()) {
//        tempV3.set(p.position);
//      } else {
//        tempV3.set(p.position).subtractLocal(particles.getNode().getWorldTranslation().subtract(p.initialPosition).divide(8f));
//      }


    }


    pvb.updateData(positions);
    cvb.updateData(colors);
    updateBound();

  }

  private void drawQuad(int index, FloatBuffer positions, ParticleData p, ParticleTrailPoint last, ParticleTrailPoint current) {
    Vector3f test = left.clone();

    positions.position(index);

    if (emitter.getParticlesFollowEmitter()) {
      tempV3.set(last.position);
    } else {
      tempV3.set(last.position).subtractLocal(emitter.getWorldTranslation().subtract(p.initialPosition).divide(8f));
      //tempV3 = particles.getNode().getWorldRotation().inverse().multLocal(tempV3);
    }

    up.set(last.velocity).crossLocal(test).normalizeLocal();
    left.set(last.velocity).crossLocal(up).normalizeLocal();
    up.multLocal(last.size);
    left.multLocal(last.size);
    positions.put(tempV3.x + left.x)// + up.x)
        .put(tempV3.y + left.y )//+ up.y)
        .put(tempV3.z + left.z );//+ up.z);

    positions.put(tempV3.x - left.x)// up.x)
        .put(tempV3.y - left.y)//+ up.y)
        .put(tempV3.z - left.z);// + up.z);


    if (emitter.getParticlesFollowEmitter()) {
      tempV3.set(current.position);
    } else {
      tempV3.set(current.position).subtractLocal(emitter.getWorldTranslation().subtract(p.initialPosition).divide(8f));
      //tempV3 = particles.getNode().getWorldRotation().inverse().multLocal(tempV3);
    }

    up.set(current.velocity).crossLocal(test).normalizeLocal();
    left.set(current.velocity).crossLocal(up).normalizeLocal();
    up.multLocal(current.size);
    left.multLocal(current.size);
    positions.put(tempV3.x + left.x)// - up.x)
        .put(tempV3.y + left.y)// - up.y)
        .put(tempV3.z + left.z);// - up.z);

    positions.put(tempV3.x - left.x)// - up.x)
        .put(tempV3.y - left.y)// - up.y)
        .put(tempV3.z - left.z);// - up.z);

  }

}
