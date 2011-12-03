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
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.KinematicRagdollControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
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
public class TestRagdollCharacter extends SimpleApplication implements AnimEventListener, ActionListener {

    BulletAppState bulletAppState;
    Node model;
    KinematicRagdollControl ragdoll;
    boolean leftStrafe = false, rightStrafe = false, forward = false, backward = false,
            leftRotate = false, rightRotate = false;
    AnimControl animControl;
    AnimChannel animChannel;

    public static void main(String[] args) {
        TestRagdollCharacter app = new TestRagdollCharacter();
        app.start();
    }

    public void simpleInitApp() {
        setupKeys();

        bulletAppState = new BulletAppState();
        bulletAppState.setEnabled(true);
        stateManager.attach(bulletAppState);


//        bulletAppState.getPhysicsSpace().enableDebug(assetManager);
        PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
        initWall(2,1,1);
        setupLight();

        cam.setLocation(new Vector3f(-8,0,-4));
        cam.lookAt(new Vector3f(4,0,-7), Vector3f.UNIT_Y);

        model = (Node) assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
        model.lookAt(new Vector3f(0,0,-1), Vector3f.UNIT_Y);
        model.setLocalTranslation(4, 0, -7f);

        ragdoll = new KinematicRagdollControl(0.5f);
        model.addControl(ragdoll);

        getPhysicsSpace().add(ragdoll);
        speed = 1.3f;

        rootNode.attachChild(model);


        AnimControl control = model.getControl(AnimControl.class);
        animChannel = control.createChannel();
        animChannel.setAnim("IdleTop");
        control.addListener(this);

    }

    private void setupLight() {
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -0.7f, -1).normalizeLocal());
        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1.0f));
        rootNode.addLight(dl);
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    private void setupKeys() {
        inputManager.addMapping("Rotate Left",
                new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("Rotate Right",
                new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("Walk Forward",
                new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("Walk Backward",
                new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("Slice",
                new KeyTrigger(KeyInput.KEY_SPACE),
                new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "Strafe Left", "Strafe Right");
        inputManager.addListener(this, "Rotate Left", "Rotate Right");
        inputManager.addListener(this, "Walk Forward", "Walk Backward");
        inputManager.addListener(this, "Slice");
    }

    public void initWall(float bLength, float bWidth, float bHeight) {
        Box brick = new Box(Vector3f.ZERO, bLength, bHeight, bWidth);
        brick.scaleTextureCoordinates(new Vector2f(1f, .5f));
        Material mat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key = new TextureKey("Textures/Terrain/BrickWall/BrickWall.jpg");
        key.setGenerateMips(true);
        Texture tex = assetManager.loadTexture(key);
        mat2.setTexture("ColorMap", tex);
        
        float startpt = bLength / 4;
        float height = -5;
        for (int j = 0; j < 15; j++) {
            for (int i = 0; i < 4; i++) {
                Vector3f ori = new Vector3f(i * bLength * 2 + startpt, bHeight + height, -10);
                Geometry reBoxg = new Geometry("brick", brick);
                reBoxg.setMaterial(mat2);
                reBoxg.setLocalTranslation(ori);
                //for geometry with sphere mesh the physics system automatically uses a sphere collision shape
                reBoxg.addControl(new RigidBodyControl(1.5f));
                reBoxg.setShadowMode(ShadowMode.CastAndReceive);
                reBoxg.getControl(RigidBodyControl.class).setFriction(0.6f);
                this.rootNode.attachChild(reBoxg);
                this.getPhysicsSpace().add(reBoxg);
            }
            startpt = -startpt;
            height += 2 * bHeight;
        }
    }

    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {

        if (channel.getAnimationName().equals("SliceHorizontal")) {
            channel.setLoopMode(LoopMode.DontLoop);
            channel.setAnim("IdleTop", 5);
            channel.setLoopMode(LoopMode.Loop);
        }

    }

    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
    }
    
    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Rotate Left")) {
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
        } else if (binding.equals("Slice")) {
            if (value) {
                animChannel.setAnim("SliceHorizontal");
                animChannel.setSpeed(0.3f);
            }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        if(forward){
            model.move(model.getLocalRotation().multLocal(new Vector3f(0,0,1)).multLocal(tpf));
        }else if(backward){
            model.move(model.getLocalRotation().multLocal(new Vector3f(0,0,1)).multLocal(-tpf));
        }else if(leftRotate){
            model.rotate(0, tpf, 0);
        }else if(rightRotate){
            model.rotate(0, -tpf, 0);
        }
        fpsText.setText(cam.getLocation() + "/" + cam.getRotation());
    }

}
