/*
 * Copyright (c) 2019 jMonkeyEngine
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
package jme3test.animation;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * Test case for JME issue #1138: Elephant's legUp animation sets Joint
 * translation to NaN.
 * <p>
 * If successful, the animation cycle will complete without throwing an
 * IllegalStateException.
 *
 * @author Stephen Gold
 */
public class TestIssue1138 extends SimpleApplication {

    SkinningControl sControl;

    public static void main(String... argv) {
        new TestIssue1138().start();
    }

    @Override
    public void simpleInitApp() {
        Node cgModel = (Node) assetManager.loadModel(
                "Models/Elephant/Elephant.mesh.xml");
        rootNode.attachChild(cgModel);
        cgModel.rotate(0f, -1f, 0f);
        cgModel.scale(0.04f);

        AnimComposer composer = cgModel.getControl(AnimComposer.class);
        composer.setCurrentAction("legUp");
        sControl = cgModel.getControl(SkinningControl.class);

        AmbientLight light = new AmbientLight();
        rootNode.addLight(light);
    }

    @Override
    public void simpleUpdate(float tpf) {
        for (Joint joint : sControl.getArmature().getJointList()) {
            Vector3f translation = joint.getLocalTranslation();
            if (!Vector3f.isValidVector(translation)) {
                String msg = "Invalid translation for joint " + joint.getName();
                throw new IllegalStateException(msg);
            }
        }
    }
}
