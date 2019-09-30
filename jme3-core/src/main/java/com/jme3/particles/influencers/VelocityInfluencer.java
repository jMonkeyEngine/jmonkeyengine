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
import com.jme3.math.Vector3f;
import com.jme3.particles.particle.ParticleData;
import com.jme3.particles.valuetypes.ValueType;
import com.jme3.particles.valuetypes.VectorValueType;

import java.io.IOException;

/**
 * VelocityInfluencer
 *
 * Manipulates the velocity of the particle over it's lifetime. These values are multiplied against
 * the initial velocity to create the final value.
 *
 * @author ghoffman
 */
public class VelocityInfluencer extends ParticleInfluencer {

  private Vector3f tempOne = new Vector3f();
  private Vector3f tempTwo = new Vector3f();
  private Vector3f tempThree = new Vector3f();

  private VectorValueType linear = new VectorValueType(new Vector3f(0, 2, 0));
  private VectorValueType orbital = new VectorValueType(new Vector3f(0, 0, 0));
  private VectorValueType orbitalRotations = new VectorValueType(new Vector3f(8, 8, 8));

  @Override
  public void update(ParticleData p, float tpf) {
    orbital.getValue3f(p.percentLife, p.randomValue, tempOne);
    orbitalRotations.getValue3f(p.percentLife, p.randomValue, tempThree);
    linear.getValue3f(p.percentLife, p.randomValue, tempTwo);
    tempOne.multLocal(p.initialVelocity);
    tempTwo.multLocal(p.initialVelocity);
    p.velocity.set(tempTwo);

    // for orbital velocity we add velocity based on each plane
    // X
    p.velocity.y += tempOne.x * Math.cos(tempThree.x * Math.PI * p.percentLife);
    p.velocity.z += tempOne.x * Math.sin(tempThree.x * Math.PI * p.percentLife);

    // Y
    p.velocity.x += tempOne.y * Math.cos(tempThree.y * Math.PI * p.percentLife);
    p.velocity.z += tempOne.y * Math.sin(tempThree.y * Math.PI * p.percentLife);

    // Z
    p.velocity.x += tempOne.z * Math.cos(tempThree.z * Math.PI * p.percentLife);
    p.velocity.y += tempOne.z * Math.sin(tempThree.z * Math.PI * p.percentLife);


  }

  public VectorValueType getLinear() {
    return linear;
  }

  public void setLinear(VectorValueType linear) {
    this.linear = linear;
  }

  public VectorValueType getOrbital() {
    return orbital;
  }

  public void setOrbital(VectorValueType orbital) {
    this.orbital = orbital;
  }

  public VectorValueType getOrbitalRotations() {
    return orbitalRotations;
  }

  public void setOrbitalRotations(VectorValueType orbitalRotations) {
    this.orbitalRotations = orbitalRotations;
  }

  @Override
  public void initialize(ParticleData p) {
  }

  @Override
  public void reset(ParticleData p) {
  }

  @Override
  public int getPriority() {
    return 1000;
  }

  @Override
  public void write(JmeExporter ex) throws IOException {
    super.write(ex);
    OutputCapsule oc = ex.getCapsule(this);
    oc.write(linear, "linear", new VectorValueType(new Vector3f()));
    oc.write(orbital, "orbital", new VectorValueType(new Vector3f()));
    oc.write(orbitalRotations, "orbitalRotations", new VectorValueType(new Vector3f()));
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    super.read(im);
    InputCapsule ic = im.getCapsule(this);
    linear = (VectorValueType) ic.readSavable("linear", new VectorValueType(new Vector3f()));
    orbital = (VectorValueType) ic.readSavable("orbital", new VectorValueType(new Vector3f()));
    orbitalRotations = (VectorValueType) ic.readSavable("orbitalRotations", new VectorValueType(new Vector3f()));
  }

  @Override
  public ParticleInfluencer clone() {
    VelocityInfluencer vi = (VelocityInfluencer) super.clone();
    vi.linear = linear.clone();
    vi.orbital = orbital.clone();
    vi.orbitalRotations = orbitalRotations.clone();
    return vi;
  }

  
}
