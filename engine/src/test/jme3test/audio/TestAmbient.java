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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

public class TestAmbient extends SimpleApplication {

  private AudioNode nature, waves;

  public static void main(String[] args) {
    TestAmbient test = new TestAmbient();
    test.start();
  }

  @Override
  public void simpleInitApp() {
    float[] eax = new float[]{15, 38.0f, 0.300f, -1000, -3300, 0,
      1.49f, 0.54f, 1.00f, -2560, 0.162f, 0.00f, 0.00f,
      0.00f, -229, 0.088f, 0.00f, 0.00f, 0.00f, 0.125f, 1.000f,
      0.250f, 0.000f, -5.0f, 5000.0f, 250.0f, 0.00f, 0x3f};
    Environment env = new Environment(eax);
    audioRenderer.setEnvironment(env);

    waves = new AudioNode(assetManager, "Sound/Environment/Ocean Waves.ogg", false);
    waves.setPositional(true);
    waves.setLocalTranslation(new Vector3f(0, 0,0));
    waves.setMaxDistance(100);
    waves.setRefDistance(5);

    nature = new AudioNode(assetManager, "Sound/Environment/Nature.ogg", true);
    nature.setVolume(3);
    
    waves.playInstance();
    nature.play();
    
    // just a blue box to mark the spot
    Box box1 = new Box(Vector3f.ZERO, .5f, .5f, .5f);
    Geometry player = new Geometry("Player", box1);
    Material mat1 = new Material(assetManager,
            "Common/MatDefs/Misc/Unshaded.j3md");
    mat1.setColor("Color", ColorRGBA.Blue);
    player.setMaterial(mat1);
    rootNode.attachChild(player);
  }

  @Override
  public void simpleUpdate(float tpf) {
  }
}
