/*
 * Copyright (c) 2018 jMonkeyEngine
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
import com.jme3.bullet.control.KinematicRagdollControl;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Test case for JME issue #931: RagdollUtils can miss model meshes or use the
 * non-animated ones.
 * <p>
 * If successful, no AssertionError will be thrown.
 */
public class TestIssue931 extends SimpleApplication {
    // *************************************************************************
    // new methods exposed

    public static void main(String[] args) {
        TestIssue931 app = new TestIssue931();
        app.start();
    }
    // *************************************************************************
    // SimpleApplication methods

    @Override
    public void simpleInitApp() {
        BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        String sinbadPath = "Models/Sinbad/SinbadOldAnim.j3o";
        Node sinbad = (Node) assetManager.loadModel(sinbadPath);

        Node extender = new Node();
        for (Spatial child : sinbad.getChildren()) {
            extender.attachChild(child);
        }
        sinbad.attachChild(extender);

        //Note: PhysicsRagdollControl is still a WIP, constructor will change
        KinematicRagdollControl ragdoll = new KinematicRagdollControl(0.5f);
        sinbad.addControl(ragdoll);

        stop();
    }
}
