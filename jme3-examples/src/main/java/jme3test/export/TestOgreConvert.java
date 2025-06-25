/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package jme3test.export;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.SkinningControl;
import com.jme3.app.SimpleApplication;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.MatParamOverride;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * This class is a jMonkeyEngine 3 (jME3) test application designed to verify
 * the import, export, and runtime behavior of 3D models, particularly those
 * in or compatible with the Ogre3D format (.mesh.xml).
 * It loads an Ogre model, saves and reloads it using jME3's binary exporter,
 * plays an animation, and displays debugging information about its skinning
 * and material parameters.
 *
 * @author capdevon
 */
public class TestOgreConvert extends SimpleApplication {

    public static void main(String[] args) {
        TestOgreConvert app = new TestOgreConvert();
        app.setPauseOnLostFocus(false);
        app.start();
    }

    private final StringBuilder sb = new StringBuilder();
    private int frameCount = 0;
    private BitmapText bmp;
    private Spatial spCopy;
    private SkinningControl skinningControl;

    @Override
    public void simpleInitApp() {
        configureCamera();
        setupLights();

        bmp = createLabelText(10, 20, "<placeholder>");

        // Load the Ogre model (Oto.mesh.xml) from the assets
        Spatial model = assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        // Save the loaded model to jME3's binary format and then reload it.
        // This tests the binary serialization/deserialization process.
        spCopy = BinaryExporter.saveAndLoad(assetManager, model);
        spCopy.setName("Oto-Copy");
        rootNode.attachChild(spCopy);

        AnimComposer animComposer = spCopy.getControl(AnimComposer.class);
        animComposer.setCurrentAction("Walk");

        // Get the SkinningControl from the model to inspect skinning properties
        skinningControl = spCopy.getControl(SkinningControl.class);
    }

    private void setupLights() {
        AmbientLight al = new AmbientLight();
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(0, -1, -1).normalizeLocal());
        rootNode.addLight(dl);
    }

    private void configureCamera() {
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(15f);

        cam.setLocation(new Vector3f(0, 0, 20));
    }

    @Override
    public void simpleUpdate(float tpf) {
        frameCount++;
        if (frameCount == 10) {
            frameCount = 0;

            sb.append("HW Skinning Preferred: ").append(skinningControl.isHardwareSkinningPreferred()).append("\n");
            sb.append("HW Skinning Enabled: ").append(skinningControl.isHardwareSkinningUsed()).append("\n");
            sb.append("Mesh Targets: ").append(skinningControl.getTargets().length).append("\n");

            for (MatParamOverride mpo : spCopy.getLocalMatParamOverrides()) {
                sb.append(mpo.getVarType()).append(" ");
                sb.append(mpo.getName()).append(": ");
                sb.append(mpo.getValue()).append("\n");
            }

            bmp.setText(sb.toString());
            sb.setLength(0);
        }
    }

    private BitmapText createLabelText(int x, int y, String text) {
        BitmapText bmp = new BitmapText(guiFont);
        bmp.setText(text);
        bmp.setLocalTranslation(x, settings.getHeight() - y, 0);
        bmp.setColor(ColorRGBA.Red);
        guiNode.attachChild(bmp);
        return bmp;
    }
}
