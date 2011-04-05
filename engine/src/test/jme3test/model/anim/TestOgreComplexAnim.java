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

package jme3test.model.anim;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.debug.SkeletonDebugger;

public class TestOgreComplexAnim extends SimpleApplication {

    private AnimControl control;
    private float angle = 0;
    private float scale = 1;
    private float rate = 1;

    public static void main(String[] args) {
        TestOgreComplexAnim app = new TestOgreComplexAnim();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(10f);
        cam.setLocation(new Vector3f(6.4013605f, 7.488437f, 12.843031f));
        cam.setRotation(new Quaternion(-0.060740203f, 0.93925786f, -0.2398315f, -0.2378785f));

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -0.7f, -1).normalizeLocal());
        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1.0f));
        rootNode.addLight(dl);

        Node model = (Node) assetManager.loadModel("Models/Oto/Oto.mesh.xml");

        control = model.getControl(AnimControl.class);

        AnimChannel feet = control.createChannel();
        AnimChannel leftHand = control.createChannel();
        AnimChannel rightHand = control.createChannel();

        // feet will dodge
        feet.addFromRootBone("hip.right");
        feet.addFromRootBone("hip.left");
        feet.setAnim("Dodge");
        feet.setSpeed(2);
        feet.setLoopMode(LoopMode.Cycle);

        // will blend over 15 seconds to stand
        feet.setAnim("Walk", 15);
        feet.setSpeed(0.25f);
        feet.setLoopMode(LoopMode.Cycle);

        // left hand will pull
        leftHand.addFromRootBone("uparm.right");
        leftHand.setAnim("pull");
        leftHand.setSpeed(.5f);

        // will blend over 15 seconds to stand
        leftHand.setAnim("stand", 15);

        // right hand will push
        rightHand.addBone("spinehigh");
        rightHand.addFromRootBone("uparm.left");
        rightHand.setAnim("push");

        SkeletonDebugger skeletonDebug = new SkeletonDebugger("skeleton", control.getSkeleton());
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Green);
        mat.getAdditionalRenderState().setDepthTest(false);
        skeletonDebug.setMaterial(mat);

        model.attachChild(skeletonDebug);
        rootNode.attachChild(model);
    }

    @Override
    public void simpleUpdate(float tpf){
        Bone b = control.getSkeleton().getBone("spinehigh");
        Bone b2 = control.getSkeleton().getBone("uparm.left");
        
        angle += tpf * rate;        
        if (angle > FastMath.HALF_PI / 2f){
            angle = FastMath.HALF_PI / 2f;
            rate = -1;
        }else if (angle < -FastMath.HALF_PI / 2f){
            angle = -FastMath.HALF_PI / 2f;
            rate = 1;
        }

        Quaternion q = new Quaternion();
        q.fromAngles(0, angle, 0);

        b.setUserControl(true);
        b.setUserTransforms(Vector3f.ZERO, q, Vector3f.UNIT_XYZ);
        
        b2.setUserControl(true);
        b2.setUserTransforms(Vector3f.ZERO, Quaternion.IDENTITY, new Vector3f(1+angle,1+ angle, 1+angle));
  
  
    }

}
