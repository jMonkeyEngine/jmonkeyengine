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

import com.jme3.anim.AnimComposer;
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.tween.action.Action;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.animation.RangeOfMotion;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;

/**
 * @author normenhansen
 */
public class TestRagdollCharacter
        extends SimpleApplication
        implements ActionListener {

    private AnimComposer composer;
    private boolean forward = false, backward = false,
            leftRotate = false, rightRotate = false;
    private Node model;
    private PhysicsSpace physicsSpace;

    public static void main(String[] args) {
        TestRagdollCharacter app = new TestRagdollCharacter();
        app.start();
    }

    public void onSliceDone() {
        composer.setCurrentAction("IdleTop");
    }

    static void setupSinbad(DynamicAnimControl ragdoll) {
        ragdoll.link("Waist", 1f,
                new RangeOfMotion(1f, -0.4f, 0.8f, -0.8f, 0.4f, -0.4f));
        ragdoll.link("Chest", 1f, new RangeOfMotion(0.4f, 0f, 0.4f));
        ragdoll.link("Neck", 1f, new RangeOfMotion(0.5f, 1f, 0.7f));

        ragdoll.link("Clavicle.R", 1f,
                new RangeOfMotion(0.3f, -0.6f, 0f, 0f, 0.4f, -0.4f));
        ragdoll.link("Humerus.R", 1f,
                new RangeOfMotion(1.6f, -0.8f, 1f, -1f, 1.6f, -1f));
        ragdoll.link("Ulna.R", 1f, new RangeOfMotion(0f, 0f, 1f, -1f, 0f, -2f));
        ragdoll.link("Hand.R", 1f, new RangeOfMotion(0.8f, 0f, 0.2f));

        ragdoll.link("Clavicle.L", 1f,
                new RangeOfMotion(0.6f, -0.3f, 0f, 0f, 0.4f, -0.4f));
        ragdoll.link("Humerus.L",
                1f, new RangeOfMotion(0.8f, -1.6f, 1f, -1f, 1f, -1.6f));
        ragdoll.link("Ulna.L", 1f, new RangeOfMotion(0f, 0f, 1f, -1f, 2f, 0f));
        ragdoll.link("Hand.L", 1f, new RangeOfMotion(0.8f, 0f, 0.2f));

        ragdoll.link("Thigh.R", 1f,
                new RangeOfMotion(0.4f, -1f, 0.4f, -0.4f, 1f, -0.5f));
        ragdoll.link("Calf.R", 1f, new RangeOfMotion(2f, 0f, 0f, 0f, 0f, 0f));
        ragdoll.link("Foot.R", 1f, new RangeOfMotion(0.3f, 0.5f, 0f));

        ragdoll.link("Thigh.L", 1f,
                new RangeOfMotion(0.4f, -1f, 0.4f, -0.4f, 0.5f, -1f));
        ragdoll.link("Calf.L", 1f, new RangeOfMotion(2f, 0f, 0f, 0f, 0f, 0f));
        ragdoll.link("Foot.L", 1f, new RangeOfMotion(0.3f, 0.5f, 0f));
    }

    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
        if (binding.equals("Rotate Left")) {
            if (isPressed) {
                leftRotate = true;
            } else {
                leftRotate = false;
            }
        } else if (binding.equals("Rotate Right")) {
            if (isPressed) {
                rightRotate = true;
            } else {
                rightRotate = false;
            }
        } else if (binding.equals("Slice")) {
            if (isPressed) {
                composer.setCurrentAction("SliceOnce");
            }
        } else if (binding.equals("Walk Forward")) {
            if (isPressed) {
                forward = true;
            } else {
                forward = false;
            }
        } else if (binding.equals("Walk Backward")) {
            if (isPressed) {
                backward = true;
            } else {
                backward = false;
            }
        }
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(50f);
        cam.setLocation(new Vector3f(-16f, 4.7f, -1.6f));
        cam.setRotation(new Quaternion(0.0484f, 0.804337f, -0.066f, 0.5885f));

        setupKeys();
        setupLight();

        BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.setDebugEnabled(true);
        physicsSpace = bulletAppState.getPhysicsSpace();

        PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager,
                physicsSpace);
        initWall(2f, 1f, 1f);

        model = (Node) assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
        rootNode.attachChild(model);
        model.lookAt(new Vector3f(0f, 0f, -1f), Vector3f.UNIT_Y);
        model.setLocalTranslation(4f, 0f, -7f);

        composer = model.getControl(AnimComposer.class);
        composer.setCurrentAction("IdleTop");

        Action slice = composer.action("SliceHorizontal");
        composer.actionSequence("SliceOnce",
                slice, Tweens.callMethod(this, "onSliceDone"));

        DynamicAnimControl ragdoll = new DynamicAnimControl();
        setupSinbad(ragdoll);
        model.addControl(ragdoll);
        physicsSpace.add(ragdoll);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (forward) {
            model.move(model.getLocalRotation().multLocal(new Vector3f(0f, 0f, tpf)));
        } else if (backward) {
            model.move(model.getLocalRotation().multLocal(new Vector3f(0f, 0f, -tpf)));
        } else if (leftRotate) {
            model.rotate(0f, tpf, 0f);
        } else if (rightRotate) {
            model.rotate(0f, -tpf, 0f);
        }
    }

    private void initWall(float bLength, float bWidth, float bHeight) {
        Box brick = new Box(bLength, bHeight, bWidth);
        brick.scaleTextureCoordinates(new Vector2f(1f, 0.5f));
        Material mat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key = new TextureKey("Textures/Terrain/BrickWall/BrickWall.jpg");
        key.setGenerateMips(true);
        Texture tex = assetManager.loadTexture(key);
        mat2.setTexture("ColorMap", tex);

        float startpt = bLength / 4f;
        float height = -5f;
        for (int j = 0; j < 15; j++) {
            for (int i = 0; i < 4; i++) {
                Vector3f ori = new Vector3f(i * bLength * 2f + startpt, bHeight + height, -10f);
                Geometry brickGeometry = new Geometry("brick", brick);
                brickGeometry.setMaterial(mat2);
                brickGeometry.setLocalTranslation(ori);
                // for geometry with sphere mesh the physics system automatically uses a sphere collision shape
                brickGeometry.addControl(new RigidBodyControl(1.5f));
                brickGeometry.setShadowMode(ShadowMode.CastAndReceive);
                brickGeometry.getControl(RigidBodyControl.class).setFriction(0.6f);
                this.rootNode.attachChild(brickGeometry);
                physicsSpace.add(brickGeometry);
            }
            startpt = -startpt;
            height += 2f * bHeight;
        }
    }

    private void setupKeys() {
        inputManager.addMapping("Rotate Left",
                new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("Rotate Right",
                new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("Walk Backward",
                new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("Walk Forward",
                new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("Slice",
                new KeyTrigger(KeyInput.KEY_SPACE),
                new KeyTrigger(KeyInput.KEY_RETURN));

        inputManager.addListener(this, "Rotate Left", "Rotate Right", "Slice",
                "Walk Backward", "Walk Forward");
    }

    private void setupLight() {
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -0.7f, -1f).normalizeLocal());
        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1f));
        rootNode.addLight(dl);
    }
}
