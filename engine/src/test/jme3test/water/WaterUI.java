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

package jme3test.water;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.water.SimpleWaterProcessor;

/**
 *
 * @author nehon
 */
public class WaterUI {
    private SimpleWaterProcessor processor;
    public WaterUI(InputManager inputManager, SimpleWaterProcessor proc) {
        processor=proc;


        System.out.println("----------------- SSAO UI Debugger --------------------");
        System.out.println("-- Water transparency : press Y to increase, H to decrease");
        System.out.println("-- Water depth : press U to increase, J to decrease");
//        System.out.println("-- AO scale : press I to increase, K to decrease");
//        System.out.println("-- AO bias : press O to increase, P to decrease");
//        System.out.println("-- Toggle AO on/off : press space bar");
//        System.out.println("-- Use only AO : press Num pad 0");
//        System.out.println("-- Output config declaration : press P");
        System.out.println("-------------------------------------------------------");
    
        inputManager.addMapping("transparencyUp", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("transparencyDown", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("depthUp", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("depthDown", new KeyTrigger(KeyInput.KEY_J));
//        inputManager.addMapping("scaleUp", new KeyTrigger(KeyInput.KEY_I));
//        inputManager.addMapping("scaleDown", new KeyTrigger(KeyInput.KEY_K));
//        inputManager.addMapping("biasUp", new KeyTrigger(KeyInput.KEY_O));
//        inputManager.addMapping("biasDown", new KeyTrigger(KeyInput.KEY_L));
//        inputManager.addMapping("outputConfig", new KeyTrigger(KeyInput.KEY_P));
//        inputManager.addMapping("toggleUseAO", new KeyTrigger(KeyInput.KEY_SPACE));
//        inputManager.addMapping("toggleUseOnlyAo", new KeyTrigger(KeyInput.KEY_NUMPAD0));
        
//        ActionListener acl = new ActionListener() {
//
//            public void onAction(String name, boolean keyPressed, float tpf) {
//
//                if (name.equals("toggleUseAO") && keyPressed) {
//                    ssaoConfig.setUseAo(!ssaoConfig.isUseAo());
//                    System.out.println("use AO : "+ssaoConfig.isUseAo());
//                }
//                if (name.equals("toggleUseOnlyAo") && keyPressed) {
//                    ssaoConfig.setUseOnlyAo(!ssaoConfig.isUseOnlyAo());
//                    System.out.println("use Only AO : "+ssaoConfig.isUseOnlyAo());
//
//                }
//                if (name.equals("outputConfig") && keyPressed) {
//                    System.out.println("new SSAOConfig("+ssaoConfig.getSampleRadius()+"f,"+ssaoConfig.getIntensity()+"f,"+ssaoConfig.getScale()+"f,"+ssaoConfig.getBias()+"f,"+ssaoConfig.isUseOnlyAo()+","+ssaoConfig.isUseAo()+");");
//                }
//
//            }
//        };

         AnalogListener anl = new AnalogListener() {

            public void onAnalog(String name, float value, float tpf) {
                if (name.equals("transparencyUp")) {
                    processor.setWaterTransparency(processor.getWaterTransparency()+0.001f);
                    System.out.println("Water transparency : "+processor.getWaterTransparency());
                }
                if (name.equals("transparencyDown")) {
                    processor.setWaterTransparency(processor.getWaterTransparency()-0.001f);
                    System.out.println("Water transparency : "+processor.getWaterTransparency());
                }
                if (name.equals("depthUp")) {
                    processor.setWaterDepth(processor.getWaterDepth()+0.001f);
                    System.out.println("Water depth : "+processor.getWaterDepth());
                }
                if (name.equals("depthDown")) {
                    processor.setWaterDepth(processor.getWaterDepth()-0.001f);
                    System.out.println("Water depth : "+processor.getWaterDepth());
                }

            }
        };
    //    inputManager.addListener(acl,"toggleUseAO","toggleUseOnlyAo","outputConfig");
        inputManager.addListener(anl, "transparencyUp","transparencyDown","depthUp","depthDown");
     
    }
    
    

}
