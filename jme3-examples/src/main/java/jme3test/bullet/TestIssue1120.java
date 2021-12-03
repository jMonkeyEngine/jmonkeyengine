/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package jme3test.bullet;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.GImpactCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.debug.BulletDebugAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import com.jme3.system.AppSettings;
import java.util.ArrayList;
import java.util.List;

/**
 * Test demonstrating a GImpactCollisionShape falling through a curved mesh, when using JBullet. Bullet native
 * does not experience this issue at the time this test was created.
 *
 * @author lou
 */
public class TestIssue1120 extends SimpleApplication {

    private BulletAppState bulletAppState;
    private final boolean physicsDebug = true;
    private BitmapText speedText;
    private final List<Spatial> testObjects = new ArrayList<>();
    private static final boolean SKIP_SETTINGS = false;//Used for repeated runs of this test during dev
    private float bulletSpeed = 0.5f;

    public static void main(String[] args) {
        TestIssue1120 test = new TestIssue1120();
        test.setSettings(new AppSettings(true));
        test.settings.setFrameRate(60);
        if (SKIP_SETTINGS) {
            test.settings.setWidth(1920);
            test.settings.setHeight(1150);
            test.showSettings = !SKIP_SETTINGS;
        }
        test.start();
    }

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(-7.285349f, -2.2638104f, 4.954474f));
        cam.setRotation(new Quaternion(0.07345789f, 0.92521834f, -0.2876841f, 0.23624739f));
        getFlyByCamera().setMoveSpeed(5);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        dl.setColor(ColorRGBA.Green);
        rootNode.addLight(dl);

        //Setup interactive test controls
        inputManager.addMapping("restart", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("pause", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("+", new KeyTrigger(KeyInput.KEY_ADD), new KeyTrigger(KeyInput.KEY_EQUALS));
        inputManager.addMapping("-", new KeyTrigger(KeyInput.KEY_SUBTRACT), new KeyTrigger(KeyInput.KEY_MINUS));
        inputManager.addListener((ActionListener) (String name, boolean isPressed, float tpf) -> {
            if (!isPressed) {
                return;
            }
            switch (name) {
                case "restart":
                    cleanup();
                    initializeNewTest();
                    break;
                case "pause":
                    bulletAppState.setSpeed(bulletAppState.getSpeed() > 0.1 ? 0 : bulletSpeed);
                    break;
                case "+":
                    bulletSpeed += 0.1f;
                    if (bulletSpeed > 1f) {
                        bulletSpeed = 1f;
                    }
                    bulletAppState.setSpeed(bulletSpeed);
                    break;
                case "-":
                    bulletSpeed -= 0.1f;
                    if (bulletSpeed < 0.1f) {
                        bulletSpeed = 0.1f;
                    }
                    bulletAppState.setSpeed(bulletSpeed);
                    break;
            }
        }, "pause", "restart", "+", "-");

        guiNode = getGuiNode();
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText[] testInfo = new BitmapText[2];
        testInfo[0] = new BitmapText(font);
        testInfo[1] = new BitmapText(font);
        speedText = new BitmapText(font);

        float lineHeight = testInfo[0].getLineHeight();
        testInfo[0].setText("Camera move: W/A/S/D/Q/Z     +/-: Increase/Decrease Speed");
        testInfo[0].setLocalTranslation(5, settings.getHeight(), 0);
        guiNode.attachChild(testInfo[0]);
        testInfo[1].setText("Left Click: Toggle pause            Space: Restart test");
        testInfo[1].setLocalTranslation(5, settings.getHeight() - lineHeight, 0);
        guiNode.attachChild(testInfo[1]);

        speedText.setLocalTranslation(202, lineHeight * 1, 0);
        guiNode.attachChild(speedText);

        initializeNewTest();
    }

    private void initializeNewTest() {
        bulletAppState = new BulletAppState();
        bulletAppState.setDebugEnabled(physicsDebug);
        stateManager.attach(bulletAppState);

        bulletAppState.setSpeed(bulletSpeed);

        dropTest();

        Geometry leftFloor = PhysicsTestHelper.createMeshTestFloor(assetManager, 20, new Vector3f(-11, -5, -10));
        addObject(leftFloor);

        //Hide physics debug visualization for floors
        if (physicsDebug) {
            BulletDebugAppState bulletDebugAppState = stateManager.getState(BulletDebugAppState.class);
            bulletDebugAppState.setFilter((Object obj) -> {
                return !(obj.equals(leftFloor.getControl(RigidBodyControl.class)));
            });
        }
    }

    private void addObject(Spatial s) {
        testObjects.add(s);
        rootNode.attachChild(s);
        physicsSpace().add(s);
    }

    private void dropTest() {
        attachTestObject(new Cylinder(2, 16, 0.2f, 2f, true), new Vector3f(0f, 2f, -5f), 2);
        attachTestObject(new Cylinder(2, 16, 0.2f, 2f, true), new Vector3f(-1f, 2f, -5f), 2);
        attachTestObject(new Cylinder(2, 16, 0.2f, 2f, true), new Vector3f(-2f, 2f, -5f), 2);
        attachTestObject(new Cylinder(2, 16, 0.2f, 2f, true), new Vector3f(-3f, 2f, -5f), 2);
    }

    private void attachTestObject(Mesh mesh, Vector3f position, float mass) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Geometry g = new Geometry("mesh", mesh);
        g.setLocalTranslation(position);
        g.setMaterial(material);

        RigidBodyControl control = new RigidBodyControl(new GImpactCollisionShape(mesh), mass);
        g.addControl(control);
        addObject(g);
    }

    private PhysicsSpace physicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    @Override
    public void simpleUpdate(float tpf) {
        speedText.setText("Speed: " + String.format("%.1f", bulletSpeed));
    }

    private void cleanup() {
        stateManager.detach(bulletAppState);
        stateManager.detach(stateManager.getState(BulletDebugAppState.class));
        for (Spatial s : testObjects) {
            rootNode.detachChild(s);
        }
    }
}
