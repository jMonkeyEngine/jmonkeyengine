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

package jme3test.post;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

public class TestToneMapFilter extends SimpleApplication {

    private boolean enabled = true;
    private FilterPostProcessor fpp;
    private ToneMapFilter toneMapFilter;
    private float whitePointLog = 1f;

    public static void main(String[] args){
        TestToneMapFilter app = new TestToneMapFilter();
        AppSettings settings = new AppSettings(true);
        
        // Must turn on gamma correction, as otherwise it looks too dark.
        settings.setGammaCorrection(true);
        
        app.setSettings(settings);
        app.start();
    }

    public Geometry createHDRBox(){
        Box boxMesh = new Box(1, 1, 1);
        Geometry box = new Geometry("Box", boxMesh);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("Textures/HdrTest/Memorial.hdr"));
        box.setMaterial(mat);
        return box;
    }

    @Override
    public void simpleInitApp() {
        System.out.println("== Tone Mapping Sample ==");
        System.out.println(" SPACE:\tToggle tone-mapping OFF or ON");
        System.out.println(" Y:\tIncrease white-point");
        System.out.println(" H:\tDecrease white-point");
        
        fpp = new FilterPostProcessor(assetManager);
        toneMapFilter = new ToneMapFilter();
        fpp.addFilter(toneMapFilter);
        viewPort.addProcessor(fpp);
        
        rootNode.attachChild(createHDRBox());
        
        cam.setLocation(new Vector3f(0f,0f,3f));
        
        initInputs();
    }
    
    private void initInputs() {
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("WhitePointUp", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("WhitePointDown", new KeyTrigger(KeyInput.KEY_H));

        ActionListener acl = new ActionListener() {

            public void onAction(String name, boolean keyPressed, float tpf) {
                if (name.equals("toggle") && keyPressed) {
                    if (enabled) {
                        enabled = false;
                        viewPort.removeProcessor(fpp);
                        System.out.println("Tone Mapping OFF");
                    } else {
                        enabled = true;
                        viewPort.addProcessor(fpp);
                        System.out.println("Tone Mapping ON");
                    }
                }
            }
        };

        AnalogListener anl = new AnalogListener() {

            public void onAnalog(String name, float isPressed, float tpf) {
                if (name.equals("WhitePointUp")) {
                    whitePointLog += tpf * 1.0;
                    if (whitePointLog > 4f) {
                        whitePointLog = 4f;
                    }
                    float wp = FastMath.exp(whitePointLog);
                    toneMapFilter.setWhitePoint(new Vector3f(wp, wp, wp));
                    System.out.println("White point: " + wp);
                }

                if (name.equals("WhitePointDown")) {
                    whitePointLog -= tpf * 1.0;
                    if (whitePointLog < -4f) {
                        whitePointLog = -4f;
                    }
                    float wp = FastMath.exp(whitePointLog);
                    toneMapFilter.setWhitePoint(new Vector3f(wp, wp, wp));
                    System.out.println("White point: " + wp);
                }
            }
        };

        inputManager.addListener(acl, "toggle");
        inputManager.addListener(anl, "WhitePointUp", "WhitePointDown");
    }
}
