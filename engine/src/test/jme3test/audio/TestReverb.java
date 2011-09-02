/*
 * Copyright (c) 2009-2010 jMonkeyEngine All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package jme3test.audio;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Environment;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public class TestReverb extends SimpleApplication {

  private AudioNode audioSource;
  private float time = 0;
  private float nextTime = 1;

  public static void main(String[] args) {
    TestReverb test = new TestReverb();
    test.start();
  }

  @Override
  public void simpleInitApp() {
    audioSource = new AudioNode(assetManager, "Sound/Effects/Bang.wav");

    float[] eax = new float[]{15, 38.0f, 0.300f, -1000, -3300, 0,
      1.49f, 0.54f, 1.00f, -2560, 0.162f, 0.00f, 0.00f, 0.00f,
      -229, 0.088f, 0.00f, 0.00f, 0.00f, 0.125f, 1.000f, 0.250f,
      0.000f, -5.0f, 5000.0f, 250.0f, 0.00f, 0x3f};
    audioRenderer.setEnvironment(new Environment(eax));
    Environment env = Environment.Cavern;
    audioRenderer.setEnvironment(env);
  }

  @Override
  public void simpleUpdate(float tpf) {
    time += tpf;

    if (time > nextTime) {
      Vector3f v = new Vector3f();
      v.setX(FastMath.nextRandomFloat());
      v.setY(FastMath.nextRandomFloat());
      v.setZ(FastMath.nextRandomFloat());
      v.multLocal(40, 2, 40);
      v.subtractLocal(20, 1, 20);

      audioSource.setLocalTranslation(v);
      audioSource.playInstance();
      time = 0;
      nextTime = FastMath.nextRandomFloat() * 2 + 0.5f;
    }
  }
}
