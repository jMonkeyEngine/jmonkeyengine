/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.tween.action.Action;
import com.jme3.anim.tween.action.BaseAction;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.DirectionalLightShadowFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Nehon
 */
public class TestSkeletonControlRefresh extends SimpleApplication implements ActionListener{

    private final static int SIZE = 10;
    private boolean hwSkinningEnable = true;
    final private List<SkinningControl> skinningControls = new ArrayList<>();
    private BitmapText hwsText;
 
    public static void main(String[] args) {
        TestSkeletonControlRefresh app = new TestSkeletonControlRefresh();
        app.start();
    }
 
    @Override
    public void simpleInitApp() {
        viewPort.setBackgroundColor(ColorRGBA.White);
        flyCam.setMoveSpeed(10f);
        cam.setLocation(new Vector3f(3.8664846f, 6.2704787f, 9.664585f));
        cam.setRotation(new Quaternion(-0.054774776f, 0.94064945f, -0.27974048f, -0.18418397f));
        makeHudText();
 
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -0.7f, -1).normalizeLocal());
        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1.0f));
        rootNode.addLight(dl);
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey k = new TextureKey("Models/Oto/Oto.jpg", false);
        m.setTexture("ColorMap", assetManager.loadTexture(k));        
 
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                Spatial model = assetManager.loadModel("Models/Oto/Oto.mesh.xml");
                //setting a different material
                model.setMaterial(m.clone());
                model.setLocalScale(0.1f);
                model.setLocalTranslation(i - SIZE / 2, 0, j - SIZE / 2);

                AnimComposer animComposer
                        = model.getControl(AnimComposer.class);
                for (AnimClip animClip : animComposer.getAnimClips()) {
                    Action action = animComposer.action(animClip.getName());
                    animComposer.addAction(animClip.getName(), new BaseAction(
                            Tweens.sequence(action, Tweens.callMethod(animComposer, "removeCurrentAction", AnimComposer.DEFAULT_LAYER))));
                }
                animComposer.setCurrentAction(new ArrayList<>(animComposer.getAnimClips()).get((i + j) % 4).getName());

                SkinningControl skinningControl = model.getControl(SkinningControl.class);
                skinningControl.setHardwareSkinningPreferred(hwSkinningEnable);
                skinningControls.add(skinningControl);

                rootNode.attachChild(model);
            }
        }        
        
        rootNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        setupFloor();
        
        inputManager.addListener(this, "toggleHWS");
        inputManager.addMapping("toggleHWS", new KeyTrigger(KeyInput.KEY_SPACE));
        
//        DirectionalLightShadowRenderer pssm = new DirectionalLightShadowRenderer(assetManager, 1024, 2);
//        pssm.setLight(dl);
//        viewPort.addProcessor(pssm);
        
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        
        DirectionalLightShadowFilter sf = new DirectionalLightShadowFilter(assetManager, 1024, 2);
        sf.setLight(dl);
        fpp.addFilter(sf);
        fpp.addFilter(new SSAOFilter());
        viewPort.addProcessor(fpp);
     
        
    }
    
     public void setupFloor() {
        Quad q = new Quad(20, 20);
       q.scaleTextureCoordinates(Vector2f.UNIT_XY.mult(10));
       Geometry geom = new Geometry("floor", q);
       Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
       mat.setColor("Color", ColorRGBA.White);       
       geom.setMaterial(mat);

       geom.rotate(-FastMath.HALF_PI, 0, 0);
       geom.center();
       geom.move(0, -0.3f, 0);
       geom.setShadowMode(RenderQueue.ShadowMode.Receive);
       rootNode.attachChild(geom);
    }

 
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if(isPressed && name.equals("toggleHWS")){
            hwSkinningEnable = !hwSkinningEnable;
            for (SkinningControl sc : skinningControls) {
                sc.setHardwareSkinningPreferred(hwSkinningEnable);
            }
            hwsText.setText("HWS : "+ hwSkinningEnable);
        }
    }
 
    private void makeHudText() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        hwsText = new BitmapText(guiFont);
        hwsText.setSize(guiFont.getCharSet().getRenderedSize());
        hwsText.setText("HWS : "+ hwSkinningEnable);
        hwsText.setLocalTranslation(0, cam.getHeight(), 0);
        guiNode.attachChild(hwsText);
    }
}
