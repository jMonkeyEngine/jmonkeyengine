/*
 * Copyright (c) 2009-2012 jMonkeyEngine All rights reserved. <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. <p/> * Redistributions
 * in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. <p/> * Neither the name of
 * 'jMonkeyEngine' nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission. <p/> THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3test.bullet;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.debug.DebugTools;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;

/**
 * A walking physical character followed by a 3rd person camera. (No animation.)
 *
 * @author normenhansen, zathras
 */
public class TestBetterCharacter extends SimpleApplication implements ActionListener {

    private BulletAppState bulletAppState;
    private BetterCharacterControl physicsCharacter;
    private Node characterNode;
    private CameraNode camNode;
    boolean rotate = false;
    private Vector3f walkDirection = new Vector3f(0, 0, 0);
    private Vector3f viewDirection = new Vector3f(0, 0, 1);
    boolean leftStrafe = false, rightStrafe = false, forward = false, backward = false,
            leftRotate = false, rightRotate = false;
    private Vector3f normalGravity = new Vector3f(0, -9.81f, 0);
    private Geometry planet;

    public static void main(String[] args) {
        TestBetterCharacter app = new TestBetterCharacter();
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL2);
        settings.setAudioRenderer(AppSettings.LWJGL_OPENAL);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        //setup keyboard mapping
        setupKeys();

        // activate physics
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.setDebugEnabled(true);

        // init a physics test scene
        PhysicsTestHelper.createPhysicsTestWorldSoccer(rootNode, assetManager, bulletAppState.getPhysicsSpace());
        PhysicsTestHelper.createBallShooter(this, rootNode, bulletAppState.getPhysicsSpace());
        setupPlanet();

        // Create a node for the character model
        characterNode = new Node("character node");
        characterNode.setLocalTranslation(new Vector3f(4, 5, 2));

        // Add a character control to the node so we can add other things and
        // control the model rotation
        physicsCharacter = new BetterCharacterControl(0.3f, 2.5f, 8f);
        characterNode.addControl(physicsCharacter);
        getPhysicsSpace().add(physicsCharacter);

        // Load model, attach to character node
        Node model = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        model.setLocalScale(1.50f);
        characterNode.attachChild(model);

        // Add character node to the rootNode
        rootNode.attachChild(characterNode);

        // Set forward camera node that follows the character, only used when
        // view is "locked"
        camNode = new CameraNode("CamNode", cam);
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        camNode.setLocalTranslation(new Vector3f(0, 2, -6));
        Quaternion quat = new Quaternion();
        // These coordinates are local, the camNode is attached to the character node!
        quat.lookAt(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
        camNode.setLocalRotation(quat);
        characterNode.attachChild(camNode);
        // Disable by default, can be enabled via keyboard shortcut
        camNode.setEnabled(false);
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Apply planet gravity to character if close enough (see below)
        checkPlanetGravity();

        // Get current forward and left vectors of model by using its rotation
        // to rotate the unit vectors
        Vector3f modelForwardDir = characterNode.getWorldRotation().mult(Vector3f.UNIT_Z);
        Vector3f modelLeftDir = characterNode.getWorldRotation().mult(Vector3f.UNIT_X);

        // WalkDirection is global!
        // You *can* make your character fly with this.
        walkDirection.set(0, 0, 0);
        if (leftStrafe) {
            walkDirection.addLocal(modelLeftDir.mult(3));
        } else if (rightStrafe) {
            walkDirection.addLocal(modelLeftDir.negate().multLocal(3));
        }
        if (forward) {
            walkDirection.addLocal(modelForwardDir.mult(3));
        } else if (backward) {
            walkDirection.addLocal(modelForwardDir.negate().multLocal(3));
        }
        physicsCharacter.setWalkDirection(walkDirection);

        // ViewDirection is local to characters physics system!
        // The final world rotation depends on the gravity and on the state of
        // setApplyPhysicsLocal()
        if (leftRotate) {
            Quaternion rotateL = new Quaternion().fromAngleAxis(FastMath.PI * tpf, Vector3f.UNIT_Y);
            rotateL.multLocal(viewDirection);
        } else if (rightRotate) {
            Quaternion rotateR = new Quaternion().fromAngleAxis(-FastMath.PI * tpf, Vector3f.UNIT_Y);
            rotateR.multLocal(viewDirection);
        }
        physicsCharacter.setViewDirection(viewDirection);
        fpsText.setText("Touch da ground = " + physicsCharacter.isOnGround());
        if (!lockView) {
            cam.lookAt(characterNode.getWorldTranslation().add(new Vector3f(0, 2, 0)), Vector3f.UNIT_Y);
        }
    }

    private void setupPlanet() {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));
        //immovable sphere with mesh collision shape
        Sphere sphere = new Sphere(64, 64, 20);
        planet = new Geometry("Sphere", sphere);
        planet.setMaterial(material);
        planet.setLocalTranslation(30, -15, 30);
        planet.addControl(new RigidBodyControl(new MeshCollisionShape(sphere), 0));
        rootNode.attachChild(planet);
        getPhysicsSpace().add(planet);
    }

    private void checkPlanetGravity() {
        Vector3f planetDist = planet.getWorldTranslation().subtract(characterNode.getWorldTranslation());
        if (planetDist.length() < 24) {
            physicsCharacter.setGravity(planetDist.normalizeLocal().multLocal(9.81f));
        } else {
            physicsCharacter.setGravity(normalGravity);
        }
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Strafe Left")) {
            if (value) {
                leftStrafe = true;
            } else {
                leftStrafe = false;
            }
        } else if (binding.equals("Strafe Right")) {
            if (value) {
                rightStrafe = true;
            } else {
                rightStrafe = false;
            }
        } else if (binding.equals("Rotate Left")) {
            if (value) {
                leftRotate = true;
            } else {
                leftRotate = false;
            }
        } else if (binding.equals("Rotate Right")) {
            if (value) {
                rightRotate = true;
            } else {
                rightRotate = false;
            }
        } else if (binding.equals("Walk Forward")) {
            if (value) {
                forward = true;
            } else {
                forward = false;
            }
        } else if (binding.equals("Walk Backward")) {
            if (value) {
                backward = true;
            } else {
                backward = false;
            }
        } else if (binding.equals("Jump")) {
            physicsCharacter.jump();
        } else if (binding.equals("Duck")) {
            if (value) {
                physicsCharacter.setDucked(true);
            } else {
                physicsCharacter.setDucked(false);
            }
        } else if (binding.equals("Lock View")) {
            if (value && lockView) {
                lockView = false;
            } else if (value && !lockView) {
                lockView = true;
            }
            flyCam.setEnabled(!lockView);
            camNode.setEnabled(lockView);
        }
    }
    private boolean lockView = false;

    private void setupKeys() {
        inputManager.addMapping("Strafe Left",
                new KeyTrigger(KeyInput.KEY_U),
                new KeyTrigger(KeyInput.KEY_Z));
        inputManager.addMapping("Strafe Right",
                new KeyTrigger(KeyInput.KEY_O),
                new KeyTrigger(KeyInput.KEY_X));
        inputManager.addMapping("Rotate Left",
                new KeyTrigger(KeyInput.KEY_J),
                new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Rotate Right",
                new KeyTrigger(KeyInput.KEY_L),
                new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Walk Forward",
                new KeyTrigger(KeyInput.KEY_I),
                new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Walk Backward",
                new KeyTrigger(KeyInput.KEY_K),
                new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Jump",
                new KeyTrigger(KeyInput.KEY_F),
                new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Duck",
                new KeyTrigger(KeyInput.KEY_G),
                new KeyTrigger(KeyInput.KEY_LSHIFT),
                new KeyTrigger(KeyInput.KEY_RSHIFT));
        inputManager.addMapping("Lock View",
                new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "Strafe Left", "Strafe Right");
        inputManager.addListener(this, "Rotate Left", "Rotate Right");
        inputManager.addListener(this, "Walk Forward", "Walk Backward");
        inputManager.addListener(this, "Jump", "Duck", "Lock View");
    }

    @Override
    public void simpleRender(RenderManager rm) {
    }
}
