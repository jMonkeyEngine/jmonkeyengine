/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package jme3test.audio;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Environment;
import com.jme3.audio.LowPassFilter;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;

public class TestReverb extends SimpleApplication {

    public static void main(String[] args) {
        TestReverb app = new TestReverb();
        app.start();
    }

    private AudioNode audio;
    private float time = 0;
    private float nextTime = 1;

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(25f);
        flyCam.setDragToRotate(true);

        cam.setLocation(Vector3f.UNIT_XYZ.mult(50f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        audioRenderer.setEnvironment(Environment.Cavern);

        audio = new AudioNode(assetManager, "Sound/Effects/Bang.wav", AudioData.DataType.Buffer);
        audio.setPositional(true);
        audio.setReverbEnabled(true);
        audio.setReverbFilter(new LowPassFilter(1, 1));
        rootNode.attachChild(audio);

        Geometry marker = makeShape("Marker", new Sphere(16, 16, .5f), ColorRGBA.Red);
        audio.attachChild(marker);

        Geometry floor = makeShape("Floor", new Box(20f, .05f, 20f), ColorRGBA.Blue);
        rootNode.attachChild(floor);
    }

    private Geometry makeShape(String name, Mesh mesh, ColorRGBA color) {
        Geometry geo = new Geometry(name, mesh);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geo.setMaterial(mat);
        return geo;
    }

    @Override
    public void simpleUpdate(float tpf) {
        time += tpf;

        if (time > nextTime) {
            time = 0;
            nextTime = FastMath.nextRandomFloat() * 2 + 0.5f;

            Vector3f position = getRandomPosition();
            audio.setLocalTranslation(position);
            audio.playInstance();
        }
    }

    private Vector3f getRandomPosition() {
        float x = FastMath.nextRandomFloat();
        float y = FastMath.nextRandomFloat();
        float z = FastMath.nextRandomFloat();
        Vector3f vec = new Vector3f(x, y, z);
        vec.multLocal(40, 2, 40);
        vec.subtractLocal(20, 1, 20);
        return vec;
    }
}
