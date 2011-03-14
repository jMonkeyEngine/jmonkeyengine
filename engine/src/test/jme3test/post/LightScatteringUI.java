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

package jme3test.post;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.post.filters.LightScatteringFilter;

/**
 *
 * @author nehon
 */
public class LightScatteringUI {
    private LightScatteringFilter filter;
    public LightScatteringUI(InputManager inputManager, LightScatteringFilter proc) {
        filter=proc;


        System.out.println("----------------- LightScattering UI Debugger --------------------");
        System.out.println("-- Sample number : press Y to increase, H to decrease");
        System.out.println("-- blur start : press U to increase, J to decrease");
        System.out.println("-- blur width : press I to increase, K to decrease");
        System.out.println("-- Light density : press O to increase, P to decrease");
//        System.out.println("-- Toggle AO on/off : press space bar");
//        System.out.println("-- Use only AO : press Num pad 0");
//        System.out.println("-- Output config declaration : press P");
        System.out.println("-------------------------------------------------------");
    
        inputManager.addMapping("sampleUp", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("sampleDown", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("blurStartUp", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("blurStartDown", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("blurWidthUp", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping("blurWidthDown", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("lightDensityUp", new KeyTrigger(KeyInput.KEY_O));
        inputManager.addMapping("lightDensityDown", new KeyTrigger(KeyInput.KEY_L));
        inputManager.addMapping("outputConfig", new KeyTrigger(KeyInput.KEY_P));
//        inputManager.addMapping("toggleUseAO", new KeyTrigger(KeyInput.KEY_SPACE));
//        inputManager.addMapping("toggleUseOnlyAo", new KeyTrigger(KeyInput.KEY_NUMPAD0));
        
        ActionListener acl = new ActionListener() {

            public void onAction(String name, boolean keyPressed, float tpf) {

                if (name.equals("sampleUp")) {
                    filter.setNbSamples(filter.getNbSamples()+1);
                    System.out.println("Nb Samples : "+filter.getNbSamples());
                }
                if (name.equals("sampleDown")) {
                   filter.setNbSamples(filter.getNbSamples()-1);
                   System.out.println("Nb Samples : "+filter.getNbSamples());
                }
                if (name.equals("outputConfig") && keyPressed) {
                   System.out.println("lightScatteringFilter.setNbSamples("+filter.getNbSamples()+");");
                   System.out.println("lightScatteringFilter.setBlurStart("+filter.getBlurStart()+"f);");
                   System.out.println("lightScatteringFilter.setBlurWidth("+filter.getBlurWidth()+"f);");
                   System.out.println("lightScatteringFilter.setLightDensity("+filter.getLightDensity()+"f);");
                }
               

            }
        };

         AnalogListener anl = new AnalogListener() {

            public void onAnalog(String name, float value, float tpf) {
               
                if (name.equals("blurStartUp")) {
                    filter.setBlurStart(filter.getBlurStart()+0.001f);
                    System.out.println("Blur start : "+filter.getBlurStart());
                }
                if (name.equals("blurStartDown")) {
                    filter.setBlurStart(filter.getBlurStart()-0.001f);
                    System.out.println("Blur start : "+filter.getBlurStart());
                }
                 if (name.equals("blurWidthUp")) {
                    filter.setBlurWidth(filter.getBlurWidth()+0.001f);
                    System.out.println("Blur Width : "+filter.getBlurWidth());
                }
                if (name.equals("blurWidthDown")) {
                    filter.setBlurWidth(filter.getBlurWidth()-0.001f);
                    System.out.println("Blur Width : "+filter.getBlurWidth());
                }
                if (name.equals("lightDensityUp")) {
                    filter.setLightDensity(filter.getLightDensity()+0.001f);
                    System.out.println("light Density : "+filter.getLightDensity());
                }
                if (name.equals("lightDensityDown")) {
                     filter.setLightDensity(filter.getLightDensity()-0.001f);
                    System.out.println("light Density : "+filter.getLightDensity());
                }

            }
        };
        inputManager.addListener(acl,"sampleUp","sampleDown","outputConfig");

        inputManager.addListener(anl, "blurStartUp","blurStartDown","blurWidthUp", "blurWidthDown","lightDensityUp", "lightDensityDown");
     
    }
    
    

}
