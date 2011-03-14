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
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.debug.SkeletonDebugger;

public class TestAnimBlendBug extends SimpleApplication implements ActionListener {

//    private AnimControl control;
    private AnimChannel channel1, channel2;
    private String[] animNames;

    private float blendTime = 0.5f;
    private float lockAfterBlending =  blendTime + 0.25f;
    private float blendingAnimationLock;

    public static void main(String[] args) {
        TestAnimBlendBug app = new TestAnimBlendBug();
        app.start();
    }

    public void onAction(String name, boolean value, float tpf) {
        if (name.equals("One") && value){
            channel1.setAnim(animNames[4], blendTime);
            channel2.setAnim(animNames[4], 0);
            channel1.setSpeed(0.25f);
            channel2.setSpeed(0.25f);
            blendingAnimationLock = lockAfterBlending;
        }
    }

    public void onPreUpdate(float tpf) {
    }

    public void onPostUpdate(float tpf) {
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Is there currently a blending underway?
        if (blendingAnimationLock > 0f) {
            blendingAnimationLock -= tpf;
        }
    }

    @Override
    public void simpleInitApp() {
        inputManager.addMapping("One", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addListener(this, "One");

        flyCam.setMoveSpeed(100f);
        cam.setLocation( new Vector3f( 0f, 150f, -325f ) );
        cam.lookAt( new Vector3f( 0f, 100f, 0f ), Vector3f.UNIT_Y );

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -0.7f, 1).normalizeLocal());
        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1.0f));
        rootNode.addLight(dl);

        Node model1 = (Node) assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
        Node model2 = (Node) assetManager.loadModel("Models/Ninja/Ninja.mesh.xml");
//        Node model2 = model1.clone();

        model1.setLocalTranslation(-60, 0, 0);
        model2.setLocalTranslation(60, 0, 0);

        AnimControl control1 = model1.getControl(AnimControl.class);
        animNames = control1.getAnimationNames().toArray(new String[0]);
        channel1 = control1.createChannel();
        
        AnimControl control2 = model2.getControl(AnimControl.class);
        channel2 = control2.createChannel();

        SkeletonDebugger skeletonDebug = new SkeletonDebugger("skeleton1", control1.getSkeleton());
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Green);
        mat.getAdditionalRenderState().setDepthTest(false);
        skeletonDebug.setMaterial(mat);
        model1.attachChild(skeletonDebug);

        skeletonDebug = new SkeletonDebugger("skeleton2", control2.getSkeleton());
        skeletonDebug.setMaterial(mat);
        model2.attachChild(skeletonDebug);

        rootNode.attachChild(model1);
        rootNode.attachChild(model2);
    }

}
