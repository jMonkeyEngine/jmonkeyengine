/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import com.jme3.animation.*;
import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;

public class TestHWSkinning extends SimpleApplication implements ActionListener{

    private AnimChannel channel;
    private AnimControl control;
    private String[] animNames = {"Dodge", "Walk", "pull", "push"};
    private final static int SIZE = 10;
    private boolean hwSkinningEnable = true;
    private List<SkeletonControl> skControls = new ArrayList<SkeletonControl>();
    private BitmapText hwsText;

    public static void main(String[] args) {
        TestHWSkinning app = new TestHWSkinning();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(10f);
        cam.setLocation(new Vector3f(3.8664846f, 6.2704787f, 9.664585f));
        cam.setRotation(new Quaternion(-0.054774776f, 0.94064945f, -0.27974048f, -0.18418397f));
        makeHudText();
 
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -0.7f, -1).normalizeLocal());
        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1.0f));
        rootNode.addLight(dl);

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                Spatial model = (Spatial) assetManager.loadModel("Models/Oto/Oto.mesh.xml");
                model.setLocalScale(0.1f);
                model.setLocalTranslation(i - SIZE / 2, 0, j - SIZE / 2);
                control = model.getControl(AnimControl.class);

                channel = control.createChannel();
                channel.setAnim(animNames[(i + j) % 4]);
                SkeletonControl skeletonControl = model.getControl(SkeletonControl.class);
                skeletonControl.setHardwareSkinningPreferred(hwSkinningEnable);
                skControls.add(skeletonControl);
                rootNode.attachChild(model);
            }
        }

        inputManager.addListener(this, "toggleHWS");
        inputManager.addMapping("toggleHWS", new KeyTrigger(KeyInput.KEY_SPACE));
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if(isPressed && name.equals("toggleHWS")){
            hwSkinningEnable = !hwSkinningEnable;
            for (SkeletonControl control : skControls) {
                control.setHardwareSkinningPreferred(hwSkinningEnable);
                hwsText.setText("HWS : "+ hwSkinningEnable);
            }
        }
    }

    private void makeHudText() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        hwsText = new BitmapText(guiFont, false);
        hwsText.setSize(guiFont.getCharSet().getRenderedSize());
        hwsText.setText("HWS : "+ hwSkinningEnable);
        hwsText.setLocalTranslation(0, cam.getHeight(), 0);
        guiNode.attachChild(hwsText);
    }
}
