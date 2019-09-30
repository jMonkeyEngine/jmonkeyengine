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

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.particles.Emitter;
import com.jme3.particles.influencers.ParticleInfluencer;
import com.jme3.particles.valuetypes.ValueType;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * ParticleData
 * Info used to define a particle and it's attributes over time
 *
 * @author t0neg0d
 * @author jme3
 */
public class ParticleData {

  /**
   * ParticleData velocity.
   */
  public final Vector3f velocity = new Vector3f();
  /**
   * Current particle position
   */
  public final Vector3f position = new Vector3f();
  /**
   * ParticleData color
   */
  public final ColorRGBA startColor = new ColorRGBA(1, 1, 1, 1);
  public final ColorRGBA color = new ColorRGBA(0, 0, 0, 0);
  /**
   * The position of the particles when the particle was released.
   */
  public final Vector3f initialPosition = new Vector3f();
  public final Vector3f initialVelocity = new Vector3f();
  public final Vector3f randomOffset = new Vector3f();
  /**
   * The parent particle particles
   */
  public Emitter emitter;
  /**
   * The particles index
   */
  public int index;
  /**
   * Used for when we do random between two curve calculations
   */
  public float randomValue = 0.0f;
  /**
   * The force at which the particle was emitted
   */
  public Vector3f force;
  /**
   * ParticleData start size or radius.
   */
  public float startSize = 1f;
  /**
   * ParticleData size or radius.
   */
  public float size = 1f;
  /**
   * ParticleData remaining life, in seconds.
   */
  public float life;
  /**
   * The initial particle life
   */
  public float startlife;
  /**
   * The current percentage of the completed lifecycle updated every frame
   * IE (startlife - life)/startlife
   */
  public float percentLife;
  /**
   * ParticleData rotation angle per axis (in radians).
   */
  public Vector3f angles = new Vector3f();
  /**
   * ParticleData rotation angle speed per axis (in radians).
   */
  public Vector3f rotationSpeed = new Vector3f();

  /**
   * The index of the particles shape's mesh triangle the particle was emitted
   * from
   */
  public int triangleIndex;
  /**
   * ParticleData image index.
   */
  public int spriteCol = 0, spriteRow = 0;
  /**
   * The state of the particle
   */
  public boolean active = false;

  /**
   * Used for particle trails
   */
  public LinkedList<ParticleTrailPoint> trailSegments = new LinkedList<>();

  /**
   * A strage facility for per-particle data used by influencers
   */
  Map<String, Object> data = new HashMap();

  /**
   * Sets data to store with the particle
   *
   * @param key The data's map key
   * @param data The data
   */
  public void setData(String key, Object data) {
    this.data.put(key, data);
  }

  /**
   * Returns the stored per-particle data
   *
   * @param key The data's map key
   * @return The data
   */
  public Object getData(String key) {
    return this.data.get(key);
  }

  public void update(float tpf) {
    if (!emitter.getUseStaticParticles()) {
      life -= tpf;
      if (life <= 0) {
        reset();
        return;
      }
      percentLife = 1.0f * (startlife - life) / startlife;
    }
    for (ParticleInfluencer influencer : emitter.getInfluencerMap()) {
      influencer.update(this, tpf);
    }

    position.x += velocity.x * tpf;
    position.y += velocity.y * tpf;
    position.z += velocity.z * tpf;
  }

  /**
   * Called once per particle use when the particle is emitted
   */
  public void initialize(ValueType lifeMin, ValueType lifeMax) {
    float blendAmount = emitter.getCurrentDuration() / emitter.getDuration();
    emitter.incActiveParticleCount();
    active = true;
    percentLife = 0;
    startSize = emitter.getStartSize().getValue(blendAmount, randomValue);
    size = startSize;
    randomValue = FastMath.nextRandomFloat();
    trailSegments.clear();
    //startlife = (emitter.getLifeMax() - emitter.getLifeMin()) * FastMath.nextRandomFloat() + emitter.getLifeMin();
    float minLife = lifeMin.getValue(blendAmount, randomValue);
    startlife = (lifeMax.getValue(blendAmount, randomValue) - minLife) * FastMath.nextRandomFloat() + minLife;
    life = startlife;
    float emitSpeed = emitter.getStartSpeed().getValue(blendAmount, randomValue);
    emitter.getShape().setNext();
    triangleIndex = emitter.getShape().getIndex();
    if (!emitter.getUseRandomEmissionPoint()) {
      position.set(
              emitter.getShape().getNextTranslation());
    } else {
      randomOffset.set(emitter.getShape().getRandomTranslation());
      position.set(
              emitter.getShape().getNextTranslation().add(randomOffset));
    }

		velocity.set(
			emitter.getShape().getNextDirection().x*emitSpeed,
			emitter.getShape().getNextDirection().y*emitSpeed,
			emitter.getShape().getNextDirection().z*emitSpeed
		);

    if (!emitter.getParticlesFollowEmitter()) {
      emitter.getWorldRotation().mult(velocity, velocity);
      initialPosition.set(emitter.getWorldTranslation());
      initialPosition.addLocal(position);
      position.set(initialPosition);
    } else {
      initialPosition.set(position);
    }

    initialVelocity.set(velocity);
    //velocity.set(force.clone());


    emitter.getStartRotation().getValue3f(blendAmount, randomValue, angles);

    // set initial color
    emitter.getStartColor().getValueColor(blendAmount, randomValue, startColor);
    color.set(startColor);

    for (ParticleInfluencer influencer : emitter.getInfluencerMap()) {
      influencer.initialize(this);
    }
  }

  /**
   * Called once per particle use when the particle finishes it's life cycle
   */
  public void reset() {
    active = false;
    position.zero();
    velocity.zero();
    trailSegments.clear();
    if (emitter.getActiveParticleCount() > 0) {
      emitter.decActiveParticleCount();
    }
    for (ParticleInfluencer influencer : emitter.getInfluencerMap()) {
      influencer.reset(this);
    }
    emitter.setNextIndex(index);
  }
}