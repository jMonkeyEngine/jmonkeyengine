/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.List;
import jme3test.bullet.PhysicsTestHelper;

/**
 * 1st Test: 10 solver iterations, large pot<br>
 * 2nd Test: 20 solver iterations, large pot<br>
 * 3rd Test: 30 solver iterations, large pot<br>
 * 4th Test: 10 solver iterations, small pot<br>
 * 5th Test: 20 solver iterations, small pot<br>
 * 6th Test: 30 solver iterations, small pot
 *
 * @author lou
 */
public class TestGimpactShape extends SimpleApplication {

    private BulletAppState bulletAppState;
    private final boolean physicsDebug = true;
    private int solverNumIterations = 10;
    protected BitmapFont font;
    protected BitmapText timeElapsedTxt;
    protected BitmapText solverNumIterationsTxt;
    private final List<Spatial> testObjects = new ArrayList<>();
    private float testTimer = 0;
    private final float TIME_PER_TEST = 10;
    private float teapotScale = 1;

    public static void main(String[] args) {
        TestGimpactShape a = new TestGimpactShape();
        a.start();
    }

    @Override
    public void simpleInitApp() {
        getCamera().setLocation(new Vector3f(0, 10, 25));
        getCamera().lookAt(new Vector3f(0, -5, 0), Vector3f.UNIT_Y);
        getFlyByCamera().setMoveSpeed(25);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        dl.setColor(ColorRGBA.Green);
        rootNode.addLight(dl);

        guiNode = getGuiNode();
        font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        timeElapsedTxt = new BitmapText(font, false);
        solverNumIterationsTxt = new BitmapText(font, false);
        float lineHeight = timeElapsedTxt.getLineHeight();

        timeElapsedTxt.setLocalTranslation(202, lineHeight * 1, 0);
        guiNode.attachChild(timeElapsedTxt);
        solverNumIterationsTxt.setLocalTranslation(202, lineHeight * 2, 0);
        guiNode.attachChild(solverNumIterationsTxt);

        init();
    }

    private void init() {
        solverNumIterationsTxt.setText("Solver Iterations: " + solverNumIterations);

        bulletAppState = new BulletAppState();
        bulletAppState.setDebugEnabled(physicsDebug);
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().setSolverNumIterations(solverNumIterations);

        //Left side test - GImpact objects collide with MeshCollisionShape floor
        dropTest(-5, 2, 0);
        dropTest2(-11, 7, 3);

        Geometry leftFloor = PhysicsTestHelper.createMeshTestFloor(assetManager, 20, new Vector3f(-21, -5, -10));
        addObject(leftFloor);

        //Right side test - GImpact objects collide with GImpact floor
        dropTest(10, 2, 0);
        dropTest2(9, 7, 3);

        Geometry rightFloor = PhysicsTestHelper.createGImpactTestFloor(assetManager, 20, new Vector3f(0, -5, -10));
        addObject(rightFloor);

        //Hide physics debug visualization for floors
        if (physicsDebug) {
            BulletDebugAppState bulletDebugAppState = stateManager.getState(BulletDebugAppState.class);
            bulletDebugAppState.setFilter((Object obj) -> {
                return !(obj.equals(rightFloor.getControl(RigidBodyControl.class))
                    || obj.equals(leftFloor.getControl(RigidBodyControl.class)));
            });
        }
    }

    private void addObject(Spatial s) {
        testObjects.add(s);
        rootNode.attachChild(s);
        physicsSpace().add(s);
    }

    private void dropTest(float x, float y, float z) {
        Vector3f offset = new Vector3f(x, y, z);
        attachTestObject(new Sphere(16, 16, 0.5f), new Vector3f(-4f, 2f, 2f).add(offset), 1);
        attachTestObject(new Sphere(16, 16, 0.5f), new Vector3f(-5f, 2f, 0f).add(offset), 1);
        attachTestObject(new Sphere(16, 16, 0.5f), new Vector3f(-6f, 2f, -2f).add(offset), 1);
        attachTestObject(new Box(0.5f, 0.5f, 0.5f), new Vector3f(-8f, 2f, -1f).add(offset), 10);
        attachTestObject(new Box(0.5f, 0.5f, 0.5f), new Vector3f(0f, 2f, -6f).add(offset), 10);
        attachTestObject(new Box(0.5f, 0.5f, 0.5f), new Vector3f(0f, 2f, -3f).add(offset), 10);
        attachTestObject(new Cylinder(2, 16, 0.2f, 2f), new Vector3f(0f, 2f, -5f).add(offset), 2);
        attachTestObject(new Cylinder(2, 16, 0.2f, 2f), new Vector3f(-1f, 2f, -5f).add(offset), 2);
        attachTestObject(new Cylinder(2, 16, 0.2f, 2f), new Vector3f(-2f, 2f, -5f).add(offset), 2);
        attachTestObject(new Cylinder(2, 16, 0.2f, 2f), new Vector3f(-3f, 2f, -5f).add(offset), 2);
    }

    private void dropTest2(float x, float y, float z) {
        Node n = (Node) assetManager.loadModel("Models/Teapot/Teapot.mesh.xml");
        n.setLocalTranslation(x, y, z);
        n.rotate(0, 0, -FastMath.HALF_PI);
        n.scale(teapotScale);

        Geometry tp = ((Geometry) n.getChild(0));
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        tp.setMaterial(mat);

        Mesh mesh = tp.getMesh();
        GImpactCollisionShape shape = new GImpactCollisionShape(mesh);
        shape.setScale(new Vector3f(teapotScale, teapotScale, teapotScale));

        RigidBodyControl control = new RigidBodyControl(shape, 2);
        n.addControl(control);
        addObject(n);
    }

    private void attachTestObject(Mesh mesh, Vector3f position, float mass) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));
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
        testTimer += tpf;

        if (testTimer / TIME_PER_TEST > 1) {
            testTimer = 0;
            switch (solverNumIterations) {
                case 10:
                    solverNumIterations = 20;
                    cleanup();
                    init();
                    break;
                case 20:
                    solverNumIterations = 30;
                    cleanup();
                    init();
                    break;
                case 30:
                    solverNumIterations = 10;
                    teapotScale = teapotScale > 0.9f ? 0.5f : 1;
                    cleanup();
                    init();
                    break;
            }
        }
        timeElapsedTxt.setText("Time Elapsed: " + testTimer);
    }

    private void cleanup() {
        stateManager.detach(bulletAppState);
        stateManager.detach(stateManager.getState(BulletDebugAppState.class));
        for (Spatial s : testObjects) {
            rootNode.detachChild(s);
        }
    }
}
