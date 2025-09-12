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
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Environment;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Sphere;

public class TestAmbient extends SimpleApplication {

    public static void main(String[] args) {
        TestAmbient test = new TestAmbient();
        test.start();
    }

    private final float[] eax = {
            15, 38.0f, 0.300f, -1000, -3300, 0,
            1.49f, 0.54f, 1.00f, -2560, 0.162f, 0.00f, 0.00f,
            0.00f, -229, 0.088f, 0.00f, 0.00f, 0.00f, 0.125f, 1.000f,
            0.250f, 0.000f, -5.0f, 5000.0f, 250.0f, 0.00f, 0x3f
    };

    @Override
    public void simpleInitApp() {
        configureCamera();

        Environment env = new Environment(eax);
        audioRenderer.setEnvironment(env);

        AudioNode waves = new AudioNode(assetManager,
                "Sound/Environment/Ocean Waves.ogg", DataType.Buffer);
        waves.setPositional(true);
        waves.setLooping(true);
        waves.setReverbEnabled(true);
        rootNode.attachChild(waves);

        AudioNode nature = new AudioNode(assetManager,
                "Sound/Environment/Nature.ogg", DataType.Stream);
        nature.setPositional(false);
        nature.setLooping(true);
        nature.setVolume(3);
        rootNode.attachChild(nature);

        waves.play();
        nature.play();

        // just a blue sphere to mark the spot
        Geometry marker = makeShape("Marker", new Sphere(16, 16, 1f), ColorRGBA.Blue);
        waves.attachChild(marker);

        Geometry grid = makeShape("DebugGrid", new Grid(21, 21, 2), ColorRGBA.Gray);
        grid.center().move(0, 0, 0);
        rootNode.attachChild(grid);
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(25f);
        flyCam.setDragToRotate(true);

        cam.setLocation(Vector3f.UNIT_XYZ.mult(5f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    private Geometry makeShape(String name, Mesh mesh, ColorRGBA color) {
        Geometry geo = new Geometry(name, mesh);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geo.setMaterial(mat);
        return geo;
    }
}
