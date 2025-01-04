/*
 * Copyright (c) 2024 jMonkeyEngine
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

import com.jme3.anim.AnimComposer;
import com.jme3.anim.ArmatureMask;
import com.jme3.anim.SingleLayerInfluenceMask;
import com.jme3.anim.SkinningControl;
import com.jme3.anim.tween.action.ClipAction;
import com.jme3.anim.util.AnimMigrationUtils;
import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * Tests {@link SingleLayerInfluenceMask}.
 * 
 * The test runs two simultaneous looping actions on seperate layers.
 * <p>
 * The test <strong>fails</strong> if the visible dancing action does <em>not</em>
 * loop seamlessly when using SingleLayerInfluenceMasks. Note that the action is not
 * expected to loop seamlessly when <em>not</em> using SingleLayerArmatureMasks.
 * <p>
 * Press the spacebar to switch between using SingleLayerInfluenceMasks and masks
 * provided by {@link ArmatureMask}.
 * 
 * @author codex
 */
public class TestSingleLayerInfluenceMask extends SimpleApplication implements ActionListener {
    
    private Spatial model;
    private AnimComposer anim;
    private SkinningControl skin;
    private boolean useSLIMask = true;
    private BitmapText display;

    public static void main(String[] args) {
        TestSingleLayerInfluenceMask app = new TestSingleLayerInfluenceMask();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        
        flyCam.setMoveSpeed(30f);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -0.7f, -1).normalizeLocal());
        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1.0f));
        rootNode.addLight(dl);
        
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        display = new BitmapText(font);
        display.setSize(font.getCharSet().getRenderedSize());
        display.setText("");
        display.setLocalTranslation(5, context.getSettings().getHeight()-5, 0);
        guiNode.attachChild(display);
 
        inputManager.addMapping("reset", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "reset");
        
        setupModel();
        
    }
    @Override
    public void simpleUpdate(float tpf) {
        cam.lookAt(model.getWorldTranslation(), Vector3f.UNIT_Y);
    }
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("reset") && isPressed) {
            model.removeFromParent();
            setupModel();
        }
    }
    
    private void setupModel() {
        
        model = assetManager.loadModel("Models/Sinbad/SinbadOldAnim.j3o");
        AnimMigrationUtils.migrate(model);
        anim = model.getControl(AnimComposer.class);
        skin = model.getControl(SkinningControl.class);
        
        if (useSLIMask) {
            SingleLayerInfluenceMask walkLayer = new SingleLayerInfluenceMask("idleLayer", anim, skin);
            walkLayer.addAll();
            walkLayer.makeLayer();
            SingleLayerInfluenceMask danceLayer = new SingleLayerInfluenceMask("danceLayer", anim, skin);
            danceLayer.addAll();
            danceLayer.makeLayer();
        } else {
            anim.makeLayer("idleLayer", ArmatureMask.createMask(skin.getArmature(), "Root"));
            anim.makeLayer("danceLayer", ArmatureMask.createMask(skin.getArmature(), "Root"));
        }
        
        setSLIMaskInfo();
        useSLIMask = !useSLIMask;
        
        ClipAction clip = (ClipAction)anim.action("Dance");
        clip.setMaxTransitionWeight(.9f);
        ClipAction clip2 = (ClipAction)anim.action("IdleTop");
        clip2.setMaxTransitionWeight(.8f);
        
        anim.setCurrentAction("Dance", "danceLayer");
        anim.setCurrentAction("IdleTop", "idleLayer");

        rootNode.attachChild(model);
        
    }
    private void setSLIMaskInfo() {
        display.setText("Using SingleLayerInfluenceMasks: "+useSLIMask+"\nPress Spacebar to switch masks");
    }
    
}
