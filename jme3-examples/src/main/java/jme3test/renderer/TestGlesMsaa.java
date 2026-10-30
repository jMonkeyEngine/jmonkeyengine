/*
 * Copyright (c) 2026 jMonkeyEngine
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
package jme3test.renderer;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;

/**
 * Visual comparison of GLES presentation MSAA, without post-processing or supersampling.
 * Run with {@code ./gradlew :jme3-examples:runExamples -Pexample=jme3test.renderer.TestGlesMsaa}.
 * The optional argument is the initial sample count: {@code --args="0"} or {@code --args="4"}.
 * Press M to toggle MSAA and Space to toggle rotation; the camera stays fixed for comparison.
 */
public class TestGlesMsaa extends SimpleApplication implements ActionListener {

    private final Node edges = new Node("MSAA edge targets");
    private BitmapText info;
    private boolean rotating;

    public static void main(String[] args) {
        int samples = args.length == 0 ? 4 : Integer.parseInt(args[0]);
        if (samples != 0 && samples != 4) {
            throw new IllegalArgumentException("Use 0 (MSAA off) or 4 (4x MSAA)");
        }
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.ANGLE_GLES3);
        settings.setTitle("GLES MSAA comparison");
        settings.setResolution(960, 640);
        settings.setDisplayScaleMode(AppSettings.DISPLAY_SCALE_NATIVE_PIXELS);
        settings.setGammaCorrection(true);
        settings.setSamples(samples);
        settings.setAudioRenderer(null);
        settings.setVSync(true);
        TestGlesMsaa app = new TestGlesMsaa();
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        setDisplayFps(false);
        setDisplayStatView(false);
        viewPort.setBackgroundColor(new ColorRGBA(0.015f, 0.015f, 0.015f, 1f));
        cam.setLocation(new Vector3f(0, 0, 11));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        Material white = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        white.setColor("Color", ColorRGBA.White);

        Geometry cube = new Geometry("Oblique silhouette", new Box(1.1f, 1.1f, 1.1f));
        cube.setMaterial(white);
        cube.setLocalTranslation(-2.2f, 0, 0);
        cube.rotate(0.35f, 0.55f, 0.2f);
        edges.attachChild(cube);

        // Thin solid triangles expose coverage aliasing without depending on GL line rendering.
        Box blade = new Box(0.9f, 0.012f, 0.015f);
        for (int index = 0; index < 32; index++) {
            float angle = (index + 0.17f) * FastMath.TWO_PI / 32f;
            Geometry spoke = new Geometry("Thin spoke " + index, blade);
            spoke.setMaterial(white);
            spoke.rotate(0, 0, angle);
            spoke.setLocalTranslation(2.2f + 1.15f * FastMath.cos(angle),
                    1.15f * FastMath.sin(angle), 0);
            edges.attachChild(spoke);
        }
        rootNode.attachChild(edges);

        info = new BitmapText(guiFont);
        info.setSize(18);
        guiNode.attachChild(info);
        inputManager.addMapping("ToggleMsaa", new KeyTrigger(KeyInput.KEY_M));
        inputManager.addMapping("ToggleRotation", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "ToggleMsaa", "ToggleRotation");
    }

    @Override
    public void onAction(String name, boolean pressed, float tpf) {
        if (!pressed) {
            return;
        }
        if ("ToggleMsaa".equals(name)) {
            int samples = context.getSettings().getSamples() > 1 ? 0 : 4;
            settings.setSamples(samples);
            // The presentation buffer is rebuilt next frame; keep the scene and GL context intact.
            context.getSettings().setSamples(samples);
        } else if ("ToggleRotation".equals(name)) {
            rotating = !rotating;
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (rotating) {
            edges.rotate(0, 0, tpf * 0.12f);
        }
        FrameBuffer target = renderer.getCurrentFrameBuffer();
        int requested = context.getSettings().getSamples();
        String active = target == null ? "unavailable" : target.getSamples() + " sample(s)";
        info.setText("ANGLE / GLES - presentation MSAA"
                + "\nRequested: " + (requested > 1 ? requested + "x" : "OFF") + " | Active: " + active
                + "\nM: toggle OFF / 4x    Space: rotation " + (rotating ? "ON" : "OFF") + "    Esc: quit"
                + "\nCompare the cube outline and the thin spokes. No post-processing or supersampling.");
        info.setLocalTranslation(15, cam.getHeight() - 15, 0);
    }
}
