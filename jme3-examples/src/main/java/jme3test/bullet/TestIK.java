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

import com.jme3.animation.AnimEventListener;
import com.jme3.animation.Bone;
import com.jme3.bullet.collision.RagdollCollisionListener;
import com.jme3.bullet.control.KinematicRagdollControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * @author reden
 */
public class TestIK extends TestBoneRagdoll implements RagdollCollisionListener, AnimEventListener {

    Node targetNode = new Node("");
    Vector3f targetPoint;
    Bone mouseBone;
    Vector3f oldMousePos;
 
    public static void main(String[] args) {
        TestIK app = new TestIK();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        super.simpleInitApp();
        final KinematicRagdollControl ikControl = model.getControl(KinematicRagdollControl.class);
        inputManager.addListener(new ActionListener() {

            public void onAction(String name, boolean isPressed, float tpf) {

                if (name.equals("stop") && isPressed) {
                    ikControl.setEnabled(!ikControl.isEnabled());
                    ikControl.setIKMode();
                }

                if (name.equals("one") && isPressed) {
                    //ragdoll.setKinematicMode();
                    targetPoint = model.getWorldTranslation().add(new Vector3f(0,2,4));
                    targetNode.setLocalTranslation(targetPoint);
                    ikControl.setIKTarget(ikControl.getBone("Hand.L"), targetPoint, 2);
                    ikControl.setIKMode();
                }
                if (name.equals("two") && isPressed) {
                    //ragdoll.setKinematicMode();
                    targetPoint = model.getWorldTranslation().add(new Vector3f(-3,3,0));
                    targetNode.setLocalTranslation(targetPoint);
                    ikControl.setIKTarget(ikControl.getBone("Hand.R"), targetPoint, 3);
                    ikControl.setIKMode();
                }
            }
        }, "one", "two");
        inputManager.addMapping("one", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("two", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("stop", new KeyTrigger(KeyInput.KEY_H));
    }
    
}
