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
import com.jme3.audio.AudioSource;
import com.jme3.audio.LowPassFilter;
import com.jme3.font.BitmapText;
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

/**
 *
 * @author capdevon
 */
public class TestOgg extends SimpleApplication implements ActionListener {

    private final StringBuilder sb = new StringBuilder();
    private int frameCount = 0;
    private BitmapText bmp;
    private AudioNode audioSource;
    private float volume = 1.0f;
    private float pitch = 1.0f;

    /**
     * ### Filters ###
     * Changing a parameter value in the Filter Object after it has been attached to a Source will not
     * affect the Source. To update the filter(s) used on a Source, an application must update the
     * parameters of a Filter object and then re-attach it to the Source.
     */
    private final LowPassFilter dryFilter = new LowPassFilter(1f, .1f);

    public static void main(String[] args) {
        TestOgg test = new TestOgg();
        test.start();
    }

    @Override
    public void simpleInitApp() {
        configureCamera();

        bmp = createLabelText(10, 20, "<placeholder>");

        // just a blue sphere to mark the spot
        Geometry marker = makeShape("Marker", new Sphere(16, 16, 1f), ColorRGBA.Blue);
        rootNode.attachChild(marker);

        Geometry grid = makeShape("DebugGrid", new Grid(21, 21, 2), ColorRGBA.Gray);
        grid.center().move(0, 0, 0);
        rootNode.attachChild(grid);

        audioSource = new AudioNode(assetManager, "Sound/Effects/Foot steps.ogg", DataType.Buffer);
        audioSource.setName("Foot steps");
        audioSource.setLooping(true);
        audioSource.setVolume(volume);
        audioSource.setPitch(pitch);
        audioSource.setMaxDistance(100);
        audioSource.setRefDistance(5);
        audioSource.play();
        rootNode.attachChild(audioSource);

        registerInputMappings();
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(25f);
        flyCam.setDragToRotate(true);

        cam.setLocation(Vector3f.UNIT_XYZ.mult(20f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    @Override
    public void simpleUpdate(float tpf) {
        frameCount++;
        if (frameCount % 10 == 0) {
            frameCount = 0;

            sb.append("Audio: ").append(audioSource.getName()).append("\n");
            sb.append(audioSource.getAudioData()).append("\n");
            sb.append("Looping: ").append(audioSource.isLooping()).append("\n");
            sb.append("Volume: ").append(String.format("%.2f", audioSource.getVolume())).append("\n");
            sb.append("Pitch: ").append(String.format("%.2f", audioSource.getPitch())).append("\n");
            sb.append("Positional: ").append(audioSource.isPositional()).append("\n");
            sb.append("MaxDistance: ").append(audioSource.getMaxDistance()).append("\n");
            sb.append("RefDistance: ").append(audioSource.getRefDistance()).append("\n");
            sb.append("Status: ").append(audioSource.getStatus()).append("\n");
            sb.append("SourceId: ").append(audioSource.getChannel()).append("\n");
            sb.append("DryFilter: ").append(audioSource.getDryFilter() != null).append("\n");
            sb.append("FilterId: ").append(dryFilter.getId()).append("\n");

            bmp.setText(sb.toString());
            sb.setLength(0);
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) return;

        if (name.equals("togglePlayPause")) {
            if (audioSource.getStatus() != AudioSource.Status.Playing) {
                audioSource.play();
            } else {
                audioSource.stop();
            }
        } else if (name.equals("togglePositional")) {
            boolean positional = audioSource.isPositional();
            audioSource.setPositional(!positional);

        } else if (name.equals("dryFilter")) {
            boolean hasFilter = audioSource.getDryFilter() != null;
            audioSource.setDryFilter(hasFilter ? null : dryFilter);

        } else if (name.equals("Volume+")) {
            volume = FastMath.clamp(volume + 0.1f, 0, 5f);
            audioSource.setVolume(volume);

        } else if (name.equals("Volume-")) {
            volume = FastMath.clamp(volume - 0.1f, 0, 5f);
            audioSource.setVolume(volume);

        } else if (name.equals("Pitch+")) {
            pitch = FastMath.clamp(pitch + 0.1f, 0.5f, 2f);
            audioSource.setPitch(pitch);

        } else if (name.equals("Pitch-")) {
            pitch = FastMath.clamp(pitch - 0.1f, 0.5f, 2f);
            audioSource.setPitch(pitch);
        }
    }

    private void registerInputMappings() {
        addMapping("togglePlayPause", new KeyTrigger(KeyInput.KEY_P));
        addMapping("togglePositional", new KeyTrigger(KeyInput.KEY_RETURN));
        addMapping("dryFilter", new KeyTrigger(KeyInput.KEY_SPACE));
        addMapping("Volume+", new KeyTrigger(KeyInput.KEY_I));
        addMapping("Volume-", new KeyTrigger(KeyInput.KEY_K));
        addMapping("Pitch+", new KeyTrigger(KeyInput.KEY_J));
        addMapping("Pitch-", new KeyTrigger(KeyInput.KEY_L));
    }

    private void addMapping(String mappingName, Trigger... triggers) {
        inputManager.addMapping(mappingName, triggers);
        inputManager.addListener(this, mappingName);
    }

    private BitmapText createLabelText(int x, int y, String text) {
        BitmapText bmp = new BitmapText(guiFont);
        bmp.setText(text);
        bmp.setLocalTranslation(x, settings.getHeight() - y, 0);
        bmp.setColor(ColorRGBA.Red);
        guiNode.attachChild(bmp);
        return bmp;
    }

    private Geometry makeShape(String name, Mesh mesh, ColorRGBA color) {
        Geometry geo = new Geometry(name, mesh);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geo.setMaterial(mat);
        return geo;
    }
}
