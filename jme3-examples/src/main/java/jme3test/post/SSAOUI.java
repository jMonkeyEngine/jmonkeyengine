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

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.post.ssao.SSAOFilter;

/**
 *
 * @author nehon
 */
public class SSAOUI {

    SSAOFilter filter;

    public SSAOUI(InputManager inputManager, SSAOFilter filter) {
        this.filter = filter;
        init(inputManager);
    }

    private void init(InputManager inputManager) {
        System.out.println("----------------- SSAO UI Debugger --------------------");
        System.out.println("-- Sample Radius : press Y to increase, H to decrease");
        System.out.println("-- AO Intensity : press U to increase, J to decrease");
        System.out.println("-- AO scale : press I to increase, K to decrease");
        System.out.println("-- AO bias : press O to increase, P to decrease");
        System.out.println("-- Toggle AO on/off : press space bar");
        System.out.println("-- Use only AO : press Num pad 0");
        System.out.println("-- Output config declaration : press P");
        System.out.println("-------------------------------------------------------");

        inputManager.addMapping("sampleRadiusUp", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("sampleRadiusDown", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("intensityUp", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("intensityDown", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("scaleUp", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping("scaleDown", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("biasUp", new KeyTrigger(KeyInput.KEY_O));
        inputManager.addMapping("biasDown", new KeyTrigger(KeyInput.KEY_L));
        inputManager.addMapping("outputConfig", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addMapping("toggleUseAO", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("toggleUseOnlyAo", new KeyTrigger(KeyInput.KEY_NUMPAD0));
        inputManager.addMapping("toggleApprox", new KeyTrigger(KeyInput.KEY_NUMPAD5));

        ActionListener acl = new ActionListener() {

            public void onAction(String name, boolean keyPressed, float tpf) {

                if (name.equals("toggleUseAO") && keyPressed) {
                    filter.setEnabled(!filter.isEnabled());
                    // filter.setUseAo(!filter.isUseAo());
                    System.out.println("use AO : " + filter.isEnabled());
                }
                if (name.equals("toggleApprox") && keyPressed) {
                    filter.setApproximateNormals(!filter.isApproximateNormals());
                    System.out.println("Approximate Normals : " + filter.isApproximateNormals());

                }
                if (name.equals("toggleUseOnlyAo") && keyPressed) {
                    filter.setUseOnlyAo(!filter.isUseOnlyAo());
                    System.out.println("use Only AO : " + filter.isUseOnlyAo());

                }
                if (name.equals("outputConfig") && keyPressed) {
                    System.out.println("new SSAOFilter(" + filter.getSampleRadius() + "f," + filter.getIntensity() + "f," + filter.getScale() + "f," + filter.getBias() + "f);");
                }
            }
        };

        AnalogListener anl = new AnalogListener() {

            public void onAnalog(String name, float value, float tpf) {
                if (name.equals("sampleRadiusUp")) {
                    filter.setSampleRadius(filter.getSampleRadius() + 0.01f);
                    System.out.println("Sample Radius : " + filter.getSampleRadius());
                }
                if (name.equals("sampleRadiusDown")) {
                    filter.setSampleRadius(filter.getSampleRadius() - 0.01f);
                    System.out.println("Sample Radius : " + filter.getSampleRadius());
                }
                if (name.equals("intensityUp")) {
                    filter.setIntensity(filter.getIntensity() + 0.01f);
                    System.out.println("Intensity : " + filter.getIntensity());
                }
                if (name.equals("intensityDown")) {
                    filter.setIntensity(filter.getIntensity() - 0.01f);
                    System.out.println("Intensity : " + filter.getIntensity());
                }
                if (name.equals("scaleUp")) {
                    filter.setScale(filter.getScale() + 0.01f);
                    System.out.println("scale : " + filter.getScale());
                }
                if (name.equals("scaleDown")) {
                    filter.setScale(filter.getScale() - 0.01f);
                    System.out.println("scale : " + filter.getScale());
                }
                if (name.equals("biasUp")) {
                    filter.setBias(filter.getBias() + 0.001f);
                    System.out.println("bias : " + filter.getBias());
                }
                if (name.equals("biasDown")) {
                    filter.setBias(filter.getBias() - 0.001f);
                    System.out.println("bias : " + filter.getBias());
                }

            }
        };
        inputManager.addListener(acl, "toggleUseAO", "toggleApprox", "toggleUseOnlyAo", "outputConfig");
        inputManager.addListener(anl, "sampleRadiusUp", "sampleRadiusDown", "intensityUp", "intensityDown", "scaleUp", "scaleDown",
                "biasUp", "biasDown");

    }
}
