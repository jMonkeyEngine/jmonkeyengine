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
package jme3test.post;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ContrastAdjustmentFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;

/**
 * Tests {@link ContrastAdjustmentFilter} on different color channels with an
 * adjustable exponents, scales and an input range during runtime.
 * @author pavl_g.
 */
public class TestContrastAdjustmentFilter extends SimpleApplication {

    /**
     * Display filter status.
     */
    private BitmapText statusText;
    /**
     * The filter being tested.
     */
    private ContrastAdjustmentFilter contrastAdjustmentFilter;

    public static void main(String[] args) {
        new TestContrastAdjustmentFilter().start();
    }

    @Override
    public void simpleInitApp() {
        //setup a spatial and a texture
        final Sphere globe = new Sphere(40, 40, 5f);
        final Geometry earth = new Geometry("Globe", globe);
        final Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        final Texture texture = assetManager.loadTexture("Textures/Sky/Earth/Earth.jpg");
        material.setTexture("ColorMap", texture);
        earth.setMaterial(material);

        rootNode.attachChild(earth);

        //add light
        final AmbientLight ambientLight = new AmbientLight(ColorRGBA.White);
        rootNode.addLight(ambientLight);

        //setup the filter
        final FilterPostProcessor postProcessor = new FilterPostProcessor(assetManager);
        contrastAdjustmentFilter = new ContrastAdjustmentFilter();
        //adjusting some parameters
        contrastAdjustmentFilter.setExponents(1.8f, 1.8f, 2.1f)
                                .setInputRange(0, 0.367f)
                                .setScales(0.25f, 0.25f, 1f);
        postProcessor.addFilter(contrastAdjustmentFilter);
        viewPort.addProcessor(postProcessor);

        setUpUserInterface();
    }

    /**
     * Update the status text.
     *
     * @param tpf unused
     */
    @Override
    public void simpleUpdate(float tpf) {
        String status = contrastAdjustmentFilter.toString();
        statusText.setText(status);
    }

    private void setUpUserInterface() {
        /*
         * Attach a BitmapText to display the status of the ContrastAdjustmentFilter.
         */
        statusText = new BitmapText(guiFont);
        guiNode.attachChild(statusText);
        statusText.setLocalTranslation(0f, cam.getHeight(), 0f);
        /*
         * Create a listener for user keypresses.
         */
        AnalogListener analog = new AnalogListener() {
            @Override
            public void onAnalog(String name, float value, float tpf) {
                float increment = name.endsWith("+") ? 0.01f : -0.01f;

                if (name.startsWith("lower")) {
                    float newValue = contrastAdjustmentFilter.getLowerLimit() + increment;
                    contrastAdjustmentFilter.setLowerLimit(newValue);

                } else if (name.startsWith("upper")) {
                    float newValue = contrastAdjustmentFilter.getUpperLimit() + increment;
                    contrastAdjustmentFilter.setUpperLimit(newValue);

                } else if (name.startsWith("re")) {
                    float newValue = contrastAdjustmentFilter.getRedExponent() + increment;
                    contrastAdjustmentFilter.setRedExponent(newValue);

                } else if (name.startsWith("ge")) {
                    float newValue = contrastAdjustmentFilter.getGreenExponent() + increment;
                    contrastAdjustmentFilter.setGreenExponent(newValue);

                } else if (name.startsWith("be")) {
                    float newValue = contrastAdjustmentFilter.getBlueExponent() + increment;
                    contrastAdjustmentFilter.setBlueExponent(newValue);

                } else if (name.startsWith("rs")) {
                    float newValue = contrastAdjustmentFilter.getRedScale() + increment;
                    contrastAdjustmentFilter.setRedScale(newValue);

                } else if (name.startsWith("gs")) {
                    float newValue = contrastAdjustmentFilter.getGreenScale() + increment;
                    contrastAdjustmentFilter.setGreenScale(newValue);

                } else if (name.startsWith("bs")) {
                    float newValue = contrastAdjustmentFilter.getBlueScale() + increment;
                    contrastAdjustmentFilter.setBlueScale(newValue);
                }
            }
        };

        System.out.println("lower limit:     press R to increase, F to decrease");
        inputManager.addMapping("lower+", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("lower-", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addListener(analog, "lower+", "lower-");

        System.out.println("upper limit:     press T to increase, G to decrease");
        inputManager.addMapping("upper+", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("upper-", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addListener(analog, "upper+", "upper-");

        System.out.println("red exponent:    press Y to increase, H to decrease");
        inputManager.addMapping("re+", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("re-", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addListener(analog, "re+", "re-");

        System.out.println("green exponent:  press U to increase, J to decrease");
        inputManager.addMapping("ge+", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("ge-", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addListener(analog, "ge+", "ge-");

        System.out.println("blue exponent:   press I to increase, K to decrease");
        inputManager.addMapping("be+", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addMapping("be-", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addListener(analog, "be+", "be-");

        System.out.println("red scale:       press O to increase, L to decrease");
        inputManager.addMapping("rs+", new KeyTrigger(KeyInput.KEY_O));
        inputManager.addMapping("rs-", new KeyTrigger(KeyInput.KEY_L));
        inputManager.addListener(analog, "rs+", "rs-");

        System.out.println("green scale:     press P to increase, ; to decrease");
        inputManager.addMapping("gs+", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addMapping("gs-", new KeyTrigger(KeyInput.KEY_SEMICOLON));
        inputManager.addListener(analog, "gs+", "gs-");

        System.out.println("blue scale:      press [ to increase, ' to decrease");
        inputManager.addMapping("bs+", new KeyTrigger(KeyInput.KEY_LBRACKET));
        inputManager.addMapping("bs-", new KeyTrigger(KeyInput.KEY_APOSTROPHE));
        inputManager.addListener(analog, "bs+", "bs-");

        System.out.println();
    }
}
