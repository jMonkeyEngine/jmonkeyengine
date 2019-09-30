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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.particles.Emitter;
import com.jme3.particles.particle.ParticleData;
import com.jme3.particles.particle.ParticleDataTrails;
import com.jme3.particles.particle.ParticleTrailPoint;
import com.jme3.particles.valuetypes.ColorValueType;
import com.jme3.particles.valuetypes.ValueType;
import com.jme3.scene.Geometry;

import java.io.IOException;
import java.util.Iterator;

/**
 * Trail Module
 * Creates particle trails that follow after a particle
 *
 * @author Jedic
 */
public class TrailInfluencer extends ParticleInfluencer {

  // Used as a temp object for internal calculations
  private Vector3f tempOne = new Vector3f();

  // trail related display info
  private Geometry trailGeo;
  private Material trailmat;
  private ParticleDataTrails trailMesh = new ParticleDataTrails();

  // How often do we output a polygon for the trail?
  private float minLength = 0.1f;

  // Needs to be between 0 - 1.0
  // Basically tells the system how long a trail segment lasts compared to the particle lifetime
  private float trailLife = 0.5f;

  private boolean useParticleSize = false;
  private boolean useParticleColor = false;

  private ValueType trailSize = new ValueType(0.1f);
  private ColorValueType colorOverLifetime = new ColorValueType(new ColorRGBA(1.0f, 0.0f, 0.0f, 1.0f));

  public TrailInfluencer() {
  }


  public boolean isUseParticleSize() {
    return useParticleSize;
  }

  public void setUseParticleSize(boolean useParticleSize) {
    this.useParticleSize = useParticleSize;
  }

  public boolean isUseParticleColor() {
    return useParticleColor;
  }

  public void setUseParticleColor(boolean useParticleColor) {
    this.useParticleColor = useParticleColor;
  }

  public ValueType getTrailSize() {
    return trailSize;
  }

  public void setTrailSize(ValueType trailSize) {
    this.trailSize = trailSize;
  }

  public ColorValueType getColorOverLifetime() {
    return colorOverLifetime;
  }

  public void setColorOverLifetime(ColorValueType colorOverLifetime) {
    this.colorOverLifetime = colorOverLifetime;
  }

  public float getMinLength() {
    return minLength;
  }

  public void setMinLength(float minLength) {
    this.minLength = minLength;
  }

  public float getTrailLife() {
    return trailLife;
  }

  public void setTrailLife(float trailLife) {
    this.trailLife = trailLife;
  }

  public Material getTrailmat() {
    return trailmat;
  }

  public void setTrailmat(Material trailmat) {
    this.trailmat = trailmat;

    initializeInfluencer(emitter);
  }

  @Override
  public ParticleInfluencer clone() {
    TrailInfluencer module = (TrailInfluencer)super.clone();
    module.trailSize = trailSize.clone();
    module.colorOverLifetime = colorOverLifetime.clone();
    return module;
  }

  @Override
  public void update(ParticleData p, float tpf) {
    // Trail update info
    boolean add = false;
    boolean start = false;
    if (p.trailSegments.size() == 0) {
      add = true;
      start = true;
    } else {
      // are we close enough to add a new segment
      ParticleTrailPoint last = p.trailSegments.getLast();
      tempOne.set(p.position);
      tempOne.subtractLocal(last.position);
      float dist = tempOne.length();
      if (dist >= minLength) {
        add = true;
      }
    }

    // add particle trail point
    if (add) {
      p.trailSegments.addLast(new ParticleTrailPoint(
          start ? p.initialPosition : p.position,
          p.velocity,
          useParticleSize ? p.size : trailSize.getValue(0.0f, p.randomValue),
          p.color,
          p.startlife * trailLife));

    }

    // check segments lifetime
    Iterator<ParticleTrailPoint> itr = p.trailSegments.iterator();
    while (itr.hasNext()) {
      ParticleTrailPoint trailPoint = itr.next();
      trailPoint.life -= tpf;
      if (trailPoint.life <= 0) {
        itr.remove();
      }

      float life = p.startlife * trailLife - trailPoint.life;
      float maxLife = p.startlife * trailLife;

      // modulate size based on lifetime
      if (!useParticleSize) {
        float newSize = trailSize.getValue(life, p.randomValue);
        trailPoint.size = newSize;
      }

      // modulate color based on lifetime
      if (!useParticleColor) {
        colorOverLifetime.getValueColor(life / maxLife, p.randomValue, trailPoint.color);
      }
    }

  }

  @Override
  public void initializeInfluencer(Emitter emitter) {
    super.initializeInfluencer(emitter);

    trailMesh.initParticleData(emitter, emitter.getMaxParticles());

    // attach our geo
    if (trailGeo != null) {
      trailGeo.removeFromParent();
      trailGeo = null;
    }

    if (trailGeo == null && trailmat != null) {
      trailGeo = new Geometry();
      trailGeo.setMesh(trailMesh);
      trailGeo.setMaterial(trailmat);
      emitter.attachChild(trailGeo);
    }

    if (trailGeo != null && trailGeo.getParent() == null) {
      emitter.attachChild(trailGeo);
    }

  }

  @Override
  public void initialize(ParticleData p) {

  }

  @Override
  public void reset(ParticleData p) {

  }

  @Override
  public void updateGlobal(float tpf) {
    if (trailGeo != null && trailGeo.getParent() == null) {
      initializeInfluencer(emitter);
    }

    if (trailGeo != null) {
      trailGeo.setIgnoreTransform(!emitter.getParticlesFollowEmitter());
    }
  }

  @Override
  public void write(JmeExporter ex) throws IOException {
    super.write(ex);
    OutputCapsule oc = ex.getCapsule(this);
    oc.write(trailmat, "trailmat", null);
    oc.write(minLength, "minlength", 0.1f);
    oc.write(trailLife, "traillife", 1.0f);
    oc.write(useParticleSize, "useparticlesize", false);
    oc.write(useParticleColor, "useparticlecolor", false);
    oc.write(trailSize, "trailsize", new ValueType(1.0f));
    oc.write(colorOverLifetime, "coloroverlifetime", new ColorValueType(new ColorRGBA()));
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    super.read(im);
    InputCapsule ic = im.getCapsule(this);
    trailmat = (Material)ic.readSavable("trailmat", null);
    minLength = ic.readFloat("minlength", 0.1f);
    trailLife = ic.readFloat("traillife", 1.0f);
    useParticleSize = ic.readBoolean("useparticlesize", false);
    useParticleColor = ic.readBoolean("useparticlecolor", false);
    trailSize = (ValueType)ic.readSavable("trailsize", new ValueType(1.0f));
    colorOverLifetime = (ColorValueType)ic.readSavable("coloroverlifetime", new ColorValueType(new ColorRGBA()));

  }
}
