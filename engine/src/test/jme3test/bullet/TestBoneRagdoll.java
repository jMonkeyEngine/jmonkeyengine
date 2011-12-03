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

import com.jme3.animation.*;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.RagdollCollisionListener;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.KinematicRagdollControl;
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
public class TestBoneRagdoll extends SimpleApplication implements RagdollCollisionListener, AnimEventListener {

    private BulletAppState bulletAppState;
    Material matBullet;
    Node model;
    KinematicRagdollControl ragdoll;
    float bulletSize = 1f;
    Material mat;
    Material mat3;
    private Sphere bullet;
    private SphereCollisionShape bulletCollisionShape;

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
        bullet = new Sphere(32, 32, 1.0f, true, false);
        bullet.setTextureMode(TextureMode.Projected);
        bulletCollisionShape = new SphereCollisionShape(1.0f);

//        bulletAppState.getPhysicsSpace().enableDebug(assetManager);
        PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
        setupLight();

        model = (Node) assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");

        //  model.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));

        //debug view
        AnimControl control = model.getControl(AnimControl.class);
        SkeletonDebugger skeletonDebug = new SkeletonDebugger("skeleton", control.getSkeleton());
        Material mat2 = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat2.getAdditionalRenderState().setWireframe(true);
        mat2.setColor("Color", ColorRGBA.Green);
        mat2.getAdditionalRenderState().setDepthTest(false);
        skeletonDebug.setMaterial(mat2);
        skeletonDebug.setLocalTranslation(model.getLocalTranslation());

        //Note: PhysicsRagdollControl is still TODO, constructor will change
        ragdoll = new KinematicRagdollControl(0.5f);
        setupSinbad(ragdoll);
        ragdoll.addCollisionListener(this);
        model.addControl(ragdoll);

        float eighth_pi = FastMath.PI * 0.125f;
        ragdoll.setJointLimit("Waist", eighth_pi, eighth_pi, eighth_pi, eighth_pi, eighth_pi, eighth_pi);
        ragdoll.setJointLimit("Chest", eighth_pi, eighth_pi, 0, 0, eighth_pi, eighth_pi);


        //Oto's head is almost rigid
        //    ragdoll.setJointLimit("head", 0, 0, eighth_pi, -eighth_pi, 0, 0);

        getPhysicsSpace().add(ragdoll);
        speed = 1.3f;

        rootNode.attachChild(model);
        // rootNode.attachChild(skeletonDebug);
        flyCam.setMoveSpeed(50);


        animChannel = control.createChannel();
        animChannel.setAnim("Dance");
        control.addListener(this);

        inputManager.addListener(new ActionListener() {

            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("toggle") && isPressed) {

                    Vector3f v = new Vector3f();
                    v.set(model.getLocalTranslation());
                    v.y = 0;
                    model.setLocalTranslation(v);
                    Quaternion q = new Quaternion();
                    float[] angles = new float[3];
                    model.getLocalRotation().toAngles(angles);
                    q.fromAngleAxis(angles[1], Vector3f.UNIT_Y);
                    model.setLocalRotation(q);
                    if (angles[0] < 0) {
                        animChannel.setAnim("StandUpBack");
                        ragdoll.blendToKinematicMode(0.5f);
                    } else {
                        animChannel.setAnim("StandUpFront");
                        ragdoll.blendToKinematicMode(0.5f);
                    }

                }
                if (name.equals("bullet+") && isPressed) {
                    bulletSize += 0.1f;

                }
                if (name.equals("bullet-") && isPressed) {
                    bulletSize -= 0.1f;

                }

                if (name.equals("stop") && isPressed) {
                    ragdoll.setEnabled(!ragdoll.isEnabled());
                    ragdoll.setRagdollMode();
                }

                if (name.equals("shoot") && !isPressed) {
                    Geometry bulletg = new Geometry("bullet", bullet);
                    bulletg.setMaterial(matBullet);
                    bulletg.setLocalTranslation(cam.getLocation());
                    bulletg.setLocalScale(bulletSize);
                    bulletCollisionShape = new SphereCollisionShape(bulletSize);
                    RigidBodyControl bulletNode = new RigidBodyControl(bulletCollisionShape, bulletSize * 10);
                    bulletNode.setCcdMotionThreshold(0.001f);
                    bulletNode.setLinearVelocity(cam.getDirection().mult(80));
                    bulletg.addControl(bulletNode);
                    rootNode.attachChild(bulletg);
                    getPhysicsSpace().add(bulletNode);
                }
                if (name.equals("boom") && !isPressed) {
                    Geometry bulletg = new Geometry("bullet", bullet);
                    bulletg.setMaterial(matBullet);
                    bulletg.setLocalTranslation(cam.getLocation());
                    bulletg.setLocalScale(bulletSize);
                    bulletCollisionShape = new SphereCollisionShape(bulletSize);
                    BombControl bulletNode = new BombControl(assetManager, bulletCollisionShape, 1);
                    bulletNode.setForceFactor(8);
                    bulletNode.setExplosionRadius(20);
                    bulletNode.setCcdMotionThreshold(0.001f);
                    bulletNode.setLinearVelocity(cam.getDirection().mult(180));
                    bulletg.addControl(bulletNode);
                    rootNode.attachChild(bulletg);
                    getPhysicsSpace().add(bulletNode);
                }
            }
        }, "toggle", "shoot", "stop", "bullet+", "bullet-", "boom");
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("boom", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping("stop", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("bullet-", new KeyTrigger(KeyInput.KEY_COMMA));
        inputManager.addMapping("bullet+", new KeyTrigger(KeyInput.KEY_PERIOD));


    }

    private void setupLight() {
        // AmbientLight al = new AmbientLight();
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

    public void initMaterial() {

        matBullet = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
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

    public void collide(Bone bone, PhysicsCollisionObject object, PhysicsCollisionEvent event) {

        if (object.getUserObject() != null && object.getUserObject() instanceof Geometry) {
            Geometry geom = (Geometry) object.getUserObject();
            if ("Floor".equals(geom.getName())) {
                return;
            }
        }

        ragdoll.setRagdollMode();

    }

    private void setupSinbad(KinematicRagdollControl ragdoll) {
        ragdoll.addBoneName("Ulna.L");
        ragdoll.addBoneName("Ulna.R");
        ragdoll.addBoneName("Chest");
        ragdoll.addBoneName("Foot.L");
        ragdoll.addBoneName("Foot.R");
        ragdoll.addBoneName("Hand.R");
        ragdoll.addBoneName("Hand.L");
        ragdoll.addBoneName("Neck");
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

    }
    float elTime = 0;
    boolean forward = true;
    AnimControl animControl;
    AnimChannel animChannel;
    Vector3f direction = new Vector3f(0, 0, 1);
    Quaternion rotate = new Quaternion().fromAngleAxis(FastMath.PI / 8, Vector3f.UNIT_Y);
    boolean dance = true;

    @Override
    public void simpleUpdate(float tpf) {
        // System.out.println(((BoundingBox) model.getWorldBound()).getYExtent());
//        elTime += tpf;
//        if (elTime > 3) {
//            elTime = 0;
//            if (dance) {
//                rotate.multLocal(direction);
//            }
//            if (Math.random() > 0.80) {
//                dance = true;
//                animChannel.setAnim("Dance");
//            } else {
//                dance = false;
//                animChannel.setAnim("RunBase");
//                rotate.fromAngleAxis(FastMath.QUARTER_PI * ((float) Math.random() - 0.5f), Vector3f.UNIT_Y);
//                rotate.multLocal(direction);
//            }
//        }
//        if (!ragdoll.hasControl() && !dance) {
//            if (model.getLocalTranslation().getZ() < -10) {
//                direction.z = 1;
//                direction.normalizeLocal();
//            } else if (model.getLocalTranslation().getZ() > 10) {
//                direction.z = -1;
//                direction.normalizeLocal();
//            }
//            if (model.getLocalTranslation().getX() < -10) {
//                direction.x = 1;
//                direction.normalizeLocal();
//            } else if (model.getLocalTranslation().getX() > 10) {
//                direction.x = -1;
//                direction.normalizeLocal();
//            }
//            model.move(direction.multLocal(tpf * 8));
//            direction.normalizeLocal();
//            model.lookAt(model.getLocalTranslation().add(direction), Vector3f.UNIT_Y);
//        }
    }

    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
//        if(channel.getAnimationName().equals("StandUpFront")){
//            channel.setAnim("Dance");
//        }

        if (channel.getAnimationName().equals("StandUpBack") || channel.getAnimationName().equals("StandUpFront")) {
            channel.setLoopMode(LoopMode.DontLoop);
            channel.setAnim("IdleTop", 5);
            channel.setLoopMode(LoopMode.Loop);
        }
//        if(channel.getAnimationName().equals("IdleTop")){
//            channel.setAnim("StandUpFront");
//        }

    }

    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
    }
}
