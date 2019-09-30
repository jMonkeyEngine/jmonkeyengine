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
import com.jme3.particles.Emission;
import com.jme3.particles.particle.ParticleData;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Emission module
 * Allows the emitter to emit particles outside of it's normal workflow. This allows
 * more fine grained control to have spurts of particles instead of a steady emission rate.
 *
 * @author Jedic
 */
public class EmissionInfluencer extends ParticleInfluencer {

  private ArrayList<Emission> emissions = new ArrayList<>();

  @Override
  public void updateGlobal(float tpf) {
    for (Emission emission : emissions) {
      int cycles = (int)emission.getCycles().getValue(emitter.getCurrentDuration()/emitter.getDuration(),
          FastMath.nextRandomFloat());
      // check if we have cycles to emit particles or the duration is long enough
      if (emitter.getCurrentDuration() >= emission.getDelay() && (cycles == -1 || cycles < emission.getCurrentCycleCount())) {
        float t = emission.getTimeSinceLast();
        if (t + tpf >= emission.getInterval()) {
          int count = (int)emission.getCount().getValue(emitter.getCurrentDuration()/emitter.getDuration(),
              FastMath.nextRandomFloat());
          for (int i=0; i < count; i++) {
            if (FastMath.nextRandomFloat() <=  emission.getProbability()) {
              emitter.emitNextParticle();
            }
          }
          emission.setTimeSinceLast(t + tpf - emission.getInterval());
          emission.setCurrentCycleCount(emission.getCurrentCycleCount() + 1);
        } else {
          emission.setTimeSinceLast(t + tpf);
        }
      }
    }
  }

  @Override
  public void update(ParticleData p, float tpf) {

  }

  /**
   * Gets the list of emissions
   * @return
   */
  public ArrayList<Emission> getEmissions() {
    return emissions;
  }

  public void setEmissions(ArrayList<Emission> emissions) {
    this.emissions = emissions;
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
    oc.writeSavableArrayList(emissions, "emissions", new ArrayList());
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    super.read(im);
    InputCapsule ic = im.getCapsule(this);
    emissions = ic.readSavableArrayList("emissions", new ArrayList());
  }

}
