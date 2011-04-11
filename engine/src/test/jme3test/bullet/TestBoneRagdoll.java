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
    float timer = 0;

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
        //       bulletAppState.getPhysicsSpace().enableDebug(assetManager);
        PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
        setupLight();

        model = (Node) assetManager.loadModel("Models/Oto/Oto.mesh.xml");
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
//        AnimChannel channel=control.createChannel();
//        channel.setAnim("Dodge");
//        channel.setLoopMode(LoopMode.Cycle);
//        channel.setSpeed(0.1f);



        //Note: PhysicsRagdollControl is still TODO, constructor will change
        ragdoll = new RagdollControl(1.0f);
        ragdoll.addCollisionListener(this);
        model.addControl(ragdoll);

        float eighth_pi = FastMath.PI * 0.125f;

        ragdoll.setJointLimit("head", eighth_pi, -eighth_pi, eighth_pi, -eighth_pi, eighth_pi, -eighth_pi);

        ragdoll.setJointLimit("spinehigh", FastMath.QUARTER_PI, -FastMath.QUARTER_PI, 0, 0, FastMath.QUARTER_PI, -FastMath.QUARTER_PI);

        ragdoll.setJointLimit("hip.right", FastMath.PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI);
        ragdoll.setJointLimit("hip.left", FastMath.PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI);

        ragdoll.setJointLimit("leg.left", 0, -FastMath.PI, 0, 0, 0, 0);
        ragdoll.setJointLimit("leg.right", 0, -FastMath.PI, 0, 0, 0, 0);

        ragdoll.setJointLimit("foot.right", 0, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI);
        ragdoll.setJointLimit("foot.left", 0, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI);

        ragdoll.setJointLimit("uparm.right", FastMath.HALF_PI, -FastMath.QUARTER_PI, 0, 0, FastMath.QUARTER_PI, -FastMath.HALF_PI);
        ragdoll.setJointLimit("uparm.left", FastMath.HALF_PI, -FastMath.QUARTER_PI, 0, 0, FastMath.QUARTER_PI, -FastMath.HALF_PI);
            
        ragdoll.setJointLimit("arm.right", FastMath.PI, 0, 0, 0, 0, 0);
        ragdoll.setJointLimit("arm.left", FastMath.PI, 0, 0, 0, 0, 0);

        ragdoll.setJointLimit("hand.right", FastMath.QUARTER_PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI);
        ragdoll.setJointLimit("hand.left", FastMath.QUARTER_PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI);




        getPhysicsSpace().add(ragdoll);
        speed = 1.3f;

        rootNode.attachChild(model);
        //    rootNode.attachChild(skeletonDebug);
        //flyCam.setEnabled(false);
        flyCam.setMoveSpeed(50);
//        ChaseCamera chaseCamera=new ChaseCamera(cam, inputManager);
//        chaseCamera.setLookAtOffset(Vector3f.UNIT_Y.mult(4));
//        model.addControl(chaseCamera);

        final AnimChannel channel = control.createChannel();
        channel.setAnim("Walk");

        inputManager.addListener(new ActionListener() {

            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("toggle") && isPressed) {
                    ragdoll.setControl(false);
                }
                if (name.equals("shoot") && isPressed) {
                    timer = 0;

                }
                if (name.equals("shoot") && !isPressed) {
                    Geometry bulletg = new Geometry("bullet", bullet);
                    bulletg.setMaterial(matBullet);
                    bulletg.setLocalTranslation(cam.getLocation());
                    bulletg.setLocalScale(timer);
                    bulletCollisionShape = new SphereCollisionShape(timer);
                      RigidBodyControl bulletNode = new BombControl(assetManager, bulletCollisionShape, 1);
//                    RigidBodyControl bulletNode = new RigidBodyControl(bulletCollisionShape, timer * 10);
                    bulletNode.setCcdMotionThreshold(0.001f);
                    bulletNode.setLinearVelocity(cam.getDirection().mult(80));
                    bulletg.addControl(bulletNode);
                    rootNode.attachChild(bulletg);
                    getPhysicsSpace().add(bulletNode);


                }
            }
        }, "toggle", "shoot");
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

    }

    private void setupLight() {
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(10));
        rootNode.addLight(al);

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

        if (!ragdoll.hasControl()) {

            //   bulletTime();
            ragdoll.setControl(true);

        }
    }

    private void bulletTime() {
        speed = 0.1f;
        elTime = 0;
    }
    float elTime = 0;

    @Override
    public void simpleUpdate(float tpf) {
        //  System.out.println(model.getLocalTranslation());
        elTime += tpf;
        if (elTime > 0.05f && speed < 1.0f) {
            speed += tpf * 8;
        }
        timer += tpf;
    }
}
