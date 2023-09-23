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
import com.jme3.system.AppSettings;

/**
 * Test for JMonkeyEngine issue #2011: context profiles are not defined for
 * OpenGL v3.0/v3.1
 * <p>
 * If the issue is resolved, then pressing the "0" or "1" key shouldn't crash
 * the app; it should close the app display and create a new display, mostly
 * black with statistics displayed in the lower left.
 * <p>
 * If the issue is not resolved, then pressing the "0" or "1" key should crash
 * the app with multiple exceptions.
 * <p>
 * Since the issue was specific to LWJGL v3, this test should be built with the
 * jme3-lwjgl3 library, not jme3-lwjgl.
 *
 * @author Stephen Gold
 */
public class TestIssue2011 extends SimpleApplication {
    /**
     * Main entry point for the TestIssue2011 application.
     *
     * @param args array of command-line arguments (not null)
     */
    public static void main(String[] args) {
        TestIssue2011 app = new TestIssue2011();
        app.start();
    }

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        inputManager.addMapping("3.0", new KeyTrigger(KeyInput.KEY_0),
                new KeyTrigger(KeyInput.KEY_NUMPAD0));
        inputManager.addMapping("3.1", new KeyTrigger(KeyInput.KEY_1),
                new KeyTrigger(KeyInput.KEY_NUMPAD1));
        inputManager.addMapping("3.2", new KeyTrigger(KeyInput.KEY_2),
                new KeyTrigger(KeyInput.KEY_NUMPAD2));

        ActionListener listener = new ActionListener() {
            @Override
            public void onAction(String name, boolean keyPressed, float tpf) {
                if (name.equals("3.0") && keyPressed) {
                    setApi(AppSettings.LWJGL_OPENGL30);
                } else if (name.equals("3.1") && keyPressed) {
                    setApi(AppSettings.LWJGL_OPENGL31);
                } else if (name.equals("3.2") && keyPressed) {
                    setApi(AppSettings.LWJGL_OPENGL32);
                }
            }
        };

        inputManager.addListener(listener, "3.0", "3.1", "3.2");
    }

    /**
     * Restart the app, specifying which OpenGL version to use.
     *
     * @param desiredApi the string to be passed to setRenderer()
     */
    private void setApi(String desiredApi) {
        System.out.println("desiredApi = " + desiredApi);
        settings.setRenderer(desiredApi);
        setSettings(settings);

        restart();
    }
}
