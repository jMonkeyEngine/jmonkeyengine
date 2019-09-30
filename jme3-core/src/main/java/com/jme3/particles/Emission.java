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
package com.jme3.particles;

import com.jme3.export.*;
import com.jme3.particles.valuetypes.ValueType;

import java.io.IOException;

/**
 * Emission
 * Defines a special emission type for the emitter that works outside of normal particle emissions
 * @author Jedic
 */
public class Emission implements Cloneable, Savable {

  private float delay = 0.0f;
  private ValueType count = new ValueType(25);
  private ValueType cycles = new ValueType(-1);
  private float interval = 0.1f;
  private float probability = 1.0f;

  private float timeSinceLast = 0.0f;
  private int currentCycleCount = 0;

  public Emission() {

  }

  public Emission(float delay, ValueType count, ValueType cycles, float interval, float probability) {
    this.delay = delay;
    this.count = count;
    this.cycles = cycles;
    this.interval = interval;
    this.probability = probability;
  }

  public float getTimeSinceLast() {
    return timeSinceLast;
  }

  public void setTimeSinceLast(float timeSinceLast) {
    this.timeSinceLast = timeSinceLast;
  }

  public int getCurrentCycleCount() {
    return currentCycleCount;
  }

  public void setCurrentCycleCount(int currentCycleCount) {
    this.currentCycleCount = currentCycleCount;
  }

  /**
   * Get Delay
   * Delay handles how long into the emitters life before we start attempting to emit particles
   * @return the delay in seconds
   */
  public float getDelay() {
    return delay;
  }

  public void setDelay(float delay) {
    this.delay = delay;
  }

  /**
   * Get Count
   * The count determines how many particles to attempt to emit per cycle
   * @return
   */
  public ValueType getCount() {
    return count;
  }

  public void setCount(ValueType count) {
    this.count = count;
  }

  /**
   * Get Cycles
   * The cycles determine how many times we emit particles during the emitters lifetime
   * @return
   */
  public ValueType getCycles() {
    return cycles;
  }

  public void setCycles(ValueType cycles) {
    this.cycles = cycles;
  }

  /**
   * Get Interval
   * How often do we attempt to emit particles per second
   * @return
   */
  public float getInterval() {
    return interval;
  }

  public void setInterval(float interval) {
    this.interval = interval;
  }

  /**
   * Get Probability
   * What chance do we have to emit particles every interval
   * @return
   */
  public float getProbability() {
    return probability;
  }

  public void setProbability(float probability) {
    this.probability = probability;
  }

  @Override
  public Emission clone() {
    Emission emission = this.clone();
    emission.cycles = cycles.clone();
    emission.count = count.clone();
    return emission;
  }

  @Override
  public void write(JmeExporter ex) throws IOException {
    OutputCapsule oc = ex.getCapsule(this);
    oc.write(delay, "delay", 0.0f);
    oc.write(interval, "interval", 0.1f);
    oc.write(probability, "probability", 1.0f);
    oc.write(count, "count", new ValueType(25));
    oc.write(cycles, "cycles", new ValueType(-1));
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    InputCapsule ic = im.getCapsule(this);
    delay = ic.readFloat("delay", 0.0f);
    interval = ic.readFloat("interval", 0.1f);
    probability = ic.readFloat("probability", 1.0f);
    count = (ValueType) ic.readSavable("count", new ValueType(25));
    cycles = (ValueType) ic.readSavable("cycles", new ValueType(-1));
  }
}
