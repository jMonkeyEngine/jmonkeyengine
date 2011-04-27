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

import com.jme3.bullet.BulletAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;

/**
 *
 * @author normenhansen
 */
public class TestPhysicsCharacter extends SimpleApplication implements ActionListener {

    private BulletAppState bulletAppState;
    private CharacterControl physicsCharacter;
    private Vector3f walkDirection = new Vector3f();
    private Material mat;
    private Sphere bullet;
    private SphereCollisionShape bulletCollisionShape;

    public static void main(String[] args) {
        TestPhysicsCharacter app = new TestPhysicsCharacter();
        app.start();
    }

    private void setupKeys() {
        inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "shoot");
        inputManager.addListener(this, "Lefts");
        inputManager.addListener(this, "Rights");
        inputManager.addListener(this, "Ups");
        inputManager.addListener(this, "Downs");
        inputManager.addListener(this, "Space");
        inputManager.addMapping("gc", new KeyTrigger(KeyInput.KEY_X));
        inputManager.addListener(this, "gc");
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bullet = new Sphere(32, 32, 0.4f, true, false);
        bullet.setTextureMode(TextureMode.Projected);
        bulletCollisionShape = new SphereCollisionShape(0.4f);
        PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());

        setupKeys();

        // Add a physics character to the world
        physicsCharacter = new CharacterControl(new CapsuleCollisionShape(0.5f,1.8f), .1f);
        physicsCharacter.setPhysicsLocation(new Vector3f(3, 6, 0));

        Spatial model = assetManager.loadModel("Models/Sinbad/Sinbad.mesh.xml");
        model.scale(0.25f);
        model.addControl(physicsCharacter);
        getPhysicsSpace().add(physicsCharacter);
        rootNode.attachChild(model);
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    @Override
    public void simpleUpdate(float tpf) {
        physicsCharacter.setWalkDirection(walkDirection);
        physicsCharacter.setViewDirection(walkDirection);
        cam.lookAt(physicsCharacter.getPhysicsLocation(), Vector3f.UNIT_Y);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Lefts")) {
            if (value) {
                walkDirection.addLocal(new Vector3f(-.1f, 0, 0));
            } else {
                walkDirection.addLocal(new Vector3f(.1f, 0, 0));
            }
        } else if (binding.equals("Rights")) {
            if (value) {
                walkDirection.addLocal(new Vector3f(.1f, 0, 0));
            } else {
                walkDirection.addLocal(new Vector3f(-.1f, 0, 0));
            }
        } else if (binding.equals("Ups")) {
            if (value) {
                walkDirection.addLocal(new Vector3f(0, 0, -.1f));
            } else {
                walkDirection.addLocal(new Vector3f(0, 0, .1f));
            }
        } else if (binding.equals("Downs")) {
            if (value) {
                walkDirection.addLocal(new Vector3f(0, 0, .1f));
            } else {
                walkDirection.addLocal(new Vector3f(0, 0, -.1f));
            }
        } else if (binding.equals("Space")) {
            physicsCharacter.jump();
        }
        if (binding.equals("gc") && !value) {
            System.gc();
        }
    }
}
