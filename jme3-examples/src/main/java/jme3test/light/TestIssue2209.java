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
package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import java.util.logging.Logger;
import jme3test.bullet.TestIssue1125;

/**
 * Test case for JME issue #2209: AssertionError caused by shadow renderer.
 *
 * <p>For a valid test, assertions must be enabled.
 *
 * <p>If successful, the Oto model will appear. If unsuccessful, the application
 * with crash with an {@code AssertionError} in {@code GLRenderer}.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestIssue2209 extends SimpleApplication {
    /**
     * message logger for debugging this class
     */
    final public static Logger logger
            = Logger.getLogger(TestIssue1125.class.getName());

    /**
     * Main entry point for the TestIssue2209 application.
     */
    public static void main(String[] args) {
        new TestIssue2209().start();
    }

    /**
     * Initializes this application, adding Oto, a light, and a shadow renderer.
     */
    @Override
    public void simpleInitApp() {
        if (!areAssertionsEnabled()) {
            throw new IllegalStateException(
                    "For a valid test, assertions must be enabled.");
        }

        DirectionalLight dl = new DirectionalLight();
        rootNode.addLight(dl);

        DirectionalLightShadowRenderer dlsr
                = new DirectionalLightShadowRenderer(assetManager, 4_096, 3);
        dlsr.setLight(dl);
        viewPort.addProcessor(dlsr);

        Node player = (Node) assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        player.setShadowMode(RenderQueue.ShadowMode.Cast);
        rootNode.attachChild(player);
    }

    /**
     * Tests whether assertions are enabled.
     *
     * @return true if enabled, otherwise false
     */
    private static boolean areAssertionsEnabled() {
        boolean enabled = false;
        assert enabled = true; // Note: intentional side effect.

        return enabled;
    }
}
