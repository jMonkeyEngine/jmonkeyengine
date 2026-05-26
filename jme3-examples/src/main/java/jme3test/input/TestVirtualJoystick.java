/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3test.input;

import com.jme3.app.SimpleApplication;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickButton;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.virtual.VirtualJoystick;
import com.jme3.input.virtual.VirtualJoystickDynamicLayout;
import com.jme3.input.virtual.VirtualJoystickXboxLayout;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.system.Platform;
import com.jme3.util.SkyFactory;

/**
 * Manual test for the on-screen virtual joystick.
 */
public class TestVirtualJoystick extends SimpleApplication {

    private static final String TOGGLE_LAYOUT = "ToggleVirtualJoystickLayout";

    private boolean dynamicLayout;

    public static void main(String[] args) {
        TestVirtualJoystick app = new TestVirtualJoystick();
        AppSettings settings = new AppSettings(true);
        configureSettings(settings);
        app.setSettings(settings);
        app.start();
    }

    public static void configureSettings(AppSettings settings) {
        settings.setUseJoysticks(true);
        settings.setJoysticksMapper(AppSettings.JOYSTICKS_XBOX_MAPPER);
        settings.setVirtualJoystick(defaultVirtualJoystickMode());
    }

    private static String defaultVirtualJoystickMode() {
        Platform.Os os = JmeSystem.getPlatform().getOs();
        if (os == Platform.Os.Android || os == Platform.Os.iOS) {
            return AppSettings.VIRTUAL_JOYSTICK_AUTO;
        }
        return AppSettings.VIRTUAL_JOYSTICK_ENABLED;
    }

    @Override
    public void simpleInitApp() {
        setDisplayStatView(false);
        setDisplayFps(false);

        cam.setLocation(new Vector3f(42f, 18f, 18f));
        cam.lookAt(new Vector3f(-18f, 4f, -10f), Vector3f.UNIT_Y);
        cam.setFrustumPerspective(60f, cam.getAspect(), 0.1f, 500f);

        flyCam.setMoveSpeed(18f);
        flyCam.setRotationSpeed(2f);
        flyCam.setDragToRotate(true);
        inputManager.setCursorVisible(true);
        setupLayoutToggle();

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.35f, 0.35f, 0.35f, 1f));
        rootNode.addLight(ambient);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.4f, -0.75f, -0.3f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(1.1f));
        rootNode.addLight(sun);

        buildScene();
    }

    private void setupLayoutToggle() {
        inputManager.addMapping(TOGGLE_LAYOUT, new KeyTrigger(KeyInput.KEY_L));
        Joystick[] joysticks = inputManager.getJoysticks();
        if (joysticks != null) {
            for (Joystick joystick : joysticks) {
                JoystickButton start = joystick.getButton(JoystickButton.BUTTON_XBOX_START);
                if (start != null) {
                    start.assignButton(TOGGLE_LAYOUT);
                }
            }
        }
        inputManager.addListener(layoutListener, TOGGLE_LAYOUT);
    }

    private final ActionListener layoutListener = (name, isPressed, tpf) -> {
        if (!TOGGLE_LAYOUT.equals(name) || !isPressed) {
            return;
        }
        dynamicLayout = !dynamicLayout;
        Joystick[] joysticks = inputManager.getJoysticks();
        if (joysticks == null) {
            return;
        }
        for (Joystick joystick : joysticks) {
            if (joystick instanceof VirtualJoystick) {
                ((VirtualJoystick) joystick).setLayout(dynamicLayout
                        ? new VirtualJoystickDynamicLayout(true)
                        : new VirtualJoystickXboxLayout());
            }
        }
    };

    private void buildScene() {
        Spatial scene = assetManager.loadModel("BlenderParity/scene.glb");
        rootNode.attachChild(scene);

        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);
    }
}
