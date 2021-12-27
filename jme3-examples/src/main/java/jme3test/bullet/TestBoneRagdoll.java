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
import com.jme3.bullet.animation.PhysicsLink;
import com.jme3.bullet.animation.RagdollCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
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
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.texture.Texture;

/**
 * @author normenhansen
 */
public class TestBoneRagdoll
        extends SimpleApplication
        implements ActionListener, RagdollCollisionListener {

    private AnimComposer composer;
    private DynamicAnimControl ragdoll;
    private float bulletSize = 1f;
    private Material matBullet;
    private Node model;
    private PhysicsSpace physicsSpace;
    private Sphere bullet;
    private SphereCollisionShape bulletCollisionShape;

    public static void main(String[] args) {
        TestBoneRagdoll app = new TestBoneRagdoll();
        app.start();
    }

    public void onStandDone() {
        composer.setCurrentAction("IdleTop");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("boom") && !isPressed) {
            Geometry bulletGeometry = new Geometry("bullet", bullet);
            bulletGeometry.setMaterial(matBullet);
            bulletGeometry.setLocalTranslation(cam.getLocation());
            bulletGeometry.setLocalScale(bulletSize);
            bulletCollisionShape = new SphereCollisionShape(bulletSize);
            BombControl bulletNode = new BombControl(assetManager, bulletCollisionShape, 1f);
            bulletNode.setForceFactor(8f);
            bulletNode.setExplosionRadius(20f);
            bulletNode.setCcdMotionThreshold(0.001f);
            bulletNode.setLinearVelocity(cam.getDirection().mult(180f));
            bulletGeometry.addControl(bulletNode);
            rootNode.attachChild(bulletGeometry);
            physicsSpace.add(bulletNode);
        }
        if (name.equals("bullet+") && isPressed) {
            bulletSize += 0.1f;
        }
        if (name.equals("bullet-") && isPressed) {
            bulletSize -= 0.1f;
        }
        if (name.equals("shoot") && !isPressed) {
            Geometry bulletg = new Geometry("bullet", bullet);
            bulletg.setMaterial(matBullet);
            bulletg.setLocalTranslation(cam.getLocation());
            bulletg.setLocalScale(bulletSize);
            bulletCollisionShape = new SphereCollisionShape(bulletSize);
            RigidBodyControl bulletNode = new RigidBodyControl(bulletCollisionShape, bulletSize * 10f);
            bulletNode.setCcdMotionThreshold(0.001f);
            bulletNode.setLinearVelocity(cam.getDirection().mult(80f));
            bulletg.addControl(bulletNode);
            rootNode.attachChild(bulletg);
            physicsSpace.add(bulletNode);
        }
        if (name.equals("stop") && isPressed) {
            ragdoll.setEnabled(!ragdoll.isEnabled());
            ragdoll.setRagdollMode();
        }
        if (name.equals("toggle") && isPressed) {
            Vector3f v = new Vector3f(model.getLocalTranslation());
            v.y = 0f;
            Quaternion q = new Quaternion();
            float[] angles = new float[3];
            model.getLocalRotation().toAngles(angles);
            q.fromAngleAxis(angles[1], Vector3f.UNIT_Y);
            Transform endModelTransform
                    = new Transform(v, q, new Vector3f(1f, 1f, 1f));
            if (angles[0] < 0f) {
                composer.setCurrentAction("BackOnce");
                ragdoll.blendToKinematicMode(0.5f, endModelTransform);
            } else {
                composer.setCurrentAction("FrontOnce");
                ragdoll.blendToKinematicMode(0.5f, endModelTransform);
            }
        }
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(50f);
        cam.setLocation(new Vector3f(0.3f, 6.7f, 22.3f));
        cam.setRotation(new Quaternion(-2E-4f, 0.993025f, -0.1179f, -0.0019f));

        initCrossHairs();
        initMaterial();
        setupKeys();
        setupLight();

        BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.setDebugEnabled(true);
        physicsSpace = bulletAppState.getPhysicsSpace();

        bullet = new Sphere(32, 32, 1f, true, false);
        bullet.setTextureMode(TextureMode.Projected);
        bulletCollisionShape = new SphereCollisionShape(1f);

        PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager,
                physicsSpace);

        model = (Node) assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
        rootNode.attachChild(model);

        composer = model.getControl(AnimComposer.class);
        composer.setCurrentAction("Dance");

        Action standUpFront = composer.action("StandUpFront");
        composer.actionSequence("FrontOnce",
                standUpFront, Tweens.callMethod(this, "onStandDone"));
        Action standUpBack = composer.action("StandUpBack");
        composer.actionSequence("BackOnce",
                standUpBack, Tweens.callMethod(this, "onStandDone"));

        ragdoll = new DynamicAnimControl();
        TestRagdollCharacter.setupSinbad(ragdoll);
        model.addControl(ragdoll);
        physicsSpace.add(ragdoll);
        ragdoll.addCollisionListener(this);
    }

    @Override
    public void collide(PhysicsLink bone, PhysicsCollisionObject object,
            PhysicsCollisionEvent event) {
        if (object.getUserObject() != null
                && object.getUserObject() instanceof Geometry) {
            Geometry geom = (Geometry) object.getUserObject();
            if ("bullet".equals(geom.getName())) {
                ragdoll.setRagdollMode();
            }
        }
    }

    private void initCrossHairs() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2f);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
                settings.getWidth() / 2f - guiFont.getCharSet().getRenderedSize() / 3f * 2f,
                settings.getHeight() / 2f + ch.getLineHeight() / 2f, 0f);
        guiNode.attachChild(ch);
    }

    private void initMaterial() {
        matBullet = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key2 = new TextureKey("Textures/Terrain/Rock/Rock.PNG");
        key2.setGenerateMips(true);
        Texture tex2 = assetManager.loadTexture(key2);
        matBullet.setTexture("ColorMap", tex2);
    }

    private void setupKeys() {
        inputManager.addMapping("boom", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping("bullet+", new KeyTrigger(KeyInput.KEY_PERIOD));
        inputManager.addMapping("bullet-", new KeyTrigger(KeyInput.KEY_COMMA));
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("stop", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));

        inputManager.addListener(this,
                "boom", "bullet-", "bullet+", "shoot", "stop", "toggle");

    }

    private void setupLight() {
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -0.7f, -1f).normalizeLocal());
        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1f));
        rootNode.addLight(dl);
    }
}
