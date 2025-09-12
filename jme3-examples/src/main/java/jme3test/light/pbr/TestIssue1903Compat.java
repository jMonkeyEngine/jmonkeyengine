/*
 * Copyright (c) 2023 jMonkeyEngine
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
package jme3test.light.pbr;

import com.jme3.app.SimpleApplication;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.CenterQuad;
import com.jme3.system.AppSettings;

/**
 * Reproduces an issue where PBR materials render much darker with the Core 3.2
 * profile than with the Compatibility profile.
 * 
 * <p>This test relies on AppSettings set in main(), so it shouldn't be run
 * from the jme3-examples TestChooser!
 *
 * <p>Compare the window rendered by this test with that rendered by
 * TestIssue1903Core. If they differ, you have reproduced the issue.
 * If they are identical, then you haven't reproduced it.
 */
public class TestIssue1903Compat extends SimpleApplication {
    /**
     * Main entry point for the TestIssue1903Compat application.
     *
     * @param unused array of command-line arguments
     */
    public static void main(String[] unused) {
        boolean loadDefaults = true;
        AppSettings appSettings = new AppSettings(loadDefaults);
        appSettings.setGammaCorrection(true);
        appSettings.setRenderer(AppSettings.LWJGL_OPENGL2); // Compatibility profile
        appSettings.setTitle("Compatibility");

        TestIssue1903Compat application = new TestIssue1903Compat();
        application.setSettings(appSettings);
        application.setShowSettings(false); // to speed up testing
        application.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);

        // Attach a 9x9 quad at the origin.
        Mesh mesh = new CenterQuad(9f, 9f);
        Geometry quad = new Geometry("quad", mesh);
        rootNode.attachChild(quad);

        // Apply a PBR material to the quad.
        String materialAssetPath = "TestIssue1903.j3m";
        Material material = assetManager.loadMaterial(materialAssetPath);
        quad.setMaterial(material);

        // Add a LightProbe.
        String lightProbePath = "Scenes/LightProbes/quarry_Probe.j3o";
        LightProbe probe = (LightProbe) assetManager.loadAsset(lightProbePath);
        rootNode.addLight(probe);
    }
}
