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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.particles.particle.ParticleData;

import java.io.IOException;

/**
 * Random Module
 * Adds random noise into the particle position. This works really well for things like fire.
 *
 * @author Jedic
 */
public class RandomInfluencer extends ParticleInfluencer {

  private float chance = .5f;
  private float magnitude = 0.05f;
  private float strength = 1;

  // Temp Variables
  private transient Vector3f temp = new Vector3f();
  private transient Vector3f velocityStore = new Vector3f();

  @Override
  public ParticleInfluencer clone() {
    RandomInfluencer clone = (RandomInfluencer)super.clone();
    return clone;
  }

  @Override
  public void update(ParticleData p, float tpf) {
    if (enabled) {
      if (FastMath.rand.nextFloat() > 1-(chance+tpf)) {
        temp.set(FastMath.nextRandomFloat(),
            FastMath.nextRandomFloat(),
            FastMath.nextRandomFloat()
        );
        temp.multLocal(2f);
        temp.subtractLocal(1f, 1f, 1f);
        temp.multLocal(strength);
        temp.addLocal(p.position);
        p.position.interpolateLocal(temp, magnitude);
      }
    }
  }

  public float getChance() {
    return chance;
  }

  public void setChance(float chance) {
    this.chance = chance;
  }

  public float getMagnitude() {
    return magnitude;
  }

  public void setMagnitude(float magnitude) {
    this.magnitude = magnitude;
  }

  public float getStrength() {
    return strength;
  }

  public void setStrength(float strength) {
    this.strength = strength;
  }

  @Override
  public void initialize(ParticleData p) {

  }

  @Override
  public void reset(ParticleData p) {

  }

  @Override
  public void write(JmeExporter ex) throws IOException {
    super.write(ex);
    OutputCapsule oc = ex.getCapsule(this);
    oc.write(chance, "change", 0.5f);
    oc.write(strength, "strength", 1.0f);
    oc.write(magnitude, "magnitude", 0.05f);

  }

  @Override
  public void read(JmeImporter im) throws IOException {
    super.read(im);
    InputCapsule ic = im.getCapsule(this);
    chance = ic.readFloat("chance", 0.5f);
    strength = ic.readFloat("strength", 1.0f);
    magnitude = ic.readFloat("magnitude", 0.05f);
  }
}
