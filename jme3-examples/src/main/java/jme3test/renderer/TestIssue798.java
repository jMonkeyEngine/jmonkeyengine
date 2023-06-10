/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package jme3test.renderer;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;

/**
 * Test for JMonkeyEngine issue #798: OpenGLException on restart with changed
 * display settings.
 * <p>
 * If the issue is resolved, then pressing the "P", "T", or "Y" key shouldn't
 * crash the app; it should close the app display and create a new display,
 * mostly black with statistics displayed in the lower left.
 * <p>
 * If the issue is not resolved, the expected failure mode depends on whether
 * assertions are enabled. If they're enabled, the app will crash with an
 * OpenGLException. If assertions aren't enabled, the new window will be
 * entirely black, with no statistics visible.
 * <p>
 * Since the issue was specific to LWJGL v2, this test should be built with the
 * jme3-lwjgl library, not jme3-lwjgl3.
 *
 * @author Stephen Gold
 */
public class TestIssue798 extends SimpleApplication {
    /**
     * Main entry point for the TestIssue798 application.
     *
     * @param args array of command-line arguments (not null)
     */
    public static void main(String[] args) {
        TestIssue798 app = new TestIssue798();
        app.start();
    }

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        inputManager.addMapping("windowedMode", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("moreSamples", new KeyTrigger(KeyInput.KEY_P));
        inputManager.addMapping("toggleDepth", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("toggleBpp", new KeyTrigger(KeyInput.KEY_Y));

        ActionListener listener = new ActionListener() {
            @Override
            public void onAction(String name, boolean keyPressed, float tpf) {
                if (name.equals("moreSamples") && keyPressed) {
                    moreSamples();
                } else if (name.equals("toggleBpp") && keyPressed) {
                    toggleBpp();
                } else if (name.equals("toggleDepth") && keyPressed) {
                    toggleDepth();
                } else if (name.equals("windowedMode") && keyPressed) {
                    windowedMode();
                }
            }
        };

        inputManager.addListener(listener,
                "moreSamples", "toggleBpp", "toggleDepth", "windowedMode");
    }

    /**
     * Restart the app, requesting 2 more MSAA samples per pixel.
     */
    private void moreSamples() {
        int numSamples = settings.getSamples();
        numSamples += 2;
        System.out.println("numSamples = " + numSamples);
        settings.setSamples(numSamples);
        setSettings(settings);

        restart();
    }

    /**
     * Restart the app, requesting a different number of bits per pixel in the
     * RGB buffer.
     */
    private void toggleBpp() {
        int bpp = settings.getBitsPerPixel();
        bpp = (bpp == 24) ? 16 : 24;
        System.out.println("BPP = " + bpp);
        settings.setBitsPerPixel(bpp);
        setSettings(settings);

        restart();
    }

    /**
     * Restart the app, requesting a different number of bits per pixel in the
     * depth buffer.
     */
    private void toggleDepth() {
        int depthBits = settings.getDepthBits();
        depthBits = (depthBits == 24) ? 16 : 24;
        System.out.println("depthBits = " + depthBits);
        settings.setDepthBits(depthBits);
        setSettings(settings);

        restart();
    }

    /**
     * If the app is in fullscreen mode, restart it in 640x480 windowed mode.
     */
    private void windowedMode() {
        boolean isFullscreen = settings.isFullscreen();
        if (!isFullscreen) {
            System.out.println("Request ignored: already in windowed mode!");
            return;
        }

        System.out.println("fullscreen = " + false);
        settings.setFullscreen(false);
        settings.setWidth(640);
        settings.setHeight(480);
        setSettings(settings);

        restart();
    }
}
