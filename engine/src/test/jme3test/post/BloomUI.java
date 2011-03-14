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
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.post.filters.BloomFilter;

/**
 *
 * @author nehon
 */
public class BloomUI {

    public BloomUI(InputManager inputManager, final BloomFilter filter) {

        System.out.println("----------------- Bloom UI Debugger --------------------");
        System.out.println("-- blur Scale : press Y to increase, H to decrease");
        System.out.println("-- exposure Power : press U to increase, J to decrease");
        System.out.println("-- exposure CutOff : press I to increase, K to decrease");
        System.out.println("-- bloom Intensity : press O to increase, P to decrease");
        System.out.println("-------------------------------------------------------");

        inputManager.addMapping("blurScaleUp", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("blurScaleDown", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("exposurePowerUp", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("exposurePowerDown", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("exposureCutOffUp", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping("exposureCutOffDown", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("bloomIntensityUp", new KeyTrigger(KeyInput.KEY_O));
        inputManager.addMapping("bloomIntensityDown", new KeyTrigger(KeyInput.KEY_L));


        AnalogListener anl = new AnalogListener() {

            public void onAnalog(String name, float value, float tpf) {
                if (name.equals("blurScaleUp")) {
                    filter.setBlurScale(filter.getBlurScale() + 0.01f);
                    System.out.println("blurScale : " + filter.getBlurScale());
                }
                if (name.equals("blurScaleDown")) {
                    filter.setBlurScale(filter.getBlurScale() - 0.01f);
                    System.out.println("blurScale : " + filter.getBlurScale());
                }
                if (name.equals("exposurePowerUp")) {
                    filter.setExposurePower(filter.getExposurePower() + 0.01f);
                    System.out.println("exposurePower : " + filter.getExposurePower());
                }
                if (name.equals("exposurePowerDown")) {
                    filter.setExposurePower(filter.getExposurePower() - 0.01f);
                    System.out.println("exposurePower : " + filter.getExposurePower());
                }
                if (name.equals("exposureCutOffUp")) {
                    filter.setExposureCutOff(Math.min(1.0f, Math.max(0.0f, filter.getExposureCutOff() + 0.001f)));
                    System.out.println("exposure CutOff : " + filter.getExposureCutOff());
                }
                if (name.equals("exposureCutOffDown")) {
                    filter.setExposureCutOff(Math.min(1.0f, Math.max(0.0f, filter.getExposureCutOff() - 0.001f)));
                    System.out.println("exposure CutOff : " + filter.getExposureCutOff());
                }
                if (name.equals("bloomIntensityUp")) {
                    filter.setBloomIntensity(filter.getBloomIntensity() + 0.01f);
                    System.out.println("bloom Intensity : " + filter.getBloomIntensity());
                }
                if (name.equals("bloomIntensityDown")) {
                    filter.setBloomIntensity(filter.getBloomIntensity() - 0.01f);
                    System.out.println("bloom Intensity : " + filter.getBloomIntensity());
                }


            }
        };

        inputManager.addListener(anl, "blurScaleUp", "blurScaleDown", "exposurePowerUp", "exposurePowerDown",
                "exposureCutOffUp", "exposureCutOffDown", "bloomIntensityUp", "bloomIntensityDown");

    }
}
