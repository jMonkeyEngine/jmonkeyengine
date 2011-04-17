/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.bullet.BulletAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.RagdollCollisionListener;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RagdollControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.SkeletonDebugger;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.texture.Texture;

/**
 * PHYSICS RAGDOLLS ARE NOT WORKING PROPERLY YET!
 * @author normenhansen
 */
public class TestBoneRagdoll extends SimpleApplication implements RagdollCollisionListener {

    private BulletAppState bulletAppState;
    Material matBullet;
    Node model;
    RagdollControl ragdoll;
    float bulletSize = 1f;

    public static void main(String[] args) {
        TestBoneRagdoll app = new TestBoneRagdoll();
        app.start();
    }

    public void simpleInitApp() {
        initCrossHairs();
        initMaterial();

        cam.setLocation(new Vector3f(0.26924422f, 6.646658f, 22.265987f));
        cam.setRotation(new Quaternion(-2.302544E-4f, 0.99302495f, -0.117888905f, -0.0019395084f));


        bulletAppState = new BulletAppState();
        bulletAppState.setEnabled(true);
        stateManager.attach(bulletAppState);
        //bulletAppState.getPhysicsSpace().enableDebug(assetManager);
        PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
        setupLight();

        model = (Node) assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
        //    model.setLocalTranslation(5, 0, 5);
        //  model.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));

        //debug view
        AnimControl control = model.getControl(AnimControl.class);
        SkeletonDebugger skeletonDebug = new SkeletonDebugger("skeleton", control.getSkeleton());
        Material mat2 = new Material(assetManager, "Common/MatDefs/Misc/WireColor.j3md");
        mat2.setColor("Color", ColorRGBA.Green);
        mat2.getAdditionalRenderState().setDepthTest(false);
        skeletonDebug.setMaterial(mat2);
        skeletonDebug.setLocalTranslation(model.getLocalTranslation());

        //Note: PhysicsRagdollControl is still TODO, constructor will change
        ragdoll = new RagdollControl(0.7f);
        setupSinbad(ragdoll);
        ragdoll.addCollisionListener(this);
        model.addControl(ragdoll);

        float eighth_pi = FastMath.PI * 0.125f;

        //Oto's head is almost rigid
        //    ragdoll.setJointLimit("head", 0, 0, eighth_pi, -eighth_pi, 0, 0);

        getPhysicsSpace().add(ragdoll);
        speed = 1.3f;

        rootNode.attachChild(model);
        // rootNode.attachChild(skeletonDebug);
        flyCam.setMoveSpeed(50);


        final AnimChannel channel = control.createChannel();
        channel.setAnim("Dance");

        inputManager.addListener(new ActionListener() {

            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("toggle") && isPressed) {
                    ragdoll.setControl(false);
                    model.setLocalTranslation(0, 0, 0);
                }
                if (name.equals("bullet+") && isPressed) {
                    bulletSize += 0.1f;

                }
                if (name.equals("bullet-") && isPressed) {
                    bulletSize -= 0.1f;

                }
                if (name.equals("shoot") && isPressed) {
//                    bulletSize = 0;
                }
                if (name.equals("stop") && isPressed) {
                    bulletAppState.setEnabled(!bulletAppState.isEnabled());

                }
                if (name.equals("shoot") && !isPressed) {
                    Geometry bulletg = new Geometry("bullet", bullet);
                    bulletg.setMaterial(matBullet);
                    bulletg.setLocalTranslation(cam.getLocation());
                    bulletg.setLocalScale(bulletSize);
                    bulletCollisionShape = new SphereCollisionShape(bulletSize);
                    //    RigidBodyControl bulletNode = new BombControl(assetManager, bulletCollisionShape, 1);
                    RigidBodyControl bulletNode = new RigidBodyControl(bulletCollisionShape, bulletSize * 10);
                    bulletNode.setCcdMotionThreshold(0.001f);
                    bulletNode.setLinearVelocity(cam.getDirection().mult(80));
                    bulletg.addControl(bulletNode);
                    rootNode.attachChild(bulletg);
                    getPhysicsSpace().add(bulletNode);


                }
            }
        }, "toggle", "shoot", "stop", "bullet+", "bullet-");
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("stop", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("bullet-", new KeyTrigger(KeyInput.KEY_COMMA));
        inputManager.addMapping("bullet+", new KeyTrigger(KeyInput.KEY_PERIOD));


    }

    private void setupLight() {
        AmbientLight al = new AmbientLight();
        //  al.setColor(ColorRGBA.White.mult(1));
        //   rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -0.7f, -1).normalizeLocal());
        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1.0f));
        rootNode.addLight(dl);
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }
    Material mat;
    Material mat3;
    private static final Sphere bullet;
    private static SphereCollisionShape bulletCollisionShape;

    static {
        bullet = new Sphere(32, 32, 1.0f, true, false);
        bullet.setTextureMode(TextureMode.Projected);
        bulletCollisionShape = new SphereCollisionShape(1.0f);
    }

    public void initMaterial() {

        matBullet = new Material(assetManager, "Common/MatDefs/Misc/SimpleTextured.j3md");
        TextureKey key2 = new TextureKey("Textures/Terrain/Rock/Rock.PNG");
        key2.setGenerateMips(true);
        Texture tex2 = assetManager.loadTexture(key2);
        matBullet.setTexture("ColorMap", tex2);
    }

    protected void initCrossHairs() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }

    public void collide(Bone bone, PhysicsCollisionObject object) {

        if (object.getUserObject() != null && object.getUserObject() instanceof Geometry) {
            Geometry geom = (Geometry) object.getUserObject();
            if ("Floor".equals(geom.getName())) {
                return;
            }
        }


        if (!ragdoll.hasControl()) {

            //bulletTime();
            ragdoll.setControl(true);

        }
    }

    private void setupSinbad(RagdollControl ragdoll) {
        ragdoll.addBoneName("Ulna.L");
        ragdoll.addBoneName("Ulna.R");
        ragdoll.addBoneName("Chest");
        ragdoll.addBoneName("Foot.L");
        ragdoll.addBoneName("Foot.R");
        ragdoll.addBoneName("Hand.R");
        ragdoll.addBoneName("Hand.L");
        ragdoll.addBoneName("Neck");
        ragdoll.addBoneName("Head");
        ragdoll.addBoneName("Root");
        ragdoll.addBoneName("Stomach");
        ragdoll.addBoneName("Waist");
        ragdoll.addBoneName("Humerus.L");
        ragdoll.addBoneName("Humerus.R");
        ragdoll.addBoneName("Thigh.L");
        ragdoll.addBoneName("Thigh.R");
        ragdoll.addBoneName("Calf.L");
        ragdoll.addBoneName("Calf.R");
        ragdoll.addBoneName("Clavicle.L");
        ragdoll.addBoneName("Clavicle.R");

//        <boneparent bone="ThumbMed.R" parent="ThumbProx.R" />
//        <boneparent bone="IndexFingerMed.R" parent="IndexFingerProx.R" />
//        <boneparent bone="Clavicle.R" parent="Chest" />
//        <boneparent bone="PinkyDist.L" parent="PinkyMed.L" />
//        <boneparent bone="IndexFingerDist.R" parent="IndexFingerMed.R" />
//        <boneparent bone="Cheek.L" parent="Head" />
//        <boneparent bone="MiddleFingerMed.L" parent="MiddleFingerProx.L" />
//        <boneparent bone="Jaw" parent="Head" />
//        <boneparent bone="TongueMid" parent="TongueBase" />
//        <boneparent bone="Ulna.L" parent="Humerus.L" />
//        <boneparent bone="Handle.R" parent="Hand.R" />
//        <boneparent bone="Ulna.R" parent="Humerus.R" />
//        <boneparent bone="Chest" parent="Stomach" />
//        <boneparent bone="Foot.L" parent="Calf.L" />
//        <boneparent bone="Foot.R" parent="Calf.R" />
//        <boneparent bone="Hand.R" parent="Ulna.R" />
//        <boneparent bone="IndexFingerDist.L" parent="IndexFingerMed.L" />
//        <boneparent bone="Cheek.R" parent="Head" />
//        <boneparent bone="PinkyDist.R" parent="PinkyMed.R" />
//        <boneparent bone="IndexFingerProx.R" parent="Hand.R" />
//        <boneparent bone="Handle.L" parent="Hand.L" />
//        <boneparent bone="RingFingerProx.R" parent="Hand.R" />
//        <boneparent bone="LowerLip" parent="Jaw" />
//        <boneparent bone="Neck" parent="Chest" />
//        <boneparent bone="TongueBase" parent="Jaw" />
//        <boneparent bone="Head" parent="Neck" />
//        <boneparent bone="Sheath.R" parent="Chest" />
//        <boneparent bone="Stomach" parent="Waist" />
//        <boneparent bone="Toe.L" parent="Foot.L" />
//        <boneparent bone="MiddleFingerProx.L" parent="Hand.L" />
//        <boneparent bone="RingFingerMed.L" parent="RingFingerProx.L" />
//        <boneparent bone="PinkyMed.L" parent="PinkyProx.L" />
//        <boneparent bone="MiddleFingerMed.R" parent="MiddleFingerProx.R" />
//        <boneparent bone="ThumbProx.L" parent="Hand.L" />
//        <boneparent bone="PinkyMed.R" parent="PinkyProx.R" />
//        <boneparent bone="Clavicle.L" parent="Chest" />
//        <boneparent bone="MiddleFingerProx.R" parent="Hand.R" />
//        <boneparent bone="Toe.R" parent="Foot.R" />
//        <boneparent bone="Sheath.L" parent="Chest" />
//        <boneparent bone="TongueTip" parent="TongueMid" />
//        <boneparent bone="RingFingerProx.L" parent="Hand.L" />
//        <boneparent bone="Waist" parent="Root" />
//        <boneparent bone="MiddleFingerDist.R" parent="MiddleFingerMed.R" />
//        <boneparent bone="Hand.L" parent="Ulna.L" />
//        <boneparent bone="Humerus.R" parent="Clavicle.R" />
//        <boneparent bone="RingFingerDist.L" parent="RingFingerMed.L" />
//        <boneparent bone="Eye.L" parent="Head" />
//        <boneparent bone="Humerus.L" parent="Clavicle.L" />
//        <boneparent bone="RingFingerDist.R" parent="RingFingerMed.R" />
//        <boneparent bone="MiddleFingerDist.L" parent="MiddleFingerMed.L" />
//        <boneparent bone="IndexFingerMed.L" parent="IndexFingerProx.L" />
//        <boneparent bone="ThumbMed.L" parent="ThumbProx.L" />
//        <boneparent bone="Thigh.L" parent="Root" />
//        <boneparent bone="UpperLip" parent="Head" />
//        <boneparent bone="RingFingerMed.R" parent="RingFingerProx.R" />
//        <boneparent bone="Eye.R" parent="Head" />
//        <boneparent bone="Brow.L" parent="Head" />
//        <boneparent bone="Brow.C" parent="Head" />
//        <boneparent bone="Calf.L" parent="Thigh.L" />
//        <boneparent bone="PinkyProx.L" parent="Hand.L" />
//        <boneparent bone="ThumbDist.L" parent="ThumbMed.L" />
//        <boneparent bone="ThumbProx.R" parent="Hand.R" />
//        <boneparent bone="ThumbDist.R" parent="ThumbMed.R" />
//        <boneparent bone="Calf.R" parent="Thigh.R" />
//        <boneparent bone="PinkyProx.R" parent="Hand.R" />
//        <boneparent bone="IndexFingerProx.L" parent="Hand.L" />
//        <boneparent bone="Brow.R" parent="Head" />
//        <boneparent bone="Thigh.R" parent="Root" />

    }

    private void bulletTime() {
        speed = 0.1f;
        elTime = 0;
    }
    float elTime = 0;
    boolean forward = true;

    @Override
    public void simpleUpdate(float tpf) {
        //  System.out.println(model.getLocalTranslation());
//        elTime += tpf;
//        if (elTime > 0.05f && speed < 1.0f) {
//            speed += tpf * 8;
//        }
//        timer += tpf;
        fpsText.setText("Bullet Size: " + bulletSize);
        if (!ragdoll.hasControl()) {
            if (model.getLocalTranslation().getZ() < -10) {
                forward = true;
            } else if (model.getLocalTranslation().getZ() > 10) {
                forward = false;
            }
            if (forward) {
                model.move(-tpf, 0, tpf);
            } else {
                model.move(tpf, 0, -tpf);
            }
        }
    }
}
