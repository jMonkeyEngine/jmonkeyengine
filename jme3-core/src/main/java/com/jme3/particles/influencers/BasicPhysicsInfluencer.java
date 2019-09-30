/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.particles.influencers;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.particles.particle.ParticleData;
import com.jme3.scene.Geometry;

/**
 * BasicPhysicsInfluencer
 * A basic physics influencer which allows particles to bounce off a given geometry
 *
 * @author Jedic
 */
public class BasicPhysicsInfluencer extends ParticleInfluencer {

  private Ray tempRay = new Ray();
  private CollisionResults tempResults = new CollisionResults();
  private Vector3f temp = new Vector3f();
  private float dampening = 0.4f;
  private float bounce = 1.0f;

  private Geometry collisionMesh;

  public BasicPhysicsInfluencer(Geometry geo) {
    collisionMesh = geo;
  }

  @Override
  public void update(ParticleData p, float tpf) {
    tempRay.origin.set(p.position);
    tempRay.direction.set(p.velocity);
    float length = tempRay.direction.length();
    tempRay.direction.normalizeLocal();
    tempRay.limit = length * tpf;
    tempResults.clear();

    temp.set(tempRay.direction);
    if (emitter.getParticlesFollowEmitter()) {
      emitter.getWorldTransform().transformVector(tempRay.origin, tempRay.origin);
      emitter.getWorldRotation().mult(tempRay.direction, temp);
    }
    tempRay.direction.set(temp);


    collisionMesh.collideWith(tempRay, tempResults);

    // 𝑟=𝑑−2(𝑑⋅𝑛)𝑛
    if (tempResults.size() > 0) {
      for (int i = 0; i < tempResults.size(); i++) {
        CollisionResult result = tempResults.getCollision(i);
        float dot = result.getContactNormal().dot(temp);
        if (dot > 0) {
          p.velocity.set(temp.subtract(result.getContactNormal().mult(dot * 2.0f * bounce)));
          p.velocity.multLocal(length * dampening);

          if (emitter.getParticlesFollowEmitter()) {
            emitter.getWorldRotation().inverse().mult(p.velocity, p.velocity);
          }
          //emitter.getWorldTransform().invert().transformVector(p.velocity, p.velocity);
          //p.position.set(result.getContactPoint());
          return;
        }
      }
    }
  }

  /**
   * Gets the amount the velocity is reduced after every collision
   * @return the dampened amount 0-1
   */
  public float getDampening() {
    return dampening;
  }

  /**
   * Sets the dampening amount the velocity is reduced to after a collision
   * @param dampening
   */
  public void setDampening(float dampening) {
    this.dampening = dampening;
  }

  /**
   * The amount the particles bounces off the geometry
   * @return
   */
  public float getBounce() {
    return bounce;
  }

  /**
   * Sets the amount the particle bounces off the geometry
   * @param bounce
   */
  public void setBounce(float bounce) {
    this.bounce = bounce;
  }

  /**
   * Gets the collision geometry we bounce off of
   * @return
   */
  public Geometry getCollisionMesh() {
    return collisionMesh;
  }

  /**
   * Sets the collision geometry we bounce off of
   * @param collisionMesh
   */
  public void setCollisionMesh(Geometry collisionMesh) {
    this.collisionMesh = collisionMesh;
  }

  @Override
  public void initialize(ParticleData p) {

  }

  @Override
  public void reset(ParticleData p) {

  }
}
