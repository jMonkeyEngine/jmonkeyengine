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
package jme3test.input;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;

/**
 * A test for issue https://github.com/jMonkeyEngine/jmonkeyengine/pull/1692.
 * We are testing to see if disabling then re-enabling the chase camera keeps the correct flags
 * set so that we can still rotate without dragging
 */
public class TestIssue1692 extends SimpleApplication implements ActionListener {

    private ChaseCamera chaseCam;
    private BitmapText cameraStatus;

    public static void main(String[] args) {
        TestIssue1692 app = new TestIssue1692();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Load a teapot model, we will chase this with the camera
        Geometry teaGeom = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj");
        Material teapotMaterial = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
        teaGeom.setMaterial(teapotMaterial);
        rootNode.attachChild(teaGeom);

        // Load a floor model
        Material floorMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        floorMaterial.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));
        Geometry ground = new Geometry("ground", new Quad(50, 50));
        ground.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        ground.setLocalTranslation(-25, -1, 25);
        ground.setMaterial(floorMaterial);
        rootNode.attachChild(ground);

        // Disable the default first-person cam!
        flyCam.setEnabled(false);

        // Enable a chase cam
        chaseCam = new ChaseCamera(cam, teaGeom, inputManager);
        /*
         * Explicitly set drag to rotate to false.
         * We are testing to see if disabling then re-enabling the camera keeps the correct flags
         * set so that we can still rotate without dragging.
         */
        chaseCam.setDragToRotate(false);

        // Show instructions
        int yTop = settings.getHeight();
        int size = guiFont.getCharSet().getRenderedSize();
        BitmapText hudText = new BitmapText(guiFont);
        hudText.setSize(size);
        hudText.setColor(ColorRGBA.Blue);
        hudText.setText("This test is for issue 1692.\n"
                + "We are testing to see if drag to rotate stays disabled"
                + "after disabling and re-enabling the chase camera.\n"
                + "For this test, use the SPACE key to disable and re-enable the camera.");
        hudText.setLocalTranslation(0, yTop - (hudText.getLineHeight() * 3), 0);
        guiNode.attachChild(hudText);

        // Show camera status
        cameraStatus = new BitmapText(guiFont);
        cameraStatus.setSize(size);
        cameraStatus.setColor(ColorRGBA.Blue);
        cameraStatus.setLocalTranslation(0, yTop - cameraStatus.getLineHeight(), 0); // position
        guiNode.attachChild(cameraStatus);

        // Register inputs
        registerInput();
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Update chaseCam status
        cameraStatus.setText("chaseCam " + (chaseCam.isEnabled() ? "enabled" : "disabled"));
    }

    private void registerInput() {
        inputManager.addMapping("toggleCamera", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "toggleCamera");
    }

    @Override
    public void onAction(String name, boolean keyPressed, float tpf) {
        if (name.equals("toggleCamera") && keyPressed) {
            // Toggle chase camera
            chaseCam.setEnabled(!chaseCam.isEnabled());
        }
    }
}
