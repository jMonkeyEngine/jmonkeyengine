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
package jme3test.renderer;

import com.jme3.app.SimpleApplication;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

/**
 * Simple application to test split-screen rendering. Clicking with LMB toggles
 * between a single camera/viewport (cam) and split screen (leftCam plus
 * rightCam). See issue #357.
 */
public class TestSplitScreen extends SimpleApplication implements ActionListener {

    private boolean splitScreen = false;
    final private Box mesh = new Box(1f, 1f, 1f);
    final private Node leftScene = new Node("left scene");
    private ViewPort leftView, rightView;

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);

        Geometry blueBox = new Geometry("blue box", mesh);
        Material blueMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        blueMat.setColor("Color", ColorRGBA.Blue);
        blueBox.setMaterial(blueMat);
        rootNode.attachChild(blueBox);

        Camera rightCam = cam.clone();
        rightCam.setViewPort(0.5f, 1f, 0f, 1f);

        rightView = renderManager.createMainView("right", rightCam);
        rightView.setClearFlags(true, true, true);
        rightView.setEnabled(false);
        rightView.attachScene(rootNode);

        Geometry redBox = new Geometry("red box", mesh);
        Material redMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        redMat.setColor("Color", ColorRGBA.Red);
        redBox.setMaterial(redMat);
        leftScene.attachChild(redBox);

        Camera leftCam = cam.clone();
        leftCam.setViewPort(0f, 0.5f, 0f, 1f);

        leftView = renderManager.createMainView("left", leftCam);
        leftView.setClearFlags(true, true, true);
        leftView.setEnabled(false);
        leftView.attachScene(leftScene);

        inputManager.addMapping("lmb", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "lmb");
    }

    @Override
    public void onAction(String name, boolean keyPressed, float tpf) {
        if (name.equals("lmb") && !keyPressed) {
            splitScreen = !splitScreen;
            viewPort.setEnabled(!splitScreen);
            leftView.setEnabled(splitScreen);
            rightView.setEnabled(splitScreen);
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        leftScene.updateLogicalState(tpf);
        leftScene.updateGeometricState();
    }

    public static void main(String[] args) {
        TestSplitScreen app = new TestSplitScreen();
        app.start();
    }
}
