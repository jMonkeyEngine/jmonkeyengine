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
package jme3test.bullet.shape;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.GImpactCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.debug.BulletDebugAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.PQTorus;
import com.jme3.scene.shape.Torus;
import com.jme3.system.AppSettings;
import java.util.ArrayList;
import java.util.List;
import jme3test.bullet.PhysicsTestHelper;

/**
 * This test demonstrates various GImpactCollisionShapes colliding against two identical curved surfaces. The
 * left surface is a MeshCollisionShape, right surface is another GImpactCollisionShape. An ideal result is
 * for all objects to land and change to a blue colored mesh indicating they are inactive. Falling through the
 * floor, or never going inactive (bouncing forever) are failure conditions.
 * <p>
 * Observations as of June 2019 (JME v3.3.0-alpha2):
 * <ol>
 * <li>
 * With default starting parameters, Native Bullet should pass the test parameters above. JBullet fails due to
 * the rocket/MeshCollisionShape never going inactive.
 * </li>
 * <li>
 * Native Bullet behaves better than JBullet. JBullet sometimes allows objects to "gain too much energy" after
 * a collision, such as the rocket or teapot. Native also does this, to a lesser degree. This generally
 * appears to happen at larger object scales.
 * </li>
 * <li>
 * JBullet allows some objects to get "stuck" inside the floor, which usually results in a fall-through
 * eventually, generally a larger scales for this test.
 * </li>
 * <li>
 * Some shapes such as PQTorus and signpost never go inactive at larger scales for both Native and JBullet (test
 * at 1.5 and 1.9 scale)
 * </li>
 * </ol>
 *
 * @author lou
 */
public class TestGimpactShape extends SimpleApplication {

    private static TestGimpactShape test;
    private BulletAppState bulletAppState;
    private int solverNumIterations = 10;
    private BitmapText timeElapsedTxt;
    private BitmapText solverNumIterationsTxt;
    private BitmapText testScale;
    private final List<Spatial> testObjects = new ArrayList<>();
    private float testTimer = 0;
    private float scaleMod = 1;
    private boolean restart = true;
    private static final boolean SKIP_SETTINGS = false;//Used for repeated runs of this test during dev

    public static void main(String[] args) {
        test = new TestGimpactShape();
        test.setSettings(new AppSettings(true));
        test.settings.setVSync(true);
        if (SKIP_SETTINGS) {
            test.settings.setWidth(1920);
            test.settings.setHeight(1150);
            test.showSettings = !SKIP_SETTINGS;
        }
        test.start();
    }

    @Override
    public void simpleInitApp() {
        test = this;
        getCamera().setLocation(new Vector3f(40, 30, 160));
        getCamera().lookAt(new Vector3f(40, -5, 0), Vector3f.UNIT_Y);
        getFlyByCamera().setMoveSpeed(25);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        dl.setColor(ColorRGBA.Green);
        rootNode.addLight(dl);

        //Setup test instructions
        guiNode = getGuiNode();
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText[] testInfo = new BitmapText[2];
        testInfo[0] = new BitmapText(font);
        testInfo[1] = new BitmapText(font);
        timeElapsedTxt = new BitmapText(font);
        solverNumIterationsTxt = new BitmapText(font);
        testScale = new BitmapText(font);

        float lineHeight = testInfo[0].getLineHeight();
        testInfo[0].setText("Camera move:W/A/S/D/Q/Z     Solver iterations: 1=10, 2=20, 3=30");
        testInfo[0].setLocalTranslation(5, test.settings.getHeight(), 0);
        guiNode.attachChild(testInfo[0]);
        testInfo[1].setText("P: Toggle pause     Inc/Dec object scale: +, -     Space: Restart test");
        testInfo[1].setLocalTranslation(5, test.settings.getHeight() - lineHeight, 0);
        guiNode.attachChild(testInfo[1]);

        timeElapsedTxt.setLocalTranslation(202, lineHeight * 1, 0);
        guiNode.attachChild(timeElapsedTxt);
        solverNumIterationsTxt.setLocalTranslation(202, lineHeight * 2, 0);
        guiNode.attachChild(solverNumIterationsTxt);
        testScale.setLocalTranslation(202, lineHeight * 3, 0);
        guiNode.attachChild(testScale);

        //Setup interactive test controls
        inputManager.addMapping("restart", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener((ActionListener) (String name, boolean isPressed, float tpf) -> {
            restart = true;
        }, "restart");

        inputManager.addMapping("pause", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addListener((ActionListener) (String name, boolean isPressed, float tpf) -> {
            if (!isPressed) {
                return;
            }
            bulletAppState.setSpeed(bulletAppState.getSpeed() > 0.1 ? 0 : 1);
        }, "pause");

        inputManager.addMapping("1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("3", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("+", new KeyTrigger(KeyInput.KEY_ADD), new KeyTrigger(KeyInput.KEY_EQUALS));
        inputManager.addMapping("-", new KeyTrigger(KeyInput.KEY_SUBTRACT), new KeyTrigger(KeyInput.KEY_MINUS));
        inputManager.addListener((ActionListener) (String name, boolean isPressed, float tpf) -> {
            if (!isPressed) {
                return;
            }
            switch (name) {
                case "1":
                    solverNumIterations = 10;
                    break;
                case "2":
                    solverNumIterations = 20;
                    break;
                case "3":
                    solverNumIterations = 30;
                    break;
                case "+":
                    scaleMod += scaleMod < 1.9f ? 0.1f : 0;
                    break;
                case "-":
                    scaleMod -= scaleMod > 0.5f ? 0.1f : 0;
                    break;
            }
            restart = true;
        }, "1", "2", "3", "+", "-");

        initializeNewTest();
    }

    private void initializeNewTest() {
        testScale.setText("Object scale: " + String.format("%.1f", scaleMod));
        solverNumIterationsTxt.setText("Solver Iterations: " + solverNumIterations);

        bulletAppState = new BulletAppState();
        bulletAppState.setDebugEnabled(true);
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().setSolverNumIterations(solverNumIterations);

        float floorSize = 80;
        //Left side test - GImpact objects collide with MeshCollisionShape floor
        Vector3f leftFloorPos = new Vector3f(-41, -5, -10);
        Vector3f leftFloorCenter = leftFloorPos.add(floorSize / 2, 0, floorSize / 2);

        dropTest1(leftFloorCenter);
        dropTest2(leftFloorCenter);
        dropPot(leftFloorCenter);
        dropSword(leftFloorCenter);
        dropSign(leftFloorCenter);
        dropRocket(leftFloorCenter);

        Geometry leftFloor = PhysicsTestHelper.createMeshTestFloor(assetManager, floorSize, leftFloorPos);
        addObject(leftFloor);

        //Right side test - GImpact objects collide with GImpact floor
        Vector3f rightFloorPos = new Vector3f(41, -5, -10);
        Vector3f rightFloorCenter = rightFloorPos.add(floorSize / 2, 0, floorSize / 2);

        dropTest1(rightFloorCenter);
        dropTest2(rightFloorCenter);
        dropPot(rightFloorCenter);
        dropSword(rightFloorCenter);
        dropSign(rightFloorCenter);
        dropRocket(rightFloorCenter);

        Geometry rightFloor = PhysicsTestHelper.createGImpactTestFloor(assetManager, floorSize, rightFloorPos);
        addObject(rightFloor);

        //Hide physics debug visualization for floors
        BulletDebugAppState bulletDebugAppState = stateManager.getState(BulletDebugAppState.class);
        bulletDebugAppState.setFilter((Object obj) -> {
            return !(obj.equals(rightFloor.getControl(RigidBodyControl.class))
                || obj.equals(leftFloor.getControl(RigidBodyControl.class)));
        });
    }

    private void addObject(Spatial s) {
        testObjects.add(s);
        rootNode.attachChild(s);
        physicsSpace().add(s);
    }

    private void dropTest1(Vector3f offset) {
        offset = offset.add(-18, 6, -18);
        attachTestObject(new Torus(16, 16, 0.15f, 0.5f), new Vector3f(-12f, 0f, 5f).add(offset), 1);
        attachTestObject(new PQTorus(2f, 3f, 0.6f, 0.2f, 48, 16), new Vector3f(0, 0, 0).add(offset), 5);

    }

    private void dropTest2(Vector3f offset) {
        offset = offset.add(18, 6, -18);
        attachTestObject(new Torus(16, 16, 0.3f, 0.8f), new Vector3f(12f, 0f, 5f).add(offset), 3);
        attachTestObject(new PQTorus(3f, 5f, 0.8f, 0.2f, 96, 16), new Vector3f(0, 0, 0).add(offset), 10);
    }

    private void dropPot(Vector3f offset) {
        drop(offset.add(-12, 7, 15), "Models/Teapot/Teapot.mesh.xml", 1.0f, 2);
    }

    private void dropSword(Vector3f offset) {
        drop(offset.add(-10, 5, 3), "Models/Sinbad/Sword.mesh.xml", 1.0f, 2);
    }

    private void dropSign(Vector3f offset) {
        drop(offset.add(9, 15, 5), "Models/Sign Post/Sign Post.mesh.xml", 1.0f, 1);
    }

    private void dropRocket(Vector3f offset) {
        RigidBodyControl c = drop(offset.add(26, 4, 7), "Models/SpaceCraft/Rocket.mesh.xml", 4.0f, 3);
        c.setAngularDamping(0.5f);
        c.setLinearDamping(0.5f);
    }

    private RigidBodyControl drop(Vector3f offset, String model, float scale, float mass) {
        scale *= scaleMod;
        Node n = (Node) assetManager.loadModel(model);
        n.setLocalTranslation(offset);
        n.rotate(0, 0, -FastMath.HALF_PI);

        Geometry tp = ((Geometry) n.getChild(0));
        tp.scale(scale);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        tp.setMaterial(mat);

        Mesh mesh = tp.getMesh();
        GImpactCollisionShape shape = new GImpactCollisionShape(mesh);
        shape.setScale(new Vector3f(scale, scale, scale));

        RigidBodyControl control = new RigidBodyControl(shape, mass);
        n.addControl(control);
        addObject(n);
        return control;
    }

    private void attachTestObject(Mesh mesh, Vector3f position, float mass) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));
        Geometry g = new Geometry("mesh", mesh);
        g.scale(scaleMod);
        g.setLocalTranslation(position);
        g.setMaterial(material);

        GImpactCollisionShape shape = new GImpactCollisionShape(mesh);
        shape.setScale(new Vector3f(scaleMod, scaleMod, scaleMod));
        RigidBodyControl control = new RigidBodyControl(shape, mass);
        g.addControl(control);
        addObject(g);
    }

    private PhysicsSpace physicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    @Override
    public void simpleUpdate(float tpf) {
        testTimer += tpf * bulletAppState.getSpeed();

        if (restart) {
            cleanup();
            initializeNewTest();
            restart = false;
            testTimer = 0;
        }
        timeElapsedTxt.setText("Time Elapsed: " + String.format("%.3f", testTimer));
    }

    private void cleanup() {
        stateManager.detach(bulletAppState);
        stateManager.detach(stateManager.getState(BulletDebugAppState.class));
        for (Spatial s : testObjects) {
            rootNode.detachChild(s);
        }
    }
}
