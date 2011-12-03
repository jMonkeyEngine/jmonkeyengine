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

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;

/**
 *
 * @author normenhansen
 */
public class TestCcd extends SimpleApplication implements ActionListener {

    private Material mat;
    private Material mat2;
    private Sphere bullet;
    private SphereCollisionShape bulletCollisionShape;
    private BulletAppState bulletAppState;

    public static void main(String[] args) {
        TestCcd app = new TestCcd();
        app.start();
    }

    private void setupKeys() {
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("shoot2", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(this, "shoot");
        inputManager.addListener(this, "shoot2");
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().enableDebug(assetManager);
        bullet = new Sphere(32, 32, 0.4f, true, false);
        bullet.setTextureMode(TextureMode.Projected);
        bulletCollisionShape = new SphereCollisionShape(0.1f);
        setupKeys();

        mat = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Green);

        mat2 = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat2.getAdditionalRenderState().setWireframe(true);
        mat2.setColor("Color", ColorRGBA.Red);

        // An obstacle mesh, does not move (mass=0)
        Node node2 = new Node();
        node2.setName("mesh");
        node2.setLocalTranslation(new Vector3f(2.5f, 0, 0f));
        node2.addControl(new RigidBodyControl(new MeshCollisionShape(new Box(Vector3f.ZERO, 4, 4, 0.1f)), 0));
        rootNode.attachChild(node2);
        getPhysicsSpace().add(node2);

        // The floor, does not move (mass=0)
        Node node3 = new Node();
        node3.setLocalTranslation(new Vector3f(0f, -6, 0f));
        node3.addControl(new RigidBodyControl(new BoxCollisionShape(new Vector3f(100, 1, 100)), 0));
        rootNode.attachChild(node3);
        getPhysicsSpace().add(node3);

    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("shoot") && !value) {
            Geometry bulletg = new Geometry("bullet", bullet);
            bulletg.setMaterial(mat);
            bulletg.setName("bullet");
            bulletg.setLocalTranslation(cam.getLocation());
            bulletg.setShadowMode(ShadowMode.CastAndReceive);
            bulletg.addControl(new RigidBodyControl(bulletCollisionShape, 1));
            bulletg.getControl(RigidBodyControl.class).setCcdMotionThreshold(0.1f);
            bulletg.getControl(RigidBodyControl.class).setLinearVelocity(cam.getDirection().mult(40));
            rootNode.attachChild(bulletg);
            getPhysicsSpace().add(bulletg);
        } else if (binding.equals("shoot2") && !value) {
            Geometry bulletg = new Geometry("bullet", bullet);
            bulletg.setMaterial(mat2);
            bulletg.setName("bullet");
            bulletg.setLocalTranslation(cam.getLocation());
            bulletg.setShadowMode(ShadowMode.CastAndReceive);
            bulletg.addControl(new RigidBodyControl(bulletCollisionShape, 1));
            bulletg.getControl(RigidBodyControl.class).setLinearVelocity(cam.getDirection().mult(40));
            rootNode.attachChild(bulletg);
            getPhysicsSpace().add(bulletg);
        }
    }
}
