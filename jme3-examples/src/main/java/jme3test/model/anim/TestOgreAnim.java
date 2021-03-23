/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.SkinningControl;
import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.tween.action.Action;
import com.jme3.anim.tween.action.BaseAction;
import com.jme3.anim.tween.action.LinearBlendSpace;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

public class TestOgreAnim extends SimpleApplication implements ActionListener {

    private AnimComposer animComposer;
    private static Action currentAction;

    public static void main(String[] args) {
        TestOgreAnim app = new TestOgreAnim();
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

        Spatial model = assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        model.center();

        animComposer = model.getControl(AnimComposer.class);
        animComposer.actionBlended("Attack", new LinearBlendSpace(0f, 0.5f), "Dodge");
        for (AnimClip animClip : animComposer.getAnimClips()) {
            Action action = animComposer.action(animClip.getName());
            if(!"stand".equals(animClip.getName())) {
                action = new BaseAction(Tweens.sequence(action, Tweens.callMethod(this, "backToStand", animComposer)));
            }
            animComposer.addAction(animClip.getName(), action);
        }
        currentAction = animComposer.setCurrentAction("stand"); // Walk, pull, Dodge, stand, push

        SkinningControl skinningControl = model.getControl(SkinningControl.class);
        skinningControl.setHardwareSkinningPreferred(false);

        Box b = new Box(.25f, 3f, .25f);
        Geometry item = new Geometry("Item", b);
        item.move(0, 1.5f, 0);
        item.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        Node n = skinningControl.getAttachmentsNode("hand.right");
        n.attachChild(item);

        rootNode.attachChild(model);

        inputManager.addListener(this, "Attack");
        inputManager.addMapping("Attack", new KeyTrigger(KeyInput.KEY_SPACE));
    }

    public Tween backToStand(AnimComposer animComposer) {
        currentAction =  animComposer.setCurrentAction("stand");
        return currentAction;
    }
    
    @Override
    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Attack") && value) {
            if (currentAction != null && !currentAction.equals(animComposer.getAction("Dodge"))) {
                currentAction = animComposer.setCurrentAction("Dodge");
                currentAction.setSpeed(0.1f);
            }
        }
    }
}
