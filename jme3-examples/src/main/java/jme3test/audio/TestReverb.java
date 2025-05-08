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
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Sphere;

public class TestReverb extends SimpleApplication implements ActionListener {

    public static void main(String[] args) {
        TestReverb app = new TestReverb();
        app.start();
    }

    private AudioNode audioSource;
    private float time = 0;
    private float nextTime = 1;

    /**
     * ### Effects ###
     * Changing a parameter value in the Effect Object after it has been attached to the Auxiliary Effect
     * Slot will not affect the effect in the effect slot. To update the parameters of the effect in the effect
     * slot, an application must update the parameters of an Effect object and then re-attach it to the
     * Auxiliary Effect Slot.
     */
    private int index = 0;
    private final Environment[] environments = {
            Environment.Cavern,
            Environment.AcousticLab,
            Environment.Closet,
            Environment.Dungeon,
            Environment.Garage
    };

    @Override
    public void simpleInitApp() {

        configureCamera();

        // Activate the Environment preset
        audioRenderer.setEnvironment(environments[index]);

        // Activate 3D audio
        audioSource = new AudioNode(assetManager, "Sound/Effects/Bang.wav", AudioData.DataType.Buffer);
        audioSource.setLooping(false);
        audioSource.setPositional(true);
        audioSource.setMaxDistance(100);
        audioSource.setRefDistance(5);
        audioSource.setReverbEnabled(true);
        rootNode.attachChild(audioSource);

        Geometry marker = makeShape("Marker", new Sphere(16, 16, 1f), ColorRGBA.Red);
        audioSource.attachChild(marker);

        Geometry grid = makeShape("DebugGrid", new Grid(21, 21, 4), ColorRGBA.Blue);
        grid.center().move(0, 0, 0);
        rootNode.attachChild(grid);

        registerInputMappings();
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(50f);
        flyCam.setDragToRotate(true);

        cam.setLocation(Vector3f.UNIT_XYZ.mult(50f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
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
            audioSource.setLocalTranslation(position);
            audioSource.playInstance();
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

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) return;

        if (name.equals("toggleReverbEnabled")) {
            boolean reverbEnabled = audioSource.isReverbEnabled();
            audioSource.setReverbEnabled(!reverbEnabled);
            System.out.println("reverbEnabled: " + audioSource.isReverbEnabled());

        } else if (name.equals("nextEnvironment")) {
            index = (index + 1) % environments.length;
            audioRenderer.setEnvironment(environments[index]);
            System.out.println("Next Environment Index: " + index);
        }
    }

    private void registerInputMappings() {
        addMapping("toggleReverbEnabled", new KeyTrigger(KeyInput.KEY_SPACE));
        addMapping("nextEnvironment", new KeyTrigger(KeyInput.KEY_N));
    }

    private void addMapping(String mappingName, Trigger... triggers) {
        inputManager.addMapping(mappingName, triggers);
        inputManager.addListener(this, mappingName);
    }

}
