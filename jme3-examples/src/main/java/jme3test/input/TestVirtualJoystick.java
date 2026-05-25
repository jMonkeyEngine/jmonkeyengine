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
import com.jme3.font.BitmapText;
import com.jme3.input.JoystickAxis;
import com.jme3.input.RawInputListenerAdapter;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import com.jme3.system.Platform;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manual test for the on-screen virtual joystick.
 */
public class TestVirtualJoystick extends SimpleApplication {

    private final Map<String, Float> axisValues = new LinkedHashMap<>();
    private BitmapText axisDebug;

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
        cam.setLocation(new Vector3f(0f, 5f, 22f));
        cam.lookAt(new Vector3f(0f, 5f, 0f), Vector3f.UNIT_Y);
        cam.setFrustumPerspective(60f, cam.getAspect(), 0.1f, 500f);

        flyCam.setMoveSpeed(18f);
        flyCam.setRotationSpeed(2f);
        flyCam.setDragToRotate(true);
        inputManager.setCursorVisible(true);
        setupAxisDebug();

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.25f, 0.25f, 0.25f, 1f));
        rootNode.addLight(ambient);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.35f, -0.8f, -0.45f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.8f));
        rootNode.addLight(sun);

        buildScene();
    }

    @Override
    public void simpleUpdate(float tpf) {
        updateAxisDebug();
    }

    private void setupAxisDebug() {
        axisDebug = new BitmapText(guiFont, false);
        axisDebug.setSize(16f);
        axisDebug.setColor(ColorRGBA.White);
        axisDebug.setLocalTranslation(12f, cam.getHeight() - 12f, 10f);
        guiNode.attachChild(axisDebug);

        inputManager.addRawInputListener(new RawInputListenerAdapter() {
            @Override
            public void onJoyAxisEvent(JoyAxisEvent evt) {
                JoystickAxis axis = evt.getAxis();
                String key = axis.getJoystick().getJoyId() + " "
                        + axis.getJoystick().getName() + " "
                        + axis.getLogicalId() + "[" + axis.getAxisId() + "]";
                axisValues.put(key, evt.getValue());
            }
        });
    }

    private void updateAxisDebug() {
        if (axisDebug == null) {
            return;
        }

        StringBuilder text = new StringBuilder("Joystick axes\n");
        for (Map.Entry<String, Float> entry : axisValues.entrySet()) {
            text.append(entry.getKey())
                    .append(" = ")
                    .append(String.format("%.3f", entry.getValue()))
                    .append('\n');
        }
        axisDebug.setText(text.toString());
    }

    private void buildScene() {
        Material floorMaterial = assetManager.loadMaterial("Textures/Terrain/Pond/Pond.j3m");
        Material wallMaterial = assetManager.loadMaterial("Textures/Terrain/BrickWall/BrickWall.j3m");
        Material columnMaterial = assetManager.loadMaterial("Textures/Terrain/Rock/Rock.j3m");

        Geometry floor = new Geometry("Textured Floor", new Box(24f, 0.1f, 24f));
        floor.getMesh().scaleTextureCoordinates(new Vector2f(12f, 12f));
        floor.setLocalTranslation(0f, -0.1f, 0f);
        floor.setMaterial(floorMaterial);
        rootNode.attachChild(floor);

        addWall("Back Wall", wallMaterial, 0f, 3f, -24f, 24f, 3f, 0.15f);
        addWall("Left Wall", wallMaterial, -24f, 3f, 0f, 0.15f, 3f, 24f);
        addWall("Right Wall", wallMaterial, 24f, 3f, 0f, 0.15f, 3f, 24f);

        addColumn(columnMaterial, -10f, 0f);
        addColumn(columnMaterial, 10f, 0f);
        addColumn(columnMaterial, -10f, -12f);
        addColumn(columnMaterial, 10f, -12f);

        Spatial tank = assetManager.loadModel("Models/Tank/tank.j3o");
        tank.setLocalTranslation(0f, 0.15f, -8f);
        tank.setLocalScale(2f);
        rootNode.attachChild(tank);
    }

    private void addWall(String name, Material material, float x, float y, float z, float xExtent, float yExtent,
            float zExtent) {
        Geometry wall = new Geometry(name, new Box(xExtent, yExtent, zExtent));
        wall.getMesh().scaleTextureCoordinates(new Vector2f(8f, 2f));
        wall.setLocalTranslation(x, y, z);
        wall.setMaterial(material);
        rootNode.attachChild(wall);
    }

    private void addColumn(Material material, float x, float z) {
        Geometry column = new Geometry("Column", new Box(1.25f, 3f, 1.25f));
        column.getMesh().scaleTextureCoordinates(new Vector2f(2f, 2f));
        column.setLocalTranslation(x, 3f, z);
        column.setMaterial(material);
        rootNode.attachChild(column);
    }
}
