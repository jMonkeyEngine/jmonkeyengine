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

import com.jme3.export.*;
import com.jme3.particles.Emitter;
import com.jme3.particles.particle.ParticleData;

import java.io.IOException;

/**
 * Particle Influencer
 *
 * @author t0neg0d
 * @author Jedic
 */
public abstract class ParticleInfluencer implements Savable, Cloneable {

  protected boolean enabled = true;
  protected Emitter emitter;

  /**
   * This method clones the influencer instance.
   * @return cloned instance
   */
  public ParticleInfluencer clone() {
    try {
      ParticleInfluencer pm = (ParticleInfluencer)super.clone();
      pm.enabled = enabled;
      return pm;
    } catch (Exception e) {
      throw new AssertionError();
    }
  }


  @Override
  public void write(JmeExporter ex) throws IOException {
    OutputCapsule oc = ex.getCapsule(this);
    oc.write(enabled, "enabled", true);
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    InputCapsule ic = im.getCapsule(this);
    enabled = ic.readBoolean("enabled", true);
  }

  /**
   * Initialize Influencer
   * This method is called whenever the emitter is re-initialized. This is mainly to allow influencers to
   * initialize special attributes such as particle trails or sprite animations.
   *
   * @param emitter
   */
  public void initializeInfluencer(Emitter emitter) {
    this.emitter = emitter;
  }

  /**
   * Get Priority
   * Priority controls the sort order of particle influencers. This allows certain influencers to take effect before others.
   *
   * @return The priority of this module. Higher priority takes place first.
   */
  public int getPriority() {
    return 0;
  }

  /**
   * Set Enabled
   * Enables or disables this module
   *
   * @param enabled
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Is Enabled
   * Checks to see if this module is enabled or not
   *
   * @return
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Update Global
   * Updates once per frame instead of for every particle
   *
   * @param tpf
   */
  public void updateGlobal(float tpf) {}

  /**
   * Update
   * Updates the given particle for the slice of time
   * @param p - the particle to update
   * @param tpf - the delta time for the last frame
   */
  public abstract void update(ParticleData p, float tpf);

  /**
   * Initialize
   * Sets up a particle as it is emitted
   * @param p - the particle to initialize
   */
  public abstract void initialize(ParticleData p);

  /**
   * Reset
   * Resets the given particle
   * @param p - the particle to reset
   */
  public abstract void reset(ParticleData p);

}